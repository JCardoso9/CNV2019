package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.AmazonClientException;

import java.util.ResourceBundle;
import java.util.Locale;





public class EC2InstanceController{


    private static ResourceBundle properties;
    private static String REGION = "region";
    private static String AUTO_SCALER_AMI_ID = "auto_scaler_ami_id";
    private static String AUTO_SCALER_NAME = "auto_scaler_name";
    private static String LOAD_BALANCER_NAME = "load_balancer_name";
    private static String INSTANCE_TYPE = "instance_type";
    private static String KEY_PAIR_NAME = "key_pair_name";
    private static String SECURITY_GROUP = "security_group";
    private static String IDLE_TIMEOUT = "idle_timeout";

    static String ec2InstanceID;


    public EC2InstanceController(String ec2InstanceID) {
        this.ec2InstanceID = ec2InstanceID;
    }

	public static EC2InstanceController requestNewEC2Instance(AmazonEC2 client) throws Exception {

        init();
        RunInstancesRequest runInstancesRequest =
                new RunInstancesRequest();

        System.out.println("Creating EC2 instance... ");
        runInstancesRequest.withImageId(properties.getString(AUTO_SCALER_AMI_ID))
                .withInstanceType((properties.getString(INSTANCE_TYPE)))
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(properties.getString(KEY_PAIR_NAME))
                .withSecurityGroups(properties.getString(SECURITY_GROUP))
                //.withIamInstanceProfile(new IamInstanceProfileSpecification().withName(props.getString("render.iam.role.name")))
        ;
        RunInstancesResult runInstancesResult =
                client.runInstances(runInstancesRequest);
        String instanceId = runInstancesResult.getReservation().getInstances()
                .get(0).getInstanceId();
        System.out.println("EC2 instance created.");



        return new EC2InstanceController(instanceId);
    }




    public static synchronized void shutDownEC2Instance(AmazonEC2 client) {
        System.out.println("Shutting down instance with ID " + ec2InstanceID + "...");
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(ec2InstanceID);
        client.terminateInstances(termInstanceReq);
        System.out.println("EC2 instance terminated. ");
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
                && props.containsKey(AUTO_SCALER_AMI_ID)
                && props.containsKey(AUTO_SCALER_NAME)
                && props.containsKey(INSTANCE_TYPE)
                && props.containsKey(KEY_PAIR_NAME)
                && props.containsKey(SECURITY_GROUP))
            || !(!props.getString(REGION).isEmpty()
                && !props.getString(AUTO_SCALER_AMI_ID).isEmpty()
                && !props.getString(AUTO_SCALER_NAME).isEmpty()
                && !props.getString(INSTANCE_TYPE).isEmpty()
                && !props.getString(KEY_PAIR_NAME).isEmpty()
                && !props.getString(SECURITY_GROUP).isEmpty())) {
            throw new Exception("Cannot load properties from file or they have no value." +
                    " Make sure all properties have been declared.");
        }
    }



}