package eu.pericles.spinengine;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileUtils;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.StringWriter;

@Provider
@Path("/")
public class RESTService {


    @GET
    @Path("/test")
    @Produces("text/plain")
    public String test() {
        return "test";
    }



    @POST
    @Path("/runInferences")
    @Consumes("application/x-www-form-urlencoded")
    @Produces ("text/plain")
    public String runInferences (@FormParam("baseURI")  final String baseURI,@FormParam("document")  final String document) {

        // Initialize system functions and templates
        SPINModuleRegistry.get().init();

        Model baseModel = ModelFactory.createDefaultModel();
        baseModel.read(document);

        // Create OntModel with imports
        OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM,baseModel);

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
    @GET
    @Path("/runInferencesGet")
    @Produces ("text/plain")
    public String runInferencesGet (@FormParam("baseURI")  final String baseURI,@FormParam("document")  final String document) {

        // Initialize system functions and templates
        SPINModuleRegistry.get().init();

        Model baseModel = ModelFactory.createDefaultModel();
        baseModel.read(document);

        // Create OntModel with imports
        OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM,baseModel);

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
}