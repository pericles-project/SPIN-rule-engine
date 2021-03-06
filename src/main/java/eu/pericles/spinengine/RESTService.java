/*
 * Copyright 2016 Fabio Corubolo - University of Liverpool
 *
 *                            Licensed under the Apache License, Version 2.0 (the "License");
 *                            you may not use this file except in compliance with the License.
 *                            You may obtain a copy of the License at
 *
 *                                  http://www.apache.org/licenses/LICENSE-2.0
 *
 *                            Unless required by applicable law or agreed to in writing, software
 *                            distributed under the License is distributed on an "AS IS" BASIS,
 *                            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *                            See the License for the specific language governing permissions and
 *                            limitations under the License.
 */

package eu.pericles.spinengine;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileUtils;
import org.apache.jena.vocabulary.RDFS;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * A restful web service that implements calls to the SPIN rule engine. It implements methods for rule inference and for constraints checking.
 * Contains also a command line interface.
 */
@Provider
@Path("/")
public class RESTService {

    public static class CLIParams {

        @Parameter(names = {"-ecosystemFile", "-DEM"}, description = "use the specified DEM model", required = false)
        public String DEM;
        @Parameter(names = "-sentToPersist", description = "Send generated triples to PERSIST API")
        public boolean toPersist = true;
        @Parameter(names = "-repository", description = "Send generated triples to PERSIST API")
        public String repositiry= "eumetsatdata";

        @Parameter(names = {"-baseURI", "-URI"}, description = "The base URI for SPIN rule and models", required = false)
        public String uri;

        @Parameter(names = "-basedoc", description = "The actual document in case it's not accessible or available at the specified URI")
        public String baseDoc;

        @Parameter(names = "-SPINURI", description = "The URI containing the SPIN rules, if separate and not imported by the base model")
        public String SPINURI;

        @Parameter(names = "-SPINdoc", description = "The actual SPIN rule document, in case it's not accessible or available at the specified URI")
        public String SPINdoc;
        @Parameter(names = "-outFormat", description = "The result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html")
        public String outFormat = "TTL";
        @Parameter(names = "--help", description = "Will show this message", help = true)
        public boolean help;
        @Parameter(names = "--constraints", description = "Will check constraints running inference")
        public boolean constraints = false;
        @Parameter(names = "--constraints-no-inference", description = "Will check constraints without running inference")
        public boolean constraintsnoinf = false;

    }

    public static void main(String[] args) {
        CLIParams p = new CLIParams();

        try {
            JCommander c = new JCommander(p, args);
            if (p.help) {
                c.usage();
            }

        } catch (ParameterException x) {
            System.err.println(x.getMessage());
            new JCommander(p).usage();
            return;
        }

        if (p.DEM!= null && p.DEM.trim().length()>0) {
            try {
                String model = new Scanner(new File(p.DEM)).useDelimiter("\\Z").next();
                SPINResults[] res = getSpinResults("", model, p.outFormat);
                if (p.toPersist)
                    sendNewTriples(res, p.repositiry);
                for (SPINResults r : res) {
                    System.out.print(r.toString().replace("\\n", "\\n\n"));
                    System.out.print("\n\n----\n\n");
                }
            } catch (FileNotFoundException x) {
                x.printStackTrace();
            }

        } else {
            SPINResults s = runInference(p.uri, p.baseDoc, p.SPINURI, p.SPINdoc, p.outFormat, p.constraints || p.constraintsnoinf, !p.constraintsnoinf);
            System.out.print(s.toString().replace("\\n", "\\n\n"));
        }
    }


    /**
     * Simple test method
     *
     * @return the 'test at ' String + current date
     */
    @GET
    @Path("/test")
    @Produces("text/plain")
    public String test() {
        return "test at " + LocalDateTime.now();
    }


    /**
     * This POST method call will execute the rule engine inference on the specified data. The required parameter is the base URI,
     * that will be used to gather the rules and models.
     *
     * @param baseURI      The base URI for SPIN rule and models.
     * @param document     The actual document in case it's not accessible or available at the specified URI
     * @param SPINURI      The URI containing the SPIN rules, if separate and not imported by the base model
     * @param SPINdocument The actual SPIN rule document, in case it's not accessible or available at the specified URI
     * @param outFormat    the result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @return a JSON result set in the form of a {@link SPINResults} format
     */
    @POST
    @Path("/runInferencesPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @NoCache()
    public SPINResults runInferences(@FormParam("baseURI") String baseURI, @FormParam("document") String document, @FormParam("SPINURI") String SPINURI, @FormParam("SPINdocument") String SPINdocument, @FormParam("outFormat") String outFormat) {
        return runInference(baseURI, document, SPINURI, SPINdocument, outFormat, false, true);
    }

    /**
     * This GET method call will execute the rule engine inference on the specified DEM model rules.
     *
     * @param baseURI      The base URI for SPIN rule and models.
     * @param document     The actual document in case it's not accessible or available at the specified URI
     * @param outFormat    the result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @return a JSON result set in the form of a {@link SPINResults} format
     */
    @GET
    @Path("/runInferencesDEMGet")
    @Produces("application/json")
    @NoCache()
    public SPINResults[] runInferencesDEMGet(@QueryParam("baseURI") String baseURI, @QueryParam("document") String document, @QueryParam("outFormat") String outFormat) {

        SPINResults[] res = getSpinResults(baseURI, document, outFormat);
        sendNewTriples(res,"test");
        return res;
    }


    public static class DEMRequest {
        String uri;
        String model;
        String format;
        public DEMRequest(){

        }

        public DEMRequest(String uri, String model, String format, String repository) {
            this.uri = uri;
            this.model = model;
            this.format = format;
            this.repository = repository;
        }

        public String getRepository() {
            return repository;
        }

        public void setRepository(String repository) {
            this.repository = repository;
        }

        String repository;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    @POST
    @Path("/executeDEMRules")
    @Consumes("application/json")
    @Produces("application/json")
    @NoCache()
    public SPINResults[] runInferencesDEMJson(DEMRequest r) {
        SPINResults[] res = getSpinResults(r.uri, r.model, r.format);
        sendNewTriples(res,r.repository);
        return res;

    }


    /**
     * This POST method call will execute the rule engine inference on the specified DEM model rules.
     *
     * @param baseURI      The base URI for SPIN rule and models.
     * @param document     The actual document in case it's not accessible or available at the specified URI
     * @param outFormat    the result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @return a JSON result set in the form of a {@link SPINResults} format
     */
    @POST
    @Path("/runInferencesDEMPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @NoCache()
    public SPINResults[] runInferencesDEM(@FormParam("baseURI") String baseURI, @FormParam("document") String document, @FormParam("outFormat") String outFormat) {
        SPINResults[] res = getSpinResults(baseURI, document, outFormat);
        sendNewTriples(res,"test");
        return res;

    }



    /**
     * This GET method call will execute the rule engine inference on the specified data. The required parameter is the base URI,
     * that will be used to gather the rules and models.
     *
     * @param baseURI      The base URI for SPIN rule and models.
     * @param document     The actual document in case it's not accessible or available at the specified URI
     * @param SPINURI      The URI containing the SPIN rules, if separate and not imported by the base model
     * @param SPINdocument The actual SPIN rule document, in case it's not accessible or available at the specified URI
     * @param outFormat    the result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @return a JSON result set in the form of a {@link SPINResults} format
     */
    @GET
    @Path("/runInferencesGet")
    @Produces("application/json")
    @NoCache()
    public SPINResults runInferencesGet(@QueryParam("baseURI") String baseURI, @QueryParam("document") String document, @QueryParam("SPINURI") String SPINURI, @QueryParam("SPINdocument") String SPINdocument, @QueryParam("outFormat") String outFormat) {

        return runInference(baseURI, document, SPINURI, SPINdocument, outFormat, false, true);
    }




    /**
     * This POST method call will execute the rule engine constraints (optionally inference as well) on the specified data. The required parameter is the base URI,
     * that will be used to gather the rules and models.
     *
     * @param baseURI      The base URI for SPIN rule and models.
     * @param document     The actual document in case it's not accessible or available at the specified URI
     * @param SPINURI      The URI containing the SPIN rules, if separate and not imported by the base model
     * @param SPINdocument The actual SPIN rule document, in case it's not accessible or available at the specified URI
     * @param outFormat    the result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @param doInference  perform inference before constraint checking. Defaults true
     * @return a JSON result set in the form of a {@link SPINResults} format
     */
    @POST
    @Path("/runConstraintsPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @NoCache()
    public SPINResults runConstraintsPost(@FormParam("baseURI") String baseURI, @FormParam("document") String document, @FormParam("SPINURI") String SPINURI, @FormParam("SPINdocument") String SPINdocument, @QueryParam("outFormat") String outFormat, @FormParam("doInference") @DefaultValue("true") boolean doInference) {
        return runInference(baseURI, document, SPINURI, SPINdocument, outFormat, true, doInference);
    }

    /**
     * This GET method call will execute the rule engine constraints (optionally inference as well) on the specified data. The required parameter is the base URI,
     * that will be used to gather the rules and models.
     *
     * @param baseURI      The base URI for SPIN rule and models.
     * @param document     The actual document in case it's not accessible or available at the specified URI
     * @param SPINURI      The URI containing the SPIN rules, if separate and not imported by the base model
     * @param SPINdocument The actual SPIN rule document, in case it's not accessible or available at the specified URI
     * @param doInference  perform inference before constraint checking. Defaults true
     * @param outFormat    the result output format, default TTL.  Options are TTL, NTRIPLES, RDFXML, N3, JSONLD, RDFJSON. For the complete list, see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @return a JSON result set in the form of a {@link SPINResults} format
     */
    @GET
    @Path("/runConstraintsGet")
    @Produces("application/json")
    @NoCache()
    public SPINResults runConstraintsGet(@QueryParam("baseURI") String baseURI, @QueryParam("document") String document, @QueryParam("SPINURI") String SPINURI, @QueryParam("SPINdocument") String SPINdocument, @QueryParam("outFormat") String outFormat, @QueryParam("doInference") @DefaultValue("true") boolean doInference) {
        return runInference(baseURI, document, SPINURI, SPINdocument, outFormat, true, doInference);
    }



    public static class PERSISTRequest {
        public String delta_stream;
        public String ERMR_repository;
    }

    private static void sendNewTriples(SPINResults[] res, String repository) {
        // Begin: Temporary solution to extract single deltas
//
//        Pattern pattern = Pattern.compile("DEM-Scenario:SEVIRIImage(.*?)] .");
//        Matcher matcher = pattern.matcher(r.getNewTriplets());
//        while (matcher.find()) {
//            System.out.println(matcher.group(1));
//        }

        for (SPINResults r:res) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("http://persist.iti.gr:5000/api/conversion_multiple_deltas");
            target.request().accept("application/json");
            PERSISTRequest request = new PERSISTRequest();
            request.delta_stream = r.getNewTriplets();
            request.ERMR_repository = repository;
            javax.ws.rs.core.Response response = target.request().post(Entity.json(request));
            String s = response.readEntity(String.class);
            // Entity e = Entity.json(request);
            //     int s = response.getStatus();
            //String res=response.toString();
            r.jsonPERSISTOUT = s;
        }
//        String res = response.getEntity().toString();

//        String value = response.readEntity(String.class);
//        response.close();  // You should close connections!
//
//        ResteasyClient client = new ResteasyClientBuilder().build();
//        ResteasyWebTarget target = client.target("http://foo.com/resource");        request.accept("application/json");

//        Model model = r.newTripleModel;
//        String queryString =
//                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
//                "PREFIX DEM-Core: <http://c102-086.cloud.gwdg.de/ns/DEM-Core#>\n" +
//
//                        " \n" +
//                        "SELECT ?subject \n" +
//                        "WHERE {?subject ?predicate ?object} \n\n";
//        org.apache.jena.query.Query query = QueryFactory.create(queryString) ;
//        String SPINdocument ="";
//        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
//            ResultSet results = qexec.execSelect() ;
//            for ( ; results.hasNext() ; )
//            {
//                QuerySolution soln = results.nextSolution() ;
//                RDFNode x = soln.get("subject") ;       // Get a result variable by name.
//                SPINdocument = x.toString();
//                SPINdocument.trim();
//            }
//        }

    }
    private static SPINResults[] getSpinResults(@QueryParam("baseURI") String baseURI, @QueryParam("document") String document, @QueryParam("outFormat") String outFormat) {
        LinkedList<SPINResults> result = new LinkedList<>();
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX DEM-Policy: <http://c102-086.cloud.gwdg.de/ns/DEM-Policy#>\n" +
                "PREFIX LRM: <http://c102-086.cloud.gwdg.de/ns/LRM#>\n" +
                "\n" +
                "SELECT ?definition\n" +
                "WHERE {\n" +
                "    ?policy DEM-Policy:hasPolicyStatement ?policy_statement .\n" +
                " ?policy_statement LRM:definition ?definition .\n" +
                " ?policy_statement DEM-Policy:language ?policy_format .\n" +
                " FILTER (?policy_format = \"SPIN\")\n" +
                "}";
        Model model = getOntModel(baseURI,document,null, null);
        org.apache.jena.query.Query query = QueryFactory.create(queryString,baseURI) ;
        String SPINdocument ="";
        int n=0;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect() ;
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                RDFNode x = soln.get("definition") ;       // Get a result variable by name.
                SPINdocument = x.asLiteral().getString();
                SPINResults t = runInference(baseURI, document, baseURI+(n++)+".spin.ttl", SPINdocument, outFormat, false, true);
                result.add(t);
            }
        }
        return result.toArray(new SPINResults[]{});
    }


    private static SPINResults runInference(String baseURI, String document, String SPINURI, String SPINdocument, String outFormat, boolean checkConstraints, boolean doInference) {
        String constraints = "";
        String model = "";
        String newTriplets = "";
        StringWriter w;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // IMPORTANT: Save the old System.out!
        PrintStream old = System.out;
        PrintStream olderr = System.err;

        // Tell Java to use your special streams
        System.setOut(ps);
        System.setErr(ps);
        // Put things back
        if (baseURI == null) {
            System.out.flush();
            System.err.flush();
            System.setOut(old);
            System.setErr(olderr);
            return new SPINResults(null, null, null, "The baseURI parameter must be set", null, null, null, null);
        }

        if (outFormat == null || outFormat.trim().equals("")) {
            outFormat = "TTL";
        }
        // Initialize system functions and templates
        Model baseModel;

        try {
            baseModel = getOntModel(baseURI, document, SPINURI, SPINdocument);
        } catch (org.apache.jena.atlas.web.HttpException x) {
            ps.println(x.getMessage());
            ps.println(baseURI);
            System.out.flush();
            System.err.flush();
            System.setOut(old);
            System.setErr(olderr);
            return new SPINResults(null, null, null, baos.toString(), null, null, null,null);
        }

        OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
        // Register locally defined functions
        SPINModuleRegistry.get().registerAll(ontModel, null);

        // Create and add Model for inferred triples
        Model newTriples = ModelFactory.createDefaultModel();
        newTriples.setNsPrefixes(ontModel);
        ontModel.addSubModel(newTriples);
        List<ConstraintViolation> cvs;
        if (doInference) {
            SPINInferences.run(ontModel, newTriples, null, null, false, null);
            // Perform inferencing
            w = new StringWriter();
            ontModel.write(w, outFormat);
            model = w.toString();
            // Create results model
            w = new StringWriter();
            // Output results in Turtle
            newTriples.write(w, outFormat);

            newTriplets = w.toString();
        }

        if (checkConstraints) {
            w = new StringWriter();
            // Perform constraint checking
            cvs = SPINConstraints.check(ontModel, null);
            Model results = ModelFactory.createDefaultModel();
            results.setNsPrefix(SPIN.PREFIX, SPIN.NS);
            results.setNsPrefix("rdfs", RDFS.getURI());
            SPINConstraints.addConstraintViolationsRDF(cvs, results, false);
            results.write(w, outFormat);
            constraints = w.toString();

        }
        System.out.flush();
        System.err.flush();
        System.setOut(old);
        System.setErr(olderr);
        model = toRemote(model);
        newTriplets = toRemote(newTriplets);
        constraints = toRemote(constraints);
        if (outFormat.toLowerCase().contains("json"))
            return new SPINResults("", "", "", baos.toString(), model, newTriplets, constraints, newTriples);
        else
            return new SPINResults(constraints, model, newTriplets, baos.toString(), null, null, null, newTriples);
    }


    private static String toLocal(String original) {
//        return original;
        if (original == null){
            return null;
        }
        String r = original.replaceAll("http://www.pericles-project.eu/ns/DEM-","http://c102-086.cloud.gwdg.de/ns/DEM-");
        r = r.replaceAll("http://xrce.xerox.com/LRM", "http://c102-086.cloud.gwdg.de/ns/LRM");
        return r;
    }
    private static String toRemote(String original) {
//        return original;
        if (original == null){
            return null;
        }
        String r = original.replaceAll("http://c102-086.cloud.gwdg.de/ns/DEM-","http://www.pericles-project.eu/ns/DEM-");
        r = r.replaceAll("http://c102-086.cloud.gwdg.de/ns/LRM","http://xrce.xerox.com/LRM");
        return r;
    }
    /**
     * Retrieve and builds the ontology model
     */
    private static Model getOntModel(String baseURI, String document, String SPINURI, String SPINdocument) {

        document = toLocal(document);
        SPINdocument = toLocal(SPINdocument);

        SPINModuleRegistry.get().init();
        Model baseModel = ModelFactory.createDefaultModel();
        String lang = FileUtils.guessLang(baseURI,"ttl");
        if (document != null) {
            baseModel.read(new StringReader(document), baseURI, lang);
        } else {
            baseModel.read(baseURI, lang);
        }
        if (SPINURI != null) {
            Model spiModel = ModelFactory.createDefaultModel();

            String lang2 = FileUtils.guessLang(SPINURI);

            if (SPINdocument != null) {
                spiModel.read(new StringReader(SPINdocument), SPINURI, lang2);
            } else {
                spiModel.read(SPINURI, lang);
            }
            // Create Model for the base graph with its imports
            MultiUnion union = new MultiUnion(new Graph[]{
                    baseModel.getGraph(),
                    spiModel.getGraph(),
                    SPL.getModel().getGraph(),
                    SPIN.getModel().getGraph(),
                    SP.getModel().getGraph()
            });
            baseModel = ModelFactory.createModelForGraph(union);
        }

        return baseModel;
    }


}