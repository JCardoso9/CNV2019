package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.*;
import pt.ulisboa.tecnico.cnv.aws.AmazonClient;
import pt.ulisboa.tecnico.cnv.aws.observer.AbstractInstanceObservable;
import pt.ulisboa.tecnico.cnv.parser.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;



public class EC2InstanceController extends AbstractInstanceObservable {

    enum InstanceStatus {
      Available,
      MarkedForShutdown
    }

    private InstanceStatus status = InstanceStatus.Available;
    private static ResourceBundle properties;
    private static String REGION = "region";
    private static String INSTANCE_AMI_ID = "instance_ami_id";
    private static String INSTANCE_TYPE = "instance_type";
    private static String KEY_PAIR_NAME = "key_pair_name";
    private static String SECURITY_GROUP = "security_group";
    private static List<Request> requestList = new ArrayList<Request>();
    private static int currentLoad = 0;


    static Region regionObject;

    private EC2InstancesManager manager;

    private String ec2InstanceID;
    private String ec2InstanceIP;


    private EC2InstanceController(String ec2InstanceID, String ec2InstanceIP) {
        this.ec2InstanceID = ec2InstanceID;
        this.ec2InstanceIP = ec2InstanceIP;

        manager = EC2InstancesManager.getInstance();

    }

    public static EC2InstanceController requestNewEC2Instance()  {

        try {
            init();
        } catch(Exception e) {
            e.printStackTrace();
        }

        regionObject = new Region().withRegionName(properties.getString(REGION));
        RunInstancesRequest runInstancesRequest =
                new RunInstancesRequest();

        System.out.println("Creating EC2 instance... ");
        runInstancesRequest
                .withImageId(properties.getString(INSTANCE_AMI_ID))
                .withInstanceType((properties.getString(INSTANCE_TYPE)))
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(properties.getString(KEY_PAIR_NAME))
                .withSecurityGroups(properties.getString(SECURITY_GROUP))
                //.withIamInstanceProfile(new IamInstanceProfileSpecification().withName(props.getString("render.iam.role.name")))
        ;
        RunInstancesResult runInstancesResult =

        AmazonClient.getEC2InstanceForRegion(regionObject).runInstances(runInstancesRequest);
        Instance instance = runInstancesResult.getReservation().getInstances().get(0);
        String instanceId =instance.getInstanceId();
        String instanceIp = instance.getPublicIpAddress();

        // while status not active blah blah
        System.out.println("EC2 instance created.");


        return new EC2InstanceController(instanceId, instanceIp);
    }


    public String  getInstanceID() {return ec2InstanceID;}

    public String getInstanceIP() {return ec2InstanceIP;}


    public void shutDownEC2Instance() {
        System.out.println("Shutting down instance with ID " + ec2InstanceID + "...");
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(ec2InstanceID);
        AmazonClient.getEC2InstanceForRegion(regionObject).terminateInstances(termInstanceReq);
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
                && props.containsKey(INSTANCE_AMI_ID)
                && props.containsKey(INSTANCE_TYPE)
                && props.containsKey(KEY_PAIR_NAME)
                && props.containsKey(SECURITY_GROUP))
            || !(!props.getString(REGION).isEmpty()
                && !props.getString(INSTANCE_AMI_ID).isEmpty()
                && !props.getString(INSTANCE_TYPE).isEmpty()
                && !props.getString(KEY_PAIR_NAME).isEmpty()
                && !props.getString(SECURITY_GROUP).isEmpty())) {
            throw new Exception("Cannot load properties from file or they have no value." +
                    " Make sure all properties have been declared.");
        }
    }

    public static void addNewRequest(Request request){
        requestList.add(request);
    }

    public static void removeRequest(Request request){
        requestList.remove(request);
    }

    public static int getLoad(){
        return currentLoad;
    }

    public static void setLoad(int load){
        currentLoad = load;
    }

    public boolean isMarkedForShutdown(){
        return this.status == InstanceStatus.MarkedForShutdown;
    }

    public void markForShutdown(){
        this.status = InstanceStatus.MarkedForShutdown;
    }


    public void reActivate(){
        this.status = InstanceStatus.Available;
    }
}
