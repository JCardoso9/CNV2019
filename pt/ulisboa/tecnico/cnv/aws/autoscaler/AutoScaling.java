
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
import com.amazonaws.services.applicationautoscaling.model.PolicyType;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.autoscaling.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import pt.ulisboa.tecnico.cnv.aws.AmazonClient;


public class AutoScaling {

    final static String autoScalingConfigurationName = "CNV-Autoscaling-Launch-Configuration";
    final static String autoScalingGroupName = "CNV-Autoscaling-Group";

    public static void createAutoScaling(String region, String instancesImageId, String instanceSecurityGroup, String keyPairName, String loadBalancerName) {
        
        final Region regionObject = new Region().withRegionName(region);

        // Create Auto Scaling Launch Configuration
        createLaunchConfiguration(regionObject, autoScalingConfigurationName, instancesImageId, instanceSecurityGroup, keyPairName);
        
        // Create Auto Scaling Group
        System.out.println("Creating AS Group...");
        createAutoScalingGroup(regionObject, autoScalingGroupName, autoScalingConfigurationName, loadBalancerName);

        // Add scale up and scale down policies to the group
        System.out.println("Adding Scaling Policies...");
        addScalingPoliciesToGroup(autoScalingGroupName, regionObject);
    }

    private static void createLaunchConfiguration(Region region, String configurationName, String imageId, String securityGroupName, String keyPairName) {


        com.amazonaws.services.autoscaling.model.InstanceMonitoring instanceMonitoring = new com.amazonaws.services.autoscaling.model.InstanceMonitoring();
        instanceMonitoring.withEnabled(true);

        CreateLaunchConfigurationRequest createLaunchConfigurationRequest = new CreateLaunchConfigurationRequest();
        createLaunchConfigurationRequest.withImageId(imageId)
                .withInstanceType(InstanceType.T2Micro.toString())
                .withLaunchConfigurationName(configurationName)
                .withInstanceMonitoring(instanceMonitoring)
                .withSecurityGroups(securityGroupName)
                .withKeyName(keyPairName);

        System.out.println("Checking if launch configuration exists before creating...");
        AmazonAutoScaling client = AmazonAutoScalingClientBuilder.standard().withRegion("us-east-1").build();  
        DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(autoScalingConfigurationName);
        DescribeLaunchConfigurationsResult response = client.describeLaunchConfigurations(request);

        //does not exist
        if (response.getLaunchConfigurations().isEmpty()){
            System.out.println("Creating new launch configuration...");
            AmazonClient.getASInstanceForRegion(region)
                .createLaunchConfiguration(createLaunchConfigurationRequest);
        }

        // exists and must be replaced with new one
        else{
            System.out.println("Deleting previous launch configuration...");
            deleteLaunchConfiguration(region, autoScalingConfigurationName);
            System.out.println("Creating new launch configuration...");
            AmazonClient.getASInstanceForRegion(region)
                .createLaunchConfiguration(createLaunchConfigurationRequest);
        }

        
    }


    private static void deleteLaunchConfiguration(Region region, String configurationName) {
        deleteAutoScalingGroup(region, autoScalingGroupName);
        System.out.println("Checking if launch configuration exists before deleting...");    
        DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(autoScalingConfigurationName);
        DescribeLaunchConfigurationsResult response = AmazonClient.getASInstanceForRegion(region).describeLaunchConfigurations(request);

        if (response.getLaunchConfigurations().isEmpty()){
            System.out.println("There was no launch configuration to delete");
        }


        else{
            System.out.println("Deleting launch configuration...");
            DeleteLaunchConfigurationRequest deleteRequest = new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(autoScalingConfigurationName);
            DeleteLaunchConfigurationResult deleteResponse = AmazonClient.getASInstanceForRegion(region).deleteLaunchConfiguration(deleteRequest);
        }
    }

    private static void createAutoScalingGroup(Region region, String autoScalingGroupName, String launchConfigurationName, String loadBalancerName) {
        // creating request
        CreateAutoScalingGroupRequest createAutoScalingGroupRequest = new CreateAutoScalingGroupRequest();
        createAutoScalingGroupRequest.withAutoScalingGroupName(autoScalingGroupName)
                .withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(getAvailabilityZonesFor(region))
                .withLoadBalancerNames(loadBalancerName)
                .withMinSize(1)
                .withMaxSize(5)
                .withHealthCheckType("ELB")
                .withHealthCheckGracePeriod(60);

        System.out.println("Checking if auto-scaler group exists before creating..."); 
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
        DescribeAutoScalingGroupsResult response = AmazonClient.getASInstanceForRegion(region).describeAutoScalingGroups(request);

        //there were no auto scalers
        if (response.getAutoScalingGroups().isEmpty()){
            System.out.println("Creating new auto scaling group...");
            AmazonClient.getASInstanceForRegion(region)
                .createAutoScalingGroup(createAutoScalingGroupRequest);
        }

        // there was already an auto-scaler that must be replaced
        else{
            System.out.println("Deleting previous auto scaling group...");
            deleteAutoScalingGroup(region, autoScalingGroupName);
            System.out.println("Creating new auto scaling group...");
            AmazonClient.getASInstanceForRegion(region)
                .createAutoScalingGroup(createAutoScalingGroupRequest);
        }
    }


    private static void deleteAutoScalingGroup(Region region, String autoScalingGroupName) {
        System.out.println("Checking if auto-scaler group exists before deleting..."); 
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
        DescribeAutoScalingGroupsResult response = AmazonClient.getASInstanceForRegion(region).describeAutoScalingGroups(request);

        if (response.getAutoScalingGroups().isEmpty()){
            System.out.println("There was no auto-scaling group to delete");
        }

        else{
            System.out.println("Deleting auto-scaler group...");
            DeleteAutoScalingGroupRequest deleteRequest = new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroupName).withForceDelete(true);
            DeleteAutoScalingGroupResult deleteResponse = AmazonClient.getASInstanceForRegion(region).deleteAutoScalingGroup(deleteRequest);
            try{
                Thread.sleep(130000);
            } catch (InterruptedException e) {e.printStackTrace();}
        }

    }

    


    /*private static void deleteAutoScalingGroup(Region region, String autoScalingGroupName, String launchConfigurationName, String loadBalancerName) {
        CreateAutoScalingGroupRequest createAutoScalingGroupRequest = new CreateAutoScalingGroupRequest();
        createAutoScalingGroupRequest.withAutoScalingGroupName(autoScalingGroupName)
                .withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(getAvailabilityZonesFor(region))
                //.withLoadBalancerNames(loadBalancerName)
                .withMinSize(1)
                .withMaxSize(5)
                .withHealthCheckType("ELB")
                .withHealthCheckGracePeriod(60);

        AmazonClient.getASInstanceForRegion(region)
                .createAutoScalingGroup(createAutoScalingGroupRequest);
    }*/

    private static void addScalingPoliciesToGroup(String groupName, Region region) {
//        MetricDimension metricDimension = new MetricDimension();
//        metricDimension.withName()
//                .withValue();
//
//        CustomizedMetricSpecification customizedMetricSpecification = new CustomizedMetricSpecification();
//        customizedMetricSpecification.withStatistic(MetricStatistic.Average)
//                .withUnit("Percent")
//                .withNamespace()
//                .withMetricName("CloudWatch-CPU-Percent-Average")
//                .withDimensions(metricDimension);
        PredefinedMetricSpecification predefinedMetricSpecification = new PredefinedMetricSpecification();
        predefinedMetricSpecification.withPredefinedMetricType(MetricType.ASGAverageCPUUtilization);


        TargetTrackingConfiguration targetTrackingConfiguration = new TargetTrackingConfiguration();
        targetTrackingConfiguration.withTargetValue(60.0)
                .withDisableScaleIn(false)
//                .withCustomizedMetricSpecification(customizedMetricSpecification)
                .withPredefinedMetricSpecification(predefinedMetricSpecification);

        PutScalingPolicyRequest scalingPolicyRequest = new PutScalingPolicyRequest();
        scalingPolicyRequest.withPolicyName("ScaleUp")
                .withAutoScalingGroupName(groupName)
                .withPolicyType(PolicyType.TargetTrackingScaling.toString())
                .withPolicyName("CNV-AutoScaling-CPUUtilization-scale-up")
                .withTargetTrackingConfiguration(targetTrackingConfiguration)
                .withEstimatedInstanceWarmup(180);

        AmazonClient.getASInstanceForRegion(region)
                .putScalingPolicy(scalingPolicyRequest);
    }

    private static void enableMetricsCollection() {
        // TODO:
    }

    private static Collection<String> getAvailabilityZonesFor(Region region) {
        DescribeAvailabilityZonesRequest zonesRequest = new DescribeAvailabilityZonesRequest();

        // Reduce the returning zones to the ones of the specified region
        List<Filter> filters = new ArrayList<>(1);
        filters.add(new Filter()
                .withName("region-name")
                .withValues(region.getRegionName()));

        zonesRequest.withFilters(filters);

        DescribeAvailabilityZonesResult zonesResponse = AmazonClient
                .getEC2InstanceForRegion(region)
                .describeAvailabilityZones(zonesRequest);

        List<String> availabilityZones = new ArrayList<>();
        for (AvailabilityZone zone : zonesResponse.getAvailabilityZones()) {
            availabilityZones.add(zone.getZoneName());
        }

        return availabilityZones;
    }
}
