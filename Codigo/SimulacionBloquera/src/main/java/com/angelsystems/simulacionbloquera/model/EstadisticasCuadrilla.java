package com.angelsystems.simulacionbloquera.model;

public class EstadisticasCuadrilla {
    private int bolsas = 0;
    private int bloques = 0;
    private int diasTrabajados = 0;
    private double tiempoLaboral = 0.0;
    private double tiempoEspera = 0.0;

    public void agregarBolsa(int bloquesPorBolsa) {
        bolsas++;
        bloques += bloquesPorBolsa;
    }

    public void addTiempoLaboral(double dt) { tiempoLaboral += dt; }
    public void addTiempoEspera(double dt) { tiempoEspera += dt; }
    public void incrementarDias() { diasTrabajados++; }

    public double getUtilizacion() {
        double total = tiempoLaboral + tiempoEspera;
        return total == 0 ? 0 : tiempoLaboral / total * 100.0;
    }

    public int getBolsas() { return bolsas; }
    public int getBloques() { return bloques; }
    public int getDiasTrabajados() { return diasTrabajados; }
    public double getTiempoLaboral() { return tiempoLaboral; }
    public double getTiempoEspera() { return tiempoEspera; }

    public void reset() {
        bolsas = 0;
        bloques = 0;
        diasTrabajados = 0;
        tiempoLaboral = 0;
        tiempoEspera = 0;
    }
}
