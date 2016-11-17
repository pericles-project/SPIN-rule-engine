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

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents the results of the rule engine execution, consisting in the models and console logs generated
 * by the rule engine library. Some of the results may be empty depending on the system call.
 */
public class SPINResults {

    /**
     * @param constraints     Defined if the output is NOT in JSON format. The model results for constraint checking
     * @param model           Defined if the output is NOT in JSON format. The model union model (modified original model)
     * @param newTriplets     Defined if the output is NOT in JSON format. The new generated triples.
     * @param consoleLog      The results of the console log
     * @param jsonModel       If Json output specified:  The model union model (modified original model)
     * @param jsonNewTriplets If Json output specified:  The new generated triples.
     * @param jsonConstraints If Json output specified:   The model results for constraint checking
     */
    public SPINResults(String constraints, String model, String newTriplets, String consoleLog, String jsonModel, String jsonNewTriplets, String jsonConstraints) {
        this.constraints = constraints;
        this.model = model;
        this.newTriplets = newTriplets;
        this.consoleLog = consoleLog;
        this.jsonModel = jsonModel;
        this.jsonNewTriplets = jsonNewTriplets;
        this.jsonConstraints = jsonConstraints;
    }

    /**
     * Defined if the output is NOT in JSON format. The model union model (modified original model)
     */
    public String model = "";
    /**
     * Defined if the output is NOT in JSON format. The model results for constraint checking
     */
    public String constraints;
    /**
     * Defined if the output is NOT in JSON format. The new generated triples.
     */
    public String newTriplets = "";
    /**
     * The results of the console log
     */
    public String consoleLog = "";

    /**
     * If Json output specified:  The model union model (modified original model)
     */
    @JsonRawValue
    public String jsonModel = "";
    /**
     * If Json output specified:  The new generated triples.
     */
    @JsonRawValue
    public String jsonNewTriplets = "";
    /**
     * If Json output specified:   The model results for constraint checking
     */
    @JsonRawValue
    public String jsonConstraints;


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


    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        //Object to JSON in String
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return e.toString();
        }
    }
}
