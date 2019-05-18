package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import pt.ulisboa.tecnico.cnv.aws.observer.AbstractManagerObservable;

import java.util.*;
import pt.ulisboa.tecnico.cnv.parser.Request;

import pt.ulisboa.tecnico.cnv.aws.observer.*;

import java.util.TimerTask;



public class EC2InstancesManager extends AbstractManagerObservable {


	static EC2InstancesManager  instance;

	int MAXIMUM_FAILED_HEALTH_CHECKS = 3;
	int SECONDS_BETWEEN_HEALTH_CHECKS = 40;


    String ec2InstanceID;


    private HashMap<String, EC2InstanceController> ec2instances = new HashMap<String, EC2InstanceController>();
    private HashMap<String, Integer> ec2InstancesHealth = new HashMap<String, Integer>();
/*    private HashMap<String, Integer> ec2instancesLoads = new HashMap<String, Integer>();
*/

    private static final Comparator<EC2InstanceController> instanceComparator = new Comparator<EC2InstanceController>() {
        @Override
        public int compare(EC2InstanceController i1, EC2InstanceController i2) {
            int smallestLoad = (i1.getLoad() < i2.getLoad()) ? i1.getLoad() : i2.getLoad();
            return smallestLoad;
        }
    };

    private EC2InstancesManager() {
    	//Timer timer = new Timer();
        //timer.schedule(new RunHealthCheckTimer(), SECONDS_BETWEEN_HEALTH_CHECKS * 1000, 50000);
    }


    public synchronized static EC2InstancesManager getInstance() {
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


    public List<String> getIdleInstances(){
    	List<String> idleInstances = new ArrayList<String>();
    	/*for (String instanceID : ec2instancesLoads.keySet()){
            if (ec2instancesLoads.get(instanceID) == 0)
            	idleInstances.add(instanceID);
        }*/
        for (EC2InstanceController instance : ec2instances.values()){
        	if (instance.getLoad() == 0) {
        		idleInstances.add(instance.getInstanceID());
        	}
        }
        return idleInstances;
    }




    public int calculateTotalClusterLoad(){
    	int totalClusterLoad = 0;
        /*for (int load : ec2instancesLoads.values()){
            totalClusterLoad += load;
        }*/
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


    public EC2InstanceController createInstance() {
        return EC2InstanceController.requestNewEC2Instance();

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

    public void reActivate(String instanceID){
    	ec2instances.get(instanceID).reActivate();
    }

   /* public void increaseTotalLoad(int newRequestLoad){
    	totalClusterLoad += newRequestLoad;
    }

    public void decreaseInstanceLoad(int finishedRequestLoad){
    	totalClusterLoad += newRequestLoad;
    }*/

    public synchronized EC2InstanceController getInstanceWithSmallerLoad(Request request){
        System.out.println("Getting best request");
    	ArrayList<EC2InstanceController> instances = new ArrayList<EC2InstanceController>(ec2instances.values());
        System.out.println("SIze : " + instances.size());
        Collections.sort(instances, instanceComparator);
        List<String> idleInstances = getIdleInstances();
        if (instances.isEmpty()){
            return null;
        }
        else{
            for (EC2InstanceController instance : instances){
                System.out.println("Instance ID: " + instance.getInstanceID() + "Instance load : " + instance.getLoad());
            }
            int bestIndex = 0;
            EC2InstanceController bestInstance = instances.get(bestIndex);
            while (bestInstance.isMarkedForShutdown() && bestIndex < instances.size()){
                bestIndex++;
                bestInstance = instances.get(bestIndex);
            }
            if (bestInstance.getLoad() > 0 && idleInstances.size() != 0){
                bestInstance = ec2instances.get(idleInstances.get(0));
                EC2AutoScaler.getInstance().quitShutdownProcedure(bestInstance.getInstanceID());
            }
            if (bestInstance.getLoad() + request.getEstimatedCost() > EC2AutoScaler.MAXIMUM_REQUEST_COMPLEXITY){
                //Should be through auto scaler
                EC2InstanceController newInstance = createInstance();
                while (newInstance.isPending()){
                    try{
                        Thread.sleep(5000);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                newInstance.addNewRequest(request);
                return newInstance;
            }
            bestInstance.addNewRequest(request);

            return bestInstance;
        }
    }

    public void removeRequest(String instanceID, Request request){
        if (ec2instances.containsKey(instanceID)){
            EC2InstanceController instance = ec2instances.get(instanceID);
            instance.removeRequest(request);
            //Notify auto scaler;
            EC2AutoScaler.getInstance().update(this, this);
        }
    }


    public synchronized void  checkInstances(){
    	for (EC2InstanceController instance : ec2instances.values()){
    		System.out.println("Pinging " + instance.getInstanceID());
        	boolean healthy = instance.checkHealth();
        	System.out.println("Healthy:  " + healthy);
        	updateHealthInstance(instance.getInstanceID(), healthy);
        }
    }

    public synchronized void  updateHealthInstance(String instanceID, boolean healthy){
    	if (!ec2InstancesHealth.containsKey(instanceID)) ec2InstancesHealth.put(instanceID,0);

    	else {
	    	int nrFailedChecks = ec2InstancesHealth.get(instanceID);

	    	if (healthy){
	    		if (nrFailedChecks > 0) ec2InstancesHealth.put(instanceID, 0);
	    	}

	    	else{
	    		if (nrFailedChecks == MAXIMUM_FAILED_HEALTH_CHECKS - 1){
	    			ec2instances.remove(instanceID);
	    			ec2InstancesHealth.remove(instanceID);
	    		}
	    		else ec2InstancesHealth.put(instanceID, nrFailedChecks + 1);
	    	}
	    }
    }

    class RunHealthCheckTimer extends TimerTask {

        public void run() {
            System.out.println("Running health check...");
            if (!ec2instances.isEmpty()) checkInstances();
        }
    }

    
}
