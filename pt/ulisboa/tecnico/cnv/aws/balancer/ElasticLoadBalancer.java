
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

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.elasticloadbalancing.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ElasticLoadBalancer {

    public static void createLoadBalancer(String loadBalancerName, String region, int idleTimeout) {
        Region regionObject = new Region()
                .withRegionName(region);

        // Contains the parameters for createLoadBalancer.
        CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
        lbRequest.setLoadBalancerName(loadBalancerName);
        lbRequest.setAvailabilityZones(getAvailabilityZonesFor(regionObject));
        lbRequest.setListeners(getListeners());
        lbRequest.setSecurityGroups(getSecurityGroups(regionObject));

        // Create Load Balancer
        CreateLoadBalancerResult lbResult = AmazonClient
                .getELBInstanceForRegion(regionObject)
                .createLoadBalancer(lbRequest);

        // Configure Health check
        configureHealthCheck(regionObject, loadBalancerName);

        // Configure the  number of seconds to wait before an idle connection is closed.
        configureIdleConnectionTimeout(regionObject, loadBalancerName, idleTimeout);

        System.out.println("Load Balancer created: " + lbResult.getDNSName());
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

    private static Collection<Listener> getListeners() {
        Collection<Listener> listeners = new ArrayList<>(1);
        listeners.add(new Listener("HTTP", 80, 8000));

        return listeners;
    }

    private static Collection<String> getSecurityGroups(Region region) {
        CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
        csgr.withGroupName("CNV-LoadBalancer-SecurityGroup")
                .withDescription("CNV Load Balancer Security Group");

        // Create new security group for the load balancer
        CreateSecurityGroupResult createSecurityGroupResult = AmazonClient
                .getEC2InstanceForRegion(region)
                .createSecurityGroup(csgr);

        // Configure security group to allow inboud traffic
        IpPermission ipPermission = new IpPermission();
        IpRange ipRangeAnywhere = new IpRange().withCidrIp("0.0.0.0/0");

        ipPermission.withIpv4Ranges(Collections.singletonList(ipRangeAnywhere))
                .withIpProtocol("tcp")
                .withFromPort(80)
                .withToPort(80);

        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
                new AuthorizeSecurityGroupIngressRequest();

        authorizeSecurityGroupIngressRequest
                .withGroupId(createSecurityGroupResult.getGroupId())
                .withIpPermissions(ipPermission);

        AmazonClient.getEC2InstanceForRegion(region)
                .authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);


        return Collections.singletonList(createSecurityGroupResult.getGroupId());
    }

    private static void configureHealthCheck(Region region, String loadBalancerName) {
        HealthCheck healthCheck = new HealthCheck();
        // Ping protocol, ping port and ping path
        healthCheck.setTarget("HTTP:8000/test");
        // Amount of time between health checks (5 sec - 300 sec).
        healthCheck.setInterval(60);
        // Time to wait when receiving a response from the health check (2 sec - 60 sec).
        healthCheck.setTimeout(10);
        // Number of consecutive health check failures before declaring an EC2 instance unhealthy.
        healthCheck.setUnhealthyThreshold(3);
        // Number of consecutive health check successes before declaring an EC2 instance healthy.
        healthCheck.setHealthyThreshold(10);

        ConfigureHealthCheckRequest configureHealthCheckRequest = new ConfigureHealthCheckRequest();
        configureHealthCheckRequest.setLoadBalancerName(loadBalancerName);
        configureHealthCheckRequest.setHealthCheck(healthCheck);

        AmazonClient.getELBInstanceForRegion(region).configureHealthCheck(configureHealthCheckRequest);
    }

    private static void configureIdleConnectionTimeout(Region region, String loadBalancerName, int idleTimeout) {
        ConnectionSettings connectionSettings = new ConnectionSettings();
        connectionSettings.setIdleTimeout(idleTimeout);

        LoadBalancerAttributes loadBalancerAttributes = new LoadBalancerAttributes();
        loadBalancerAttributes.withConnectionSettings(connectionSettings);

        ModifyLoadBalancerAttributesRequest modifyLoadBalancerAttributesRequest = new ModifyLoadBalancerAttributesRequest();
        modifyLoadBalancerAttributesRequest.setLoadBalancerName(loadBalancerName);
        modifyLoadBalancerAttributesRequest.setLoadBalancerAttributes(loadBalancerAttributes);

        AmazonClient.getELBInstanceForRegion(region)
                .modifyLoadBalancerAttributes(modifyLoadBalancerAttributesRequest);
    }
}
