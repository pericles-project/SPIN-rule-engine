# SPIN-rule-engine
SPIN rule engine based on the TopQuadrant SPIN API and JENA

The SPIN engine uses the SPIN API to run inference on the model.

The project is based on Maven, in order to direclty run the web service (port 8080 by default):
1 - install Maven
2 - clone or download the repository
3 - go to the folder and run "mvn jetty:run"
4 - open the browser at the local address: 
http://127.0.0.1:8080/spinengine/runInferencesGet?baseURI=http://topbraid.org/examples/kennedysSPIN

The API include the following methods:
runInferencesGet
runInferencesPost
runConstraintsGet
runConstraintsPost
All the methods use the following 4 parameters (1st parameter is necessary, the other 3 optionals):
baseURI: the URI for the base document 
document: (optional) the document contents 
SPINURI:  (optional) The URI for the SPIN document (to be merged with the base document) 
SPINdocument: the SPIN document contents 

 
---
Work in progress
