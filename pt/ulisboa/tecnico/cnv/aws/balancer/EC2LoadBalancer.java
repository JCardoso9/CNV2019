
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
package pt.ulisboa.tecnico.cnv.aws.balancer;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import pt.ulisboa.tecnico.cnv.aws.AmazonClient;

import java.util.*;

import static java.lang.Integer.parseInt;

public class EC2LoadBalancer {

    /*
     * Before running the code: Fill in your AWS access credentials in the provided
     * credentials file template, and be sure to move the file to the default
     * location (~/.aws/credentials) where the sample code will load the credentials
     * from. https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING: To avoid accidental leakage of your credentials, DO NOT keep the
     * credentials file in your source directory.
     */

    private static ResourceBundle properties;
    private static String REGION = "region";
    private static String LOAD_BALANCER_AMI_ID = "load_balancer_ami_id";
    private static String LOAD_BALANCER_NAME = "load_balancer_name";
    private static String INSTANCE_TYPE = "instance_type";
    private static String KEY_PAIR_NAME = "key_pair_name";
    private static String SECURITY_GROUP = "security_group";
    private static String IDLE_TIMEOUT = "idle_timeout";

    public static void main(String[] args) throws Exception {

        init();

        final Region regionObject = new Region().withRegionName(properties.getString(REGION));
        final String loadBalancerName = properties.getString(LOAD_BALANCER_NAME);
        int idleTimeout = 0;

        try {
            idleTimeout = parseInt(properties.getString(IDLE_TIMEOUT), 10);

            // Set idle timeout to 60 seconds in case an invalid value is provided
            if (idleTimeout <= 0) {
                idleTimeout = 60;
            }
        } catch (NumberFormatException ex) {
            idleTimeout = 60;
        }

        ElasticLoadBalancer.createLoadBalancer(loadBalancerName, regionObject.getRegionName(), idleTimeout);

        registerRunningInstancesWithLoadBalancer(loadBalancerName, regionObject);

    }

    private static void init() throws Exception {
        try {
            properties = ResourceBundle.getBundle("ec2", Locale.ENGLISH);

            checkResourceBundleKeys(properties);
        } catch (Exception ex) {
            throw new AmazonClientException("Cannot load the properties file. "
                    + "Please make sure that the \"ec2_en.properties\" file is at the correct "
                    + "location, and is in valid format.", ex);
        }
    }

    private static void checkResourceBundleKeys(ResourceBundle props) throws Exception {
        // If the required properties exist and have a value
        if (!(props.containsKey(REGION)
                && props.containsKey(LOAD_BALANCER_AMI_ID)
                && props.containsKey(LOAD_BALANCER_NAME)
                && props.containsKey(INSTANCE_TYPE)
                && props.containsKey(KEY_PAIR_NAME)
                && props.containsKey(SECURITY_GROUP))
            || !(!props.getString(REGION).isEmpty()
                && !props.getString(LOAD_BALANCER_AMI_ID).isEmpty()
                && !props.getString(LOAD_BALANCER_NAME).isEmpty()
                && !props.getString(INSTANCE_TYPE).isEmpty()
                && !props.getString(KEY_PAIR_NAME).isEmpty()
                && !props.getString(SECURITY_GROUP).isEmpty())) {
            throw new Exception("Cannot load properties from file or they have no value." +
                    " Make sure all properties have been declared.");
        }
    }

    private static void registerRunningInstancesWithLoadBalancer(String loadBalancerName, Region region) {

        // get the running instances
        DescribeInstancesResult describeInstancesRequest = AmazonClient
                .getEC2InstanceForRegion(region)
                .describeInstances();

        List<Reservation> reservations = describeInstancesRequest.getReservations();
        List<Instance> instances = new ArrayList<>();

        for (Reservation reservation : reservations)
        {
            instances.addAll(reservation.getInstances());
        }

        // get instance id's
        // Transform ec2 instances into elb instances
        if (reservations.size() == 0) {
            return;
        }

        String id;
        List<com.amazonaws.services.elasticloadbalancing.model.Instance> instanceId = new ArrayList<>();

        for (Instance instance : instances) {
            id = instance.getInstanceId();
            // Get only those that are in the correct state
            if (instance.getState().getName().equals(InstanceStateName.Running.toString())) {
                instanceId.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(id));
            }
        }

        // register the instances to the load balancer
        RegisterInstancesWithLoadBalancerRequest register = new RegisterInstancesWithLoadBalancerRequest();
        register.setLoadBalancerName(loadBalancerName);
        register.setInstances(instanceId);

        RegisterInstancesWithLoadBalancerResult registerWithLoadBalancerResult = AmazonClient
                .getELBInstanceForRegion(region)
                .registerInstancesWithLoadBalancer(register);

        for (com.amazonaws.services.elasticloadbalancing.model.Instance instance : registerWithLoadBalancerResult.getInstances()) {
            System.out.println(instance.getInstanceId());
        }
    }
}
