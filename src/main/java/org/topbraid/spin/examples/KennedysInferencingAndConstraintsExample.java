package org.topbraid.spin.examples;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.model.Function;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;

import java.util.Collection;
import java.util.List;


/**
 * Loads the Kennedys SPIN ontology and runs inferences and then
 * constraint checks on it.
 * 
 * @author Holger Knublauch
 */
public class KennedysInferencingAndConstraintsExample {

	public static void main(String[] args) {
		
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		// Load main file
		Model baseModel = ModelFactory.createDefaultModel();
		baseModel.read("http://127.0.0.1/rules.spin.ttl");
		Collection<Function> fun = SPINModuleRegistry.get().getFunctions();

		// Create OntModel with imports
		OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM,baseModel);
		
		// Create and add Model for inferred triples
		Model newTriples = ModelFactory.createDefaultModel();
		ontModel.addSubModel(newTriples);

		// Register locally defined functions
		SPINModuleRegistry.get().registerAll(ontModel, null);
		Collection<Function> fun2 = SPINModuleRegistry.get().getFunctions();

		// Run all inferences
		SPINInferences.run(ontModel, newTriples, null, null, false, null);
		System.out.println("Inferred triples: " + newTriples.size());

		// Run all constraints
		List<ConstraintViolation> cvs = SPINConstraints.check(ontModel, null);
		System.out.println("Constraint violations:");
		for(ConstraintViolation cv : cvs) {
			System.out.println(" - at " + SPINLabels.get().getLabel(cv.getRoot()) + ": " + cv.getMessage());
		}

		// Run constraints on a single instance only
		Resource person = cvs.get(0).getRoot();
		List<ConstraintViolation> localCVS = SPINConstraints.check(person, null);
		System.out.println("Constraint violations for " + SPINLabels.get().getLabel(person) + ": " + localCVS.size());
	}
}
