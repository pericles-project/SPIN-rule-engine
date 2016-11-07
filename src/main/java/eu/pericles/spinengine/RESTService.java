package eu.pericles.spinengine;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileUtils;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import javax.ws.rs.*;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

@Provider
@Path("/")
public class RESTService {

    public class SPINResults{


        public String constraints;

        public SPINResults(String constraints, String model, String newTriplets, String consoleLog) {
            this.constraints = constraints;
            this.model = model;
            this.newTriplets = newTriplets;
            this.consoleLog = consoleLog;
        }

        public String model = "";

        public String newTriplets="";
        public String consoleLog="";


        public String getConstraints() {
            return constraints;
        }

        public String getModel() {
            return model;
        }

        public String getNewTriplets() {
            return newTriplets;
        }

        public String getConsoleLog() {
            return consoleLog;
        }
    }

    public class SPINResultsJson extends SPINResults{

        public SPINResultsJson(String constraints, String model, String newTriplets, String consoleLog, String jsonModel, String jsonNewTriplets, String jsonConstraints) {
            super(constraints, model, newTriplets, consoleLog);
            this.jsonModel = jsonModel;
            this.jsonNewTriplets = jsonNewTriplets;
            this.jsonConstraints = jsonConstraints;
        }

        @JsonRawValue
        public String jsonModel = "";
        @JsonRawValue
        public String jsonNewTriplets="";
        @JsonRawValue
        public String jsonConstraints;

    }
    @GET
    @Path("/test")
    @Produces("text/plain")
    public String test() {
        return "test";
    }


    /**
     *
     * @param baseURI
     * @param document
     * @param SPINURI
     * @param SPINdocument
     * @param outFormat see formats defined in the JENA API: https://jena.apache.org/documentation/io/rdf-output.html
     * @return
     */
    @POST
    @Path("/runInferencesPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/plain")
    public SPINResults runInferences(@FormParam("baseURI") final String baseURI, @FormParam("document") final String document, @FormParam("SPINURI") final String SPINURI, @FormParam("SPINdocument") final String SPINdocument, @FormParam("outFormat") final String outFormat) {
        return runInference(baseURI, document,SPINURI, SPINdocument, outFormat, false, true);
    }

    @GET
    @Path("/runInferencesGet")
    @Produces("application/json")
    public SPINResults runInferencesGet(@QueryParam("baseURI") final String baseURI, @QueryParam("document") final String document,@QueryParam("SPINURI") final String SPINURI, @QueryParam("SPINdocument") final String SPINdocument, @QueryParam("outFormat")final String outFormat) {

        return runInference(baseURI, document,SPINURI, SPINdocument, outFormat, false, true);
    }




    @POST
    @Path("/runConstraintsPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public SPINResults runConstraintsPost(@FormParam("baseURI") final String baseURI, @FormParam("document") final String document, @FormParam("SPINURI") final String SPINURI, @FormParam("SPINdocument") final String SPINdocument, @QueryParam("outFormat")final String outFormat, @FormParam("doInference") final boolean doInference) {
        return runInference(baseURI, document,SPINURI, SPINdocument ,outFormat, true, doInference);
    }

    @GET
    @Path("/runConstraintsGet")
    @Produces("application/json")
    public SPINResults runConstraintsGet(@QueryParam("baseURI") final String baseURI, @QueryParam("document") final String document,@QueryParam("SPINURI") final String SPINURI, @QueryParam("SPINdocument") final String SPINdocument,  @QueryParam("outFormat")final String outFormat, @QueryParam("doInference") final boolean doInference) {
        return runInference(baseURI, document,SPINURI, SPINdocument ,outFormat, true, doInference);
    }

    private SPINResults runInference(String baseURI, String document, String SPINURI, String SPINdocument, String outFormat, boolean checkConstraints, boolean doInference) {
        String constraints = "";
        String model = "";
        String newTriplets="";
        StringWriter w = new StringWriter();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // IMPORTANT: Save the old System.out!
        PrintStream old = System.out;
        PrintStream olderr = System.err;

        // Tell Java to use your special stream
        System.setOut(ps);
        System.setErr(ps);
        // Put things back
        if (baseURI == null) {
            System.out.flush();
            System.err.flush();
            System.setOut(old);
            System.setErr(olderr);

            return new SPINResults(null,null,null,"The baseURI parameter must be set");
        }

        if (outFormat==null || outFormat.trim().equals("")) {
            outFormat="TTL";
        }
        // Initialize system functions and templates
        Model baseModel = null;

        try {
            baseModel = getOntModel(baseURI, document, SPINURI, SPINdocument);
        } catch (org.apache.jena.atlas.web.HttpException x) {
            ps.println(x.getMessage());
            ps.println(baseURI);

            System.out.flush();
            System.err.flush();
            System.setOut(old);
            System.setErr(olderr);

            return new SPINResults(null,null,null,baos.toString());
        }
        OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
        // Register locally defined functions
        SPINModuleRegistry.get().registerAll(ontModel, null);

        // Create and add Model for inferred triples
        Model newTriples = ModelFactory.createDefaultModel();
        newTriples.setNsPrefixes(ontModel);
        ontModel.addSubModel(newTriples);
        List<ConstraintViolation> cvs = null;
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

        if (outFormat.toLowerCase().contains("json"))
            return new SPINResultsJson("","","", baos.toString(), model, newTriplets,constraints);
        else
            return new SPINResults(constraints, model, newTriplets, baos.toString());
    }

    private Model getOntModel(String baseURI, String document, String SPINURI, String SPINdocument) {

        SPINModuleRegistry.get().init();
        Model baseModel = ModelFactory.createDefaultModel();
        String lang = FileUtils.guessLang(baseURI);
        if (document != null) {
            baseModel.read(new StringReader(document), baseURI, lang);
        }
        else {
            baseModel.read(baseURI, lang);
        }
        if (SPINURI!=null) {
            Model spiModel = ModelFactory.createDefaultModel();

            String lang2 = FileUtils.guessLang(SPINURI);

            if (document != null) {
                spiModel.read(new StringReader(SPINdocument), SPINURI, lang2);
            }
            else {
                spiModel.read(SPINURI, lang);
            }
            // Create Model for the base graph with its imports
            MultiUnion union = new MultiUnion(new Graph[] {
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