package com.angelsystems.simulacionbloquera.model;

public class ConfigTiempos {
    private static final double MEZCLA_CUARTO = 2 + 48.48 / 60.0;
    private static final double TRANSPORTE_CUARTO = 1 + 18.76 / 60.0;

    private double mezcla = MEZCLA_CUARTO * 4;
    private double transporte = TRANSPORTE_CUARTO * 4;
    private double moldeoDj = 36 + 14.87 / 60.0;
    private double moldeoVt = (36 + 14.87 / 60.0) * 2;
    private double hidratacion = 1.0;
    private double secado = 15 * 60.0;
    private double retiro = 60.0;

    public double getMezcla() { return mezcla; }
    public void setMezcla(double v) { mezcla = v; }
    public double getTransporte() { return transporte; }
    public void setTransporte(double v) { transporte = v; }
    public double getMoldeoDj() { return moldeoDj; }
    public void setMoldeoDj(double v) { moldeoDj = v; }
    public double getMoldeoVt() { return moldeoVt; }
    public void setMoldeoVt(double v) { moldeoVt = v; }
    public double getHidratacion() { return hidratacion; }
    public void setHidratacion(double v) { hidratacion = v; }
    public double getSecado() { return secado; }
    public void setSecado(double v) { secado = v; }
    public double getRetiro() { return retiro; }
    public void setRetiro(double v) { retiro = v; }

    public double getMoldeo(String cuadrilla) {
        return "Don Jose".equals(cuadrilla) ? moldeoDj : moldeoVt;
    }
}
