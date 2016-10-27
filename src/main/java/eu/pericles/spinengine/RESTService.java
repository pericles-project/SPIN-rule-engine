package eu.pericles.spinengine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import javax.ws.rs.*;
import javax.ws.rs.ext.Provider;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Provider
@Path("/")
public class RESTService {

    static {  Annotation a = new JsonIgnoreProperties(){
        @Override
        public boolean allowGetters() {
            return false;
        }

        @Override
        public boolean allowSetters() {
            return false;
        }

        @Override
        public String[] value() {
            return new String[0];
        }

        @Override
        public boolean ignoreUnknown() {
            return true;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return JsonIgnoreProperties.class;
        }
    };
        //changeAnnotationValue(a,)
        try {
            Method method = Class.class.getDeclaredMethod("annotationData", null);
            method.setAccessible(true);
            Object annotationData = method.invoke(org.apache.jena.graph.Node.class, null);
            Field declaredAnnotations = annotationData.getClass().getDeclaredField("declaredAnnotations");
            declaredAnnotations.setAccessible(true);
            Map<Class<? extends Annotation>, Annotation> m = new HashMap<>();
            m.put(JsonIgnoreProperties.class, a);
            declaredAnnotations.set(annotationData,m);
//            Map<Class<? extends Annotation>, Annotation> annotations = (Map<Class<? extends Annotation>, Annotation>) ;

        } catch (Exception x){
            x.printStackTrace();
        }}
    @GET
    @Path("/test")
    @Produces("text/plain")
    public String test() {
        return "test";
    }

    @POST
    @Path("/runInferencesPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/plain")
    public String runInferences(@FormParam("baseURI") final String baseURI, @FormParam("document") final String document, @FormParam("SPINURI") final String SPINURI, @FormParam("SPINdocument") final String SPINdocument) {
        return runInference(baseURI, document,SPINURI, SPINdocument);
    }

    @GET
    @Path("/runInferencesGet")
    @Produces("text/plain")
    public String runInferencesGet(@QueryParam("baseURI") final String baseURI, @QueryParam("document") final String document,@QueryParam("SPINURI") final String SPINURI, @QueryParam("SPINdocument") final String SPINdocument) {

        return runInference(baseURI, document,SPINURI, SPINdocument);
    }



    @POST
    @Path("/runConstraintsPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/plain")
    public String runConstraintsPost(@FormParam("baseURI") final String baseURI, @FormParam("document") final String document, @FormParam("SPINURI") final String SPINURI, @FormParam("SPINdocument") final String SPINdocument,  @FormParam("doInference") final boolean doInference) {
        return runConstraintsModel(baseURI, document,SPINURI, SPINdocument, doInference);
    }

    @GET
    @Path("/runConstraintsGet")
    @Produces("text/plain")
    public String runConstraintsGet(@QueryParam("baseURI") final String baseURI, @QueryParam("document") final String document,@QueryParam("SPINURI") final String SPINURI, @QueryParam("SPINdocument") final String SPINdocument,  @QueryParam("doInference") final boolean doInference) {

        return runConstraintsModel(baseURI, document,SPINURI, SPINdocument, doInference);
    }


    @POST
    @Path("/runConstraintsListPost")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public  List<ConstraintViolation> runConstraintsListPost(@FormParam("baseURI") final String baseURI, @FormParam("document") final String document, @FormParam("SPINURI") final String SPINURI, @FormParam("SPINdocument") final String SPINdocument,  @FormParam("doInference") final boolean doInference) {
        return runConstraints(baseURI, document,SPINURI, SPINdocument, doInference);
    }

    @GET
    @Path("/runConstraintsListGet")
    @Produces("application/json")
    public  List<ConstraintViolation>  runConstraintsListGet(@QueryParam("baseURI") final String baseURI, @QueryParam("document") final String document,@QueryParam("SPINURI") final String SPINURI, @QueryParam("SPINdocument") final String SPINdocument,  @QueryParam("doInference") final boolean doInference) {
        return runConstraints(baseURI, document,SPINURI, SPINdocument, doInference);
    }



    private String runInference(String baseURI, String document, String SPINURI, String SPINdocument) {
        // Initialize system functions and templates
        Model baseModel = getOntModel(baseURI, document, SPINURI, SPINdocument);
        OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
        // Register locally defined functions
        SPINModuleRegistry.get().registerAll(ontModel, null);

        // Create and add Model for inferred triples
        Model newTriples = ModelFactory.createDefaultModel();
        newTriples.setNsPrefixes(ontModel);
        ontModel.addSubModel(newTriples);

        // Perform inferencing
        SPINInferences.run(ontModel, newTriples, null, null, false, null);

        // Create results model
        StringWriter w = new StringWriter();
        // Output results in Turtle
        newTriples.write(w, FileUtils.langTurtle);

        return w.toString();
    }


    private String runConstraintsModel(String baseURI, String document, String SPINURI, String SPINdocument, boolean doInference)  {
        // Initialize system functions and templates
        Model ontModel = getOntModel(baseURI, document, SPINURI, SPINdocument);

        // Register locally defined functions
        SPINModuleRegistry.get().registerAll(ontModel, null);

        if (doInference) {
            // Create and add Model for inferred triples
            Model newTriples = ModelFactory.createDefaultModel();
            newTriples.setNsPrefixes(ontModel);

            /// TODO: we don't add teh new triples tot he model check if OK
            //ontModel.addSubModel(newTriples);


            // Perform inferencing
            SPINInferences.run(ontModel, newTriples, null, null, false, null);

        }
        // Perform constraint checking
        List<ConstraintViolation> cvs = SPINConstraints.check(ontModel, null);
//        System.out.println("Constraint violations:");
//        for (ConstraintViolation cv : cvs) {
//            System.out.println(" - at " + SPINLabels.get().getLabel(cv.getRoot()) + ": " + cv.getMessage());
//        }

        // Create results model
        Model results = ModelFactory.createDefaultModel();
        results.setNsPrefix(SPIN.PREFIX, SPIN.NS);
        results.setNsPrefix("rdfs", RDFS.getURI());
        SPINConstraints.addConstraintViolationsRDF(cvs, results, false);
        StringWriter w = new StringWriter();

        results.write(w, FileUtils.langTurtle);

        return w.toString();
    }


    private List<ConstraintViolation> runConstraints(String baseURI, String document, String SPINURI, String SPINdocument, boolean doInference)  {
        // Initialize system functions and templates
        Model ontModel = getOntModel(baseURI, document, SPINURI, SPINdocument);

        // Register locally defined functions
        SPINModuleRegistry.get().registerAll(ontModel, null);


        if (doInference) {
            // Create and add Model for inferred triples
            Model newTriples = ModelFactory.createDefaultModel();
            newTriples.setNsPrefixes(ontModel);

            /// TODO: we don't add teh new triples tot he model check if OK
            //ontModel.addSubModel(newTriples);


            // Perform inferencing
            SPINInferences.run(ontModel, newTriples, null, null, false, null);

        }

        // Perform constraint checking
        List<ConstraintViolation> cvs = SPINConstraints.check(ontModel, null);


        System.out.println("Constraint violations:");
        for (ConstraintViolation cv : cvs) {
            System.out.println(" - at " + SPINLabels.get().getLabel(cv.getRoot()) + ": " + cv.getMessage());
        }

        return cvs;
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