
/* 2019-05 Extended by Luís Loureiro */
/* 2016-18 Extended by Luis Veiga and Joao Garcia */
/*
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package pt.ulisboa.tecnico.cnv.aws.autoscaler;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import pt.ulisboa.tecnico.cnv.aws.AmazonClient;
import pt.ulisboa.tecnico.cnv.aws.balancer.*;
import java.lang.Runnable;



import static java.lang.Integer.parseInt;

public class EC2AutoScaler implements Runnable{

    /*
     * Before running the code: Fill in your AWS access credentials in the provided
     * credentials file template, and be sure to move the file to the default
     * location (~/.aws/credentials) where the sample code will load the credentials
     * from. https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING: To avoid accidental leakage of your credentials, DO NOT keep the
     * credentials file in your source directory.
     */


    private static int MAXIMUM_NUMBER_OF_INSTANCES = 10;
    private static int MINIMUM_NUMBER_OF_INSTANCES = 1;
    private static int MAXIMUM_REQUEST_COMLPEXITY = 10;

    // ver isso depois
    private static long MAXIMUM_LOAD_POSSIBLE = MAXIMUM_NUMBER_OF_INSTANCES * MAXIMUM_REQUEST_COMLPEXITY;

    private static long MINIMUM_LOAD_AVAILABLE = MINIMUM_NUMBER_OF_INSTANCES * MAXIMUM_REQUEST_COMLPEXITY;

    private EC2InstancesManager manager;

    public void run(){
        /*if(manager.getNumberOfInstances() < MINIMUM_NUMBER_OF_INSTANCES){
            manager.createInstance();
        }
        else{
            if (manager.getNumberOfInstances() < MAXIMUM_NUMBER_OF_INSTANCES){
                if (manager.getTotalAvailableLoad() < MINIMUM_LOAD_AVAILABLE){
                    scaleUp();
                }
            }
        }*/
    }  



    public void scaleUp() {
        manager.createInstance();
    }  

    public void scaleDown(String instanceID){
        /*if (ec2instances.size() > MINIMUM_NUMBER_OF_INSTANCES) {
            manager.deleteInstance(instanceID);
        }*/
    }

    
}
