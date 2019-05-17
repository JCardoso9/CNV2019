package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import pt.ulisboa.tecnico.cnv.aws.observer.AbstractManagerObservable;

import java.util.*;
import pt.ulisboa.tecnico.cnv.parser.Request;


public class EC2InstancesManager extends AbstractManagerObservable {


	static EC2InstancesManager  instance;


    String ec2InstanceID;


    private HashMap<String, EC2InstanceController> ec2instances = new HashMap<String, EC2InstanceController>();
    private HashMap<String, Integer> ec2instancesLoads = new HashMap<String, Integer>();


    private static final Comparator<EC2InstanceController> instanceComparator = new Comparator<EC2InstanceController>() {
        @Override
        public int compare(EC2InstanceController i1, EC2InstanceController i2) {
            int smallestLoad = (i1.getLoad() < i2.getLoad()) ? i1.getLoad() : i2.getLoad();
            return smallestLoad;
        }
    };


    public synchronized static EC2InstancesManager getInstance() {
    	if (instance == null){ 
    		instance = new EC2InstancesManager();
    	}
    	return instance;
    }

    public int getNumberInstances() { return ec2instances.size();}


    public void addInstance(EC2InstanceController instance){
        ec2instances.put(instance.getInstanceID(), instance);
        ec2instancesLoads.put(instance.getInstanceID(), 0);
    }

    public void removeInstance(EC2InstanceController instance){
        ec2instances.remove(instance.getInstanceID());
        ec2instancesLoads.remove(instance.getInstanceID());     
    }


    public List<String> getIdleInstances(){
    	List<String> idleInstances = new ArrayList<String>();
    	for (String instanceID : ec2instancesLoads.keySet()){
            if (ec2instancesLoads.get(instanceID) == 0)
            	idleInstances.add(instanceID);
        }
        return idleInstances;
    }


    public int calculateTotalClusterLoad(){
    	int totalClusterLoad = 0;
        for (int load : ec2instancesLoads.values()){
            totalClusterLoad += load;
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


    public void markForShutdown(String instanceID){
    	ec2instances.get(instanceID).markForShutdown();
    }

   /* public void increaseTotalLoad(int newRequestLoad){
    	totalClusterLoad += newRequestLoad;
    }

    public void decreaseInstanceLoad(int finishedRequestLoad){
    	totalClusterLoad += newRequestLoad;
    }*/

    public EC2InstanceController getInstanceWithSmallerLoad(Request request){
    	List<EC2InstanceController> instances = (List)ec2instances.values();
        Collections.sort(instances, instanceComparator);
        int bestIndex = 0;
        EC2InstanceController bestInstance = instances.get(bestIndex);
        while (bestInstance.isMarkedForShutdown() && bestIndex < instances.size()){
            bestIndex++;
            bestInstance = instances.get(bestIndex);
        }
        //Notify auto scaler
        bestInstance.addNewRequest(request);
        return bestInstance;
    }

    public void removeRequest(String instanceID, Request request){
        if (ec2instances.containsKey(instanceID)){
            EC2InstanceController instance = ec2instances.get(instanceID);
            instance.removeRequest(request);
            //Notify auto scaler;
        }
    }
}
