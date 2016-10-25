# SPIN-rule-engine
SPIN rule engine based on the TopQuadrant SPIN API and JENA

The SPIN engine uses the SPIN API to run inference on the model.

The project is based on Maven, in order to direclty run the web service (port 8080 by default):<br>
1 - install Maven <br>
2 - clone or download the repository <br>
3 - go to the folder and run "mvn jetty:run"<br>
4 - open the browser at the local address: <br>
http://127.0.0.1:8080/spinengine/runInferencesGet?baseURI=http://topbraid.org/examples/kennedysSPIN
<br><br>
The API include the following methods:<br>
runInferencesGet<br>
runInferencesPost<br>
runConstraintsGet<br>
runConstraintsPost<br><br>
All the methods use the following 4 parameters (1st parameter is necessary, the other 3 optionals):<br>
baseURI: the URI for the base document <br>
document: (optional) the document contents <br>
SPINURI:  (optional) The URI for the SPIN document (to be merged with the base document) <br>
SPINdocument: the SPIN document contents <br>

 
---
Work in progress
