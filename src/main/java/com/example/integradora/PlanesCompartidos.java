package com.example.integradora;

import com.example.integradora.PlanesCompartidos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PlanesCompartidos {

    private static PlanesCompartidos instancia;
    private final ObservableList<PlanDia> planes;

    private PlanesCompartidos() {
        planes = FXCollections.observableArrayList();
    }

    public static PlanesCompartidos getInstancia() {
        if (instancia == null) {
            instancia = new PlanesCompartidos();
        }
        return instancia;
    }

    public ObservableList<PlanDia> getPlanes() {
        return planes;
    }

    public void agregarPlan(PlanDia plan) {
        planes.add(plan);
    }

    public void limpiarPlanes() {
        planes.clear();
    }
}
