package com.angelsystems.simulacionbloquera.model;

public class Bolsa {
    private final int numero;
    private final String cuadrilla;
    private final String etiqueta;
    private Etapa etapa = Etapa.ESPERA;
    private double tiempoEtapa = 0.0;
    private double progreso = 0.0;
    private double espera = 0.0;
    private boolean terminada = false;

    public Bolsa(int numero, String cuadrilla, String etiqueta) {
        this.numero = numero;
        this.cuadrilla = cuadrilla;
        this.etiqueta = etiqueta;
    }

    public int getNumero() { return numero; }
    public String getCuadrilla() { return cuadrilla; }
    public String getEtiqueta() { return etiqueta; }
    public Etapa getEtapa() { return etapa; }
    public void setEtapa(Etapa etapa) { this.etapa = etapa; }
    public double getTiempoEtapa() { return tiempoEtapa; }
    public void setTiempoEtapa(double t) { this.tiempoEtapa = t; }
    public void addTiempoEtapa(double dt) { this.tiempoEtapa += dt; }
    public double getProgreso() { return progreso; }
    public void setProgreso(double p) { this.progreso = p; }
    public double getEspera() { return espera; }
    public void addEspera(double dt) { this.espera += dt; }
    public boolean isTerminada() { return terminada; }
    public void setTerminada(boolean t) { this.terminada = t; }
    public boolean esDonJose() { return "Don Jose".equals(cuadrilla); }
}
