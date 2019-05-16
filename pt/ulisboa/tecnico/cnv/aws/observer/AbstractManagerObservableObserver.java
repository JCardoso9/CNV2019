package pt.ulisboa.tecnico.cnv.aws.observer;

import pt.ulisboa.tecnico.cnv.aws.autoscaler.EC2InstanceController;

import java.util.*;

public abstract class AbstractManagerObservableObserver extends Observable implements Observer {
    private final List<Observer> observers = new ArrayList<>(2);

    public AbstractManagerObservableObserver(List<Observable> instances) {
        if (instances == null) { return; }

        for (Observable instance : instances) {
            instance.addObserver(this);
        }
    }

    @Override
    public void update(Observable instance, Object o) {
        // An instance has changed
        String[] complexityAndState = (String[])o;

        this.updateInstancesList((EC2InstanceController) instance, complexityAndState);

        this.notifyObservers(null);
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);

        observers.add(o);
    }

    @Override
    public void notifyObservers(Object arg) {
        super.notifyObservers(arg);

        for (Observer observer: observers) {
            observer.update(this, null); // Or pass the instances' list instead of null
        }
    }

    public abstract void updateInstancesList(EC2InstanceController instance, String[] complexityAndState);
}
