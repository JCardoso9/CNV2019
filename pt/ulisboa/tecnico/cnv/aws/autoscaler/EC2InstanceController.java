package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;

import pt.ulisboa.tecnico.cnv.aws.AmazonClient;
import pt.ulisboa.tecnico.cnv.aws.observer.AbstractInstanceObservable;
import pt.ulisboa.tecnico.cnv.parser.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Timer;
import java.util.TimerTask;




public class EC2InstanceController extends AbstractInstanceObservable {

    enum InstanceStatus {
      Pending,
      Available,
      MarkedForShutdown,
      Unhealthy
    }

    private InstanceStatus status = InstanceStatus.Pending;
    private static ResourceBundle properties;
    private static String REGION = "region";
    private static String INSTANCE_AMI_ID = "instance_ami_id";
    private static String INSTANCE_TYPE = "instance_type";
    private static String KEY_PAIR_NAME = "key_pair_name";
    private static String SECURITY_GROUP = "security_group";
    private static List<Request> requestList = new ArrayList<Request>();
    private int currentLoad = 0;

    private static String region;

    private int STATUS_CHECK_INTERVAL =  10000;
    private int PORT =  8000;


    private EC2InstancesManager manager;

    private String ec2InstanceID;
    private String ec2InstanceIP;

    private String ec2InstanceAdress;


    private EC2InstanceController(String ec2InstanceID) {
        this.ec2InstanceID = ec2InstanceID;

        manager = EC2InstancesManager.getInstance();

        Timer timer = new Timer();
        timer.schedule(new WaitForIP(), STATUS_CHECK_INTERVAL, STATUS_CHECK_INTERVAL);

    }

    public static EC2InstanceController requestNewEC2Instance()  {

        try {
            init();
        

/*        region = new Region().withRegionName(properties.getString(REGION));
*/        region = properties.getString(REGION);
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
        RunInstancesResult runInstancesResult = AmazonClient.getEC2InstanceForRegion(region).runInstances(runInstancesRequest);
        Instance instance = runInstancesResult.getReservation().getInstances().get(0);
        String instanceId =instance.getInstanceId();


        System.out.println("EC2 instance created.");
        System.out.println("ID: " + instanceId);


        return new EC2InstanceController(instanceId);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public String  getInstanceID() {return ec2InstanceID;}

    public String getInstanceIP() {return ec2InstanceIP;}

    public String getInstanceAdress() {return ec2InstanceAdress;}


    public void shutDownEC2Instance() {
        System.out.println("Shutting down instance with ID " + ec2InstanceID + "...");
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(ec2InstanceID);
        AmazonClient.getEC2InstanceForRegion(region).terminateInstances(termInstanceReq);
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

    public synchronized void addNewRequest(Request request){
        currentLoad += request.getEstimatedCost();
        requestList.add(request);
    }

    public synchronized void removeRequest(Request request){
        currentLoad -= request.getEstimatedCost();
        requestList.remove(request);
    }

    public synchronized int getLoad(){
        return currentLoad;
    }

    public boolean isMarkedForShutdown(){
        return this.status == InstanceStatus.MarkedForShutdown;
    }

    public void markForShutdown(){
        this.status = InstanceStatus.MarkedForShutdown;
    }

    public boolean isPending(){
        return this.status == InstanceStatus.Pending;
    }

    public void reActivate(){
        this.status = InstanceStatus.Available;
    }

    public boolean checkHealth(){
        try{
            System.out.println("Checking instance " + ec2InstanceIP);
            URL url = new URL("http://" + ec2InstanceAdress + "/test");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();
            if(conn != null){
                reader.close();
                conn.disconnect();
            }


            return !response.isEmpty();

        }catch(Exception e){
            return false;
        }
    }

    public void createInstanceAddress(String instanceIp){
        ec2InstanceIP = instanceIp;
        ec2InstanceAdress = instanceIp + ":" + PORT;
    }



    private boolean checkIfAvailable(AmazonEC2 client){
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(ec2InstanceID);
        DescribeInstancesResult describeInstancesResult = client.describeInstances(describeInstancesRequest);
        InstanceState state = describeInstancesResult.getReservations().get(0).getInstances().get(0).getState();
        if(state.getName().equals(InstanceStateName.Pending.toString())){
            this.status = InstanceStatus.Pending;
            return false;
        }
        else if(state.getName().equals(InstanceStateName.Running.toString())){
            createInstanceAddress(describeInstancesResult.getReservations().get(0).getInstances().get(0).getPublicIpAddress());
            this.status = InstanceStatus.Available;
            manager.addInstance(this);
            System.out.println("Got Address: " + ec2InstanceAdress + " for instance " + ec2InstanceID);
            return true;
        }
        return false;
    }

    class WaitForIP extends TimerTask {

        public void run() {
            //System.out.println("Checking  if worker active yet: " + ec2InstanceID);
            if(status.equals(InstanceStatus.Pending)){
                if (checkIfAvailable(AmazonClient.getEC2InstanceForRegion(region))) {
                    this.cancel();
                }
            } else {
                this.cancel();
            }
        }
    }
}
