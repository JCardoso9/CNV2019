package pt.ulisboa.tecnico.cnv.aws.observer;

import pt.ulisboa.tecnico.cnv.aws.autoscaler.EC2InstanceController;

import java.util.*;

public abstract class AbstractManagerObservable extends Observable {
    private final List<Observer> observers = new ArrayList<>(2);

  

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);

        observers.add(o);
    }

    @Override
    public synchronized void deleteObserver(Observer o){
        super.deleteObserver(o);
        observers.remove(o);
    }

    @Override
    public void notifyObservers(Object arg) {
        super.notifyObservers(arg);

        for (Observer observer: observers) {
            observer.update(this, null); // Or pass the instances' list instead of null
        }
    }

}
