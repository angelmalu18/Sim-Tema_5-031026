package com.angelsystems.simulacionbloquera.model;

public enum Etapa {
    ESPERA(0), MEZCLA(1), TRANSPORTE(2), MOLDEO(3), HIDRATACION(4), TERMINADA(5);

    private final int indice;

    Etapa(int indice) {
        this.indice = indice;
    }

    public int getIndice() {
        return indice;
    }

    public Etapa siguiente() {
        Etapa[] valores = values();
        int siguiente = indice + 1;
        return siguiente < valores.length ? valores[siguiente] : this;
    }
}
