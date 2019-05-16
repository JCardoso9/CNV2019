
package pt.ulisboa.tecnico.cnv.aws.autoscaler;

import java.util.TimerTask;

public class ShutdownTimer extends TimerTask {

    String instanceID;

    public ShutdownTimer(String instanceID){
        this.instanceID = instanceID;
    }

    public void run(){
        if( EC2AutoScaler.getInstance().isInstanceIdle(instanceID)){
            EC2AutoScaler.getInstance().scaleDown(instanceID);
        }
    }
    
}