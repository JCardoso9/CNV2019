
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
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import pt.ulisboa.tecnico.cnv.aws.AmazonClient;
import pt.ulisboa.tecnico.cnv.aws.balancer.*;

import pt.ulisboa.tecnico.cnv.aws.observer.AbstractAutoScalerObserver;




import static java.lang.Integer.parseInt;


public class EC2AutoScaler extends AbstractAutoScalerObserver implements Runnable{


    /*
     * Before running the code: Fill in your AWS access credentials in the provided
     * credentials file template, and be sure to move the file to the default
     * location (~/.aws/credentials) where the sample code will load the credentials
     * from. https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING: To avoid accidental leakage of your credentials, DO NOT keep the
     * credentials file in your source directory.
     */

    enum InstanceStatus {
      Marked,
      UnMarked
    }

    private static int MAXIMUM_NUMBER_OF_INSTANCES = 10;
    private static int MINIMUM_NUMBER_OF_INSTANCES = 1;
    public static int MAXIMUM_REQUEST_COMPLEXITY = 10;

    // ver isso depois
    private static long MAXIMUM_LOAD_POSSIBLE = MAXIMUM_NUMBER_OF_INSTANCES * MAXIMUM_REQUEST_COMPLEXITY;

    private static long MINIMUM_LOAD_AVAILABLE = MINIMUM_NUMBER_OF_INSTANCES * MAXIMUM_REQUEST_COMPLEXITY;

    private int NUMBER_OF_MILISECONDS_BEFORE_SHUTDOWN = 30 * 1000;

    private int NUMBER_OF_SECONDS_BETWEEN_AS_LOGIC = 60 ;

    private EC2InstancesManager manager;

    static EC2AutoScaler  instance;


    boolean scaledUp = false;

    private List<String> idleInstances = new ArrayList<String>();

    private HashMap<String, Timer> timers = new HashMap<String, Timer>();


    private EC2AutoScaler(){
        super(EC2InstancesManager.getInstance());

        manager = EC2InstancesManager.getInstance();
        
        Timer autoScalerAwaken = new Timer();
        autoScalerAwaken.schedule(new RunAutoScalerLogic(), NUMBER_OF_SECONDS_BETWEEN_AS_LOGIC * 1000, NUMBER_OF_SECONDS_BETWEEN_AS_LOGIC * 1000);
    }

    public synchronized static EC2AutoScaler getInstance() {
        if (instance == null){ 
            instance = new EC2AutoScaler();
        }
        return instance;
    }

    public void run(){
        System.out.println("Starting instance...");
        scaleUp();

        /*try{
        
        Thread.sleep(20000);
        } catch (Exception e) {e.printStackTrace();}
        System.out.println("Starting 2nd instance...");
        scaleUp();*/
       
    }

    public void executeAutoScalerLogic(){
        scaledUp = false;
        System.out.println("Auto-Scaling...");
        System.out.println("Nr Of instances: " + manager.getNumberInstances());
        if(manager.getNumberInstances() < MINIMUM_NUMBER_OF_INSTANCES){
            if (!manager.isInstanceBeingCreated()) scaleUp();
        }

        else{

            //check if scale up is needed
            System.out.println("Available Load: " + manager.getClusterAvailableLoad());
            if (manager.getNumberInstances() < MAXIMUM_NUMBER_OF_INSTANCES){
                if (manager.getClusterAvailableLoad() < MINIMUM_LOAD_AVAILABLE){
                    if (!manager.isInstanceBeingCreated()) scaleUp();
                    scaledUp = true;
                }

            }

            if (!scaledUp){
                //check if scale down is needed
                List<String> updatedIdleInstances = manager.getIdleInstances();
                System.out.println("Idle: " + updatedIdleInstances);
                markForShutdown(updatedIdleInstances); 
            }

        }
    } 

    public void markForShutdown(List<String> updatedIdleInstances){
        int nrInstancesLeft = manager.getNumberInstances();
        int nrKilledInstances = 0;
        for (String instanceID : updatedIdleInstances){
            /*System.out.println("Instances Left: " + nrInstancesLeft);*/
            System.out.println("If instance " + instanceID + " deleted, system would have " + (manager.getClusterAvailableLoad() - nrKilledInstances * MAXIMUM_REQUEST_COMPLEXITY - manager.getAvailableLoadInstance(instanceID) ));
            if (!idleInstances.contains(instanceID) && nrInstancesLeft > MINIMUM_NUMBER_OF_INSTANCES && 
                manager.getClusterAvailableLoad() - nrKilledInstances * MAXIMUM_REQUEST_COMPLEXITY - manager.getAvailableLoadInstance(instanceID) >= MINIMUM_LOAD_AVAILABLE){

                System.out.println("Marking " + instanceID + " for shutdown...");
                idleInstances.add(instanceID);
                manager.markForShutdown(instanceID);
                startShutdownProcedure(instanceID);
                nrInstancesLeft -= 1;
                nrKilledInstances += 1;
            }
        }
    }

    public void startShutdownProcedure(String instanceID){
        if (idleInstances.contains(instanceID)){
            Timer timer = new Timer();
            timer.schedule(new ShutdownTimer(instanceID), NUMBER_OF_MILISECONDS_BEFORE_SHUTDOWN);
            timers.put(instanceID, timer);
            System.out.println("Started timer...");
        }
    }

    public void quitShutdownProcedure(String instanceID) {
        if (timers.containsKey(instanceID)  && idleInstances.contains(instanceID)){
            idleInstances.remove(instanceID);
            timers.get(instanceID).cancel();
            timers.remove(instanceID);
            manager.reActivate(instanceID);
            System.out.println("Shutdown for " + instanceID + " has been canceled");
        }
        else{
            System.out.println("Instance was not idling");
        }
    }


    public boolean isInstanceIdle(String instanceID){
        return idleInstances.contains(instanceID);
    }

    public void scaleUp() {
        manager.createInstance();
    }  

    public void scaleDown(String instanceID){

        timers.remove(instanceID);
        idleInstances.remove(instanceID);
        manager.deleteInstance(instanceID);

    }

    class RunAutoScalerLogic extends TimerTask {

        public void run() {
            executeAutoScalerLogic();            
        }
    }
}
