package pt.ulisboa.tecnico.cnv.aws.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public abstract class AbstractInstanceObservable extends Observable {
    private final List<Observer> observers = new ArrayList<>(1);

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);

        observers.add(o);
    }

    @Override
    public void notifyObservers(Object complexityAndState) {
        super.notifyObservers(complexityAndState);

        for (Observer observer: observers) {
            observer.update(this, complexityAndState);
        }
    }
}
