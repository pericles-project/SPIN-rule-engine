package org.topbraid.spin.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileUtils;

/**
 * A stand-alone SPIN inference engine callable from the command line.
 * 
 * @author Holger Knublauch
 */
public class RunInferences {

	/**
	 * The command line entry point.
	 * @param args 
	 * 		[0]: the base URI/physical URL of the file
	 * 		[1]: the (optional) name of a local RDF file contains the base URI
	 */
	public static void main(String[] args) throws IOException {
		
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		
		if(args.length == 0) {
			System.out.println("Arguments: baseURI [fileName]");
			System.exit(0);
		}

		// Load main file
		String baseURI = args[0];
		Model baseModel = ModelFactory.createDefaultModel();
		if(args.length > 1) {
			String fileName = args[1];
			File file = new File(fileName);
			InputStream is = new FileInputStream(file);
			String lang = FileUtils.guessLang(fileName);
			baseModel.read(is, baseURI, lang);
		}
		else {
			String lang = FileUtils.guessLang(baseURI);
			baseModel.read(baseURI, lang);
		}
		
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

		// Output results in Turtle
		newTriples.write(System.out, FileUtils.langTurtle);
	}
}
