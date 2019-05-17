package pt.ulisboa.tecnico.cnv.aws;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;

public class AmazonClient {
    private static AmazonAutoScaling as;
    private static AmazonElasticLoadBalancing elb;
    private static AmazonEC2 ec2;
    private static AWSStaticCredentialsProvider awsCredentials;

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed automatically.
     * Client parameters, such as proxies, can be specified in an optional
     * ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private static AWSStaticCredentialsProvider getCredentials() throws AmazonClientException {

        if (awsCredentials != null) {
            return awsCredentials;
        }

        /*
         * The ProfileCredentialsProvider will return your [default] credential profile
         * by reading from the credentials file located at (~/.aws/credentials).
         */
        try {
            awsCredentials = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
        } catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
        }

        return awsCredentials;
    }

    public static AmazonEC2 getEC2InstanceForRegion(String regionName) {
        if (ec2 == null) {
            ec2 = AmazonEC2ClientBuilder
                    .standard()
                    .withRegion(regionName)
                    .withCredentials(getCredentials())
                    .build();
        }

        return ec2;
    }

    public static AmazonElasticLoadBalancing getELBInstanceForRegion(Region region) {
        if (elb == null) {
            elb = AmazonElasticLoadBalancingClientBuilder
                    .standard()
                    .withRegion(region.getRegionName())
                    .withCredentials(getCredentials())
                    .build();
        }

        return elb;
    }

    public static AmazonAutoScaling getASInstanceForRegion(String regionName) {
        if (as == null) {
            as = AmazonAutoScalingClientBuilder
                    .standard()
                    .withRegion(regionName)
                    .withCredentials(getCredentials())
                    .build();
        }

        return as;
    }
}
