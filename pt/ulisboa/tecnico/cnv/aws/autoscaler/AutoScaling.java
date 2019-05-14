
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import pt.ulisboa.tecnico.cnv.aws.AmazonClient;


public class AutoScaling {

    public static void createAutoScaling(String region, String instancesImageId, String instanceSecurityGroup, String keyPairName, String loadBalancerName) {
        final String autoScalingConfigurationName = "CNV-Autoscaling-Launch-Configuration";
        final String autoScalingGroupName = "CNV-Autoscaling-Group";
        final Region regionObject = new Region().withRegionName(region);

        // Create Auto Scaling Launch Configuration
        createLaunchConfiguration(regionObject, autoScalingConfigurationName, instancesImageId, instanceSecurityGroup, keyPairName);

        // Create Auto Scaling Group
        createAutoScalingGroup(regionObject, autoScalingGroupName, autoScalingConfigurationName, loadBalancerName);

        // Add scale up and scale down policies to the group
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

        AmazonClient.getASInstanceForRegion(region)
                .createLaunchConfiguration(createLaunchConfigurationRequest);
    }

    private static void createAutoScalingGroup(Region region, String autoScalingGroupName, String launchConfigurationName, String loadBalancerName) {
        CreateAutoScalingGroupRequest createAutoScalingGroupRequest = new CreateAutoScalingGroupRequest();
        createAutoScalingGroupRequest.withAutoScalingGroupName(autoScalingGroupName)
                .withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(getAvailabilityZonesFor(region))
                .withLoadBalancerNames(loadBalancerName)
                .withMinSize(1)
                .withMaxSize(5)
                .withHealthCheckType("ELB")
                .withHealthCheckGracePeriod(60);

        AmazonClient.getASInstanceForRegion(region)
                .createAutoScalingGroup(createAutoScalingGroupRequest);
    }

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
        predefinedMetricSpecification.withResourceLabel("ASGAverageCPUUtilization")
                .withPredefinedMetricType(MetricType.ASGAverageCPUUtilization);


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
