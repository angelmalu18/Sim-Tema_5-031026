package com.angelsystems.simulacionbloquera.model;

public enum Escenario {
    ESCENARIO1("Escenario 1 · Situacion actual (1 revolvedora)"),
    ESCENARIO2("Escenario 2 · Mejora (2 revolvedoras)");

    private final String descripcion;

    Escenario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean tieneDosRevolvedoras() {
        return this == ESCENARIO2;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
