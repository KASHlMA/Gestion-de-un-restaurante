package com.example.integradora;

public class PlanDia {
    private String mesa;
    private String mesero;
    private String horario;

    public PlanDia(String mesa, String mesero, String horario) {
        this.mesa = mesa;
        this.mesero = mesero;
        this.horario = horario;
    }

    public String getMesa() {
        return mesa;
    }

    public void setMesa(String mesa) {
        this.mesa = mesa;
    }

    public String getMesero() {
        return mesero;
    }

    public void setMesero(String mesero) {
        this.mesero = mesero;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }
}
