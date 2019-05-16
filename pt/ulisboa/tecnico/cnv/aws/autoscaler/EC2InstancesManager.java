package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;

import java.util.ResourceBundle;


import java.util.ResourceBundle;
import java.util.Locale;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.Collection;
import java.util.List;




public class EC2InstancesManager{


	static EC2InstancesManager  instance;


    String ec2InstanceID;


    private static final Comparator<EC2InstanceController> instanceComparator = new Comparator<EC2InstanceController>() {
        @Override
        public int compare(EC2InstanceController i1, EC2InstanceController i2) {
            int smallestLoad = (i1.getLoad() < i2.getLoad()) ? i1.getLoad() : i2.getLoad();
            return smallestLoad;
        }
    };

    private HashMap<String, EC2InstanceController> ec2instances = new HashMap<String, EC2InstanceController>();

    public static EC2InstancesManager getInstance() {
    	if (instance == null){ 
    		instance = new EC2InstancesManager();
    	}
    	return instance;
    }

    public int getNumberInstances() { return ec2instances.size();}


    public void addInstance(EC2InstanceController instance){
        ec2instances.put(instance.getInstanceID(), instance);
/*        ec2instancesLoads.put(instance.getInstanceID(), 0);
*/    }

    public void removeInstance(EC2InstanceController instance){
        ec2instances.remove(instance.getInstanceID());
/*        ec2instancesLoads.remove(instance.getInstanceID());        
*/    }


    public int calculateTotalClusterLoad(){
    	int totalClusterLoad = 0;
        for (EC2InstanceController instance : ec2instances.values()){
            totalClusterLoad += instance.getLoad();
        }
        return totalClusterLoad;
    }


    public int getClusterAvailableLoad(){
    	int totalLoadPossible = 10 * ec2instances.size();
    	int availableClusterLoad = totalLoadPossible - this.calculateTotalClusterLoad();
    	return availableClusterLoad;
    }


    public void createInstance() {
        EC2InstanceController instance = EC2InstanceController.requestNewEC2Instance();
        addInstance(instance);
    }


    public void deleteInstance(String instanceID){
        if (ec2instances.containsKey(instanceID)){
        	EC2InstanceController instance = ec2instances.get(instanceID);
            removeInstance(instance);
            instance.shutDownEC2Instance();
        }
        else{
        	System.out.println("There was no instance with this ID");
        }
    }

   /* public void increaseTotalLoad(int newRequestLoad){
    	totalClusterLoad += newRequestLoad;
    }

    public void decreaseInstanceLoad(int finishedRequestLoad){
    	totalClusterLoad += newRequestLoad;
    }*/

    public String getInstanceWithSmallerLoad(){
    	List<EC2InstanceController> instances = (List)ec2instances.values();
        Collections.sort(instances, instanceComparator);
        return instances.get(0).getInstanceID();
    }

}