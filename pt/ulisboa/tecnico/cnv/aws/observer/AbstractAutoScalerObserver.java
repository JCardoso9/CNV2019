package pt.ulisboa.tecnico.cnv.aws.observer;

import java.util.Observable;
import java.util.Observer;

public abstract class AbstractAutoScalerObserver implements Observer {

    public AbstractAutoScalerObserver(Observable manager) {
        if (manager == null) { return; }

        manager.addObserver(this);
    }

    @Override
    public void update(Observable instancesManager, Object o) {
        // An instance has changed

        this.executeAutoScalerLogic();
    }

    public abstract void executeAutoScalerLogic();
}
