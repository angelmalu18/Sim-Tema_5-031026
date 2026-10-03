package com.angelsystems.simulacionbloquera.util;

public final class TiempoUtil {
    private TiempoUtil() {}

    public static String formatearMinutos(double minutos) {
        long segundos = Math.max(0, Math.round(minutos * 60));
        long horas = segundos / 3600;
        segundos %= 3600;
        long mins = segundos / 60;
        segundos %= 60;
        return String.format("%02d:%02d:%02d", horas, mins, segundos);
    }
}
