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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by fabio on 18/11/2016.
 */
public class Client {


    public static void main(String[] args) throws FileNotFoundException {

//        WebTarget target = ClientBuilder.newClient().target("http://c102-086.cloud.gwdg.de/api/executeDEMRules");
        WebTarget target = ClientBuilder.newClient().target("http://127.0.0.1:8080/spinengine/api/executeDEMRules");

        String model = new Scanner(new File(args[0])).useDelimiter("\\Z").next();
        RESTService.DEMRequest request = new RESTService.DEMRequest("", model, "NTRIPLES", "eumetsatdata");
        javax.ws.rs.core.Response response = target.request().post(Entity.json(request));
        String s = response.readEntity(String.class);

        System.out.print(s);

    }

}
