package com.angelsystems.simulacionbloquera.model;

import com.angelsystems.simulacionbloquera.util.TiempoUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MotorSimulacion {

    public static final int MAXDIAS = 6;
    public static final int BLOQUESPORBOLSA = 50;
    public static final double PASOSIMULACION = 0.25;

    public enum Modo {
        PRODUCCION, PRODUCCIONTERMINADA, SECADO, PROCESO2TERMINADO
    }

    private final Random random = new Random();
    private Escenario escenario = Escenario.ESCENARIO1;
    private final ConfigTiempos config = new ConfigTiempos();
    private Modo modo = Modo.PRODUCCION;
    private int diaActual = 1;
    private boolean[] planVictor;

    private final EstadisticasCuadrilla estDj = new EstadisticasCuadrilla();
    private final EstadisticasCuadrilla estVt = new EstadisticasCuadrilla();
    private int objetivoDj, objetivoVt, bolsasHoyDj, bolsasHoyVt;
    private Bolsa bolsaDj, bolsaVt;
    private final List<Bolsa> bolsasTerminadas = new ArrayList<>();

    private boolean revolvedoraCompartidaOcupada;
    private String quienUsaRevolvedora;
    private boolean revolvedoraDjOcupada, revolvedoraVtOcupada;
    private double tiempoRevolvedoraOcupada;
    private boolean almuerzoMostrado, almuerzoVisible;

    private double tiempoSecadoActual;
    private final List<Bolsa> colaDj = new ArrayList<>();
    private final List<Bolsa> colaVt = new ArrayList<>();
    private int retiradoDj, retiradoVt, almacenBloques;
    private Bolsa loteEnRetiro;
    private double tiempoRetiroActual;
    private String turnoRetiro = "Don Jose";

    public interface Listener {
        void onAlmuerzo();
        void onFinAlmuerzo();
        void onFinDia();
        void onProduccionTerminada();
        void onProceso2Terminado();
        void onBolsaTerminada(Bolsa bolsa);
    }

    private Listener listener;

    public void setListener(Listener listener) { this.listener = listener; }

    public Escenario getEscenario() { return escenario; }
    public void setEscenario(Escenario escenario) { this.escenario = escenario; }
    public ConfigTiempos getConfig() { return config; }
    public Modo getModo() { return modo; }
    public int getDiaActual() { return diaActual; }
    public EstadisticasCuadrilla getEstDj() { return estDj; }
    public EstadisticasCuadrilla getEstVt() { return estVt; }
    public int getObjetivoDj() { return objetivoDj; }
    public int getObjetivoVt() { return objetivoVt; }
    public int getBolsasHoyDj() { return bolsasHoyDj; }
    public int getBolsasHoyVt() { return bolsasHoyVt; }
    public Bolsa getBolsaDj() { return bolsaDj; }
    public Bolsa getBolsaVt() { return bolsaVt; }
    public boolean isRevolvedoraCompartidaOcupada() { return revolvedoraCompartidaOcupada; }
    public String getQuienUsaRevolvedora() { return quienUsaRevolvedora; }
    public boolean isRevolvedoraDjOcupada() { return revolvedoraDjOcupada; }
    public boolean isRevolvedoraVtOcupada() { return revolvedoraVtOcupada; }
    public double getTiempoRevolvedoraOcupada() { return tiempoRevolvedoraOcupada; }
    public boolean isAlmuerzoVisible() { return almuerzoVisible; }
    public double getTiempoSecadoActual() { return tiempoSecadoActual; }
    public int getAlmacenBloques() { return almacenBloques; }
    public Bolsa getLoteEnRetiro() { return loteEnRetiro; }
    public double getTiempoRetiroActual() { return tiempoRetiroActual; }
    public int getRetiradoDj() { return retiradoDj; }
    public int getRetiradoVt() { return retiradoVt; }

    public int getPendientesDj() {
        return colaDj.size() + (loteEnRetiro != null && loteEnRetiro.esDonJose() ? 1 : 0);
    }

    public int getPendientesVt() {
        return colaVt.size() + (loteEnRetiro != null && !loteEnRetiro.esDonJose() ? 1 : 0);
    }

    public void reiniciar() {
        modo = Modo.PRODUCCION;
        diaActual = 1;
        planVictor = new boolean[MAXDIAS];
        int diasVictor = random.nextBoolean() ? 4 : 5;
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < MAXDIAS; i++) indices.add(i);
        Collections.shuffle(indices, random);
        for (int i = 0; i < diasVictor; i++) planVictor[indices.get(i)] = true;
        estDj.reset();
        estVt.reset();
        bolsasTerminadas.clear();
        revolvedoraCompartidaOcupada = false;
        quienUsaRevolvedora = null;
        revolvedoraDjOcupada = false;
        revolvedoraVtOcupada = false;
        tiempoRevolvedoraOcupada = 0;
        almacenBloques = 0;
        tiempoSecadoActual = 0;
        colaDj.clear();
        colaVt.clear();
        retiradoDj = 0;
        retiradoVt = 0;
        loteEnRetiro = null;
        tiempoRetiroActual = 0;
        turnoRetiro = "Don Jose";
        prepararDia();
    }

    public void prepararDia() {
        objetivoDj = 6 + random.nextInt(3);
        objetivoVt = planVictor[diaActual - 1] ? (3 + random.nextInt(2)) : 0;
        bolsasHoyDj = 0;
        bolsasHoyVt = 0;
        bolsaDj = null;
        bolsaVt = null;
        almuerzoMostrado = false;
        almuerzoVisible = false;
    }

    public void crearBolsaSiCorresponde(String cuadrilla) {
        boolean esDj = "Don Jose".equals(cuadrilla);
        Bolsa actual = esDj ? bolsaDj : bolsaVt;
        int objetivo = esDj ? objetivoDj : objetivoVt;
        int hechas = esDj ? bolsasHoyDj : bolsasHoyVt;
        if (actual != null || almuerzoVisible || hechas >= objetivo || objetivo == 0) return;
        int numero = hechas + 1;
        String etiqueta = esDj ? "B" + numero : "B'" + numero;
        Bolsa bolsa = new Bolsa(numero, cuadrilla, etiqueta);
        // Escenario 2: sin espera — toma su revolvedora y entra directo a mezcla
        if (escenario.tieneDosRevolvedoras()) {
            if (puedeTomarRevolvedora(bolsa)) {
                tomarRevolvedora(bolsa);
                bolsa.setEtapa(Etapa.MEZCLA);
                bolsa.setTiempoEtapa(0);
                bolsa.setProgreso(0);
            }
        }
        if (esDj) bolsaDj = bolsa; else bolsaVt = bolsa;
    }

    public boolean procesarBolsa(Bolsa bolsa, double dt) {
        if (bolsa == null || bolsa.isTerminada()) return false;
        if (bolsa.getEtapa() == Etapa.ESPERA) {
            if (puedeTomarRevolvedora(bolsa)) {
                tomarRevolvedora(bolsa);
                bolsa.setEtapa(Etapa.MEZCLA);
                bolsa.setTiempoEtapa(0);
                bolsa.setProgreso(0);
            } else {
                bolsa.addEspera(dt);
                estadistica(bolsa).addTiempoEspera(dt);
            }
            return false;
        }
        double necesario = tiempoDeEtapa(bolsa);
        bolsa.addTiempoEtapa(dt);
        bolsa.setProgreso(Math.min(100, bolsa.getTiempoEtapa() / necesario * 100));
        estadistica(bolsa).addTiempoLaboral(dt);
        if (bolsa.getEtapa() == Etapa.MEZCLA) tiempoRevolvedoraOcupada += dt;
        if (bolsa.getTiempoEtapa() < necesario) return false;
        if (bolsa.getEtapa() == Etapa.MEZCLA) liberarRevolvedora(bolsa);
        if (bolsa.getEtapa() == Etapa.HIDRATACION) {
            terminarBolsa(bolsa);
            return true;
        }
        bolsa.setEtapa(bolsa.getEtapa().siguiente());
        bolsa.setTiempoEtapa(0);
        bolsa.setProgreso(0);
        return false;
    }

    private boolean puedeTomarRevolvedora(Bolsa b) {
        if (escenario.tieneDosRevolvedoras()) {
            return b.esDonJose() ? !revolvedoraDjOcupada : !revolvedoraVtOcupada;
        }
        return !revolvedoraCompartidaOcupada;
    }

    private void tomarRevolvedora(Bolsa b) {
        if (escenario.tieneDosRevolvedoras()) {
            if (b.esDonJose()) revolvedoraDjOcupada = true; else revolvedoraVtOcupada = true;
        } else {
            revolvedoraCompartidaOcupada = true;
            quienUsaRevolvedora = b.getCuadrilla();
        }
    }

    private void liberarRevolvedora(Bolsa b) {
        if (escenario.tieneDosRevolvedoras()) {
            if (b.esDonJose()) revolvedoraDjOcupada = false; else revolvedoraVtOcupada = false;
        } else {
            revolvedoraCompartidaOcupada = false;
            quienUsaRevolvedora = null;
        }
    }

    private double tiempoDeEtapa(Bolsa b) {
        return switch (b.getEtapa()) {
            case MEZCLA -> config.getMezcla();
            case TRANSPORTE -> config.getTransporte();
            case MOLDEO -> config.getMoldeo(b.getCuadrilla());
            case HIDRATACION -> config.getHidratacion();
            default -> 1.0;
        };
    }

    private EstadisticasCuadrilla estadistica(Bolsa b) {
        return b.esDonJose() ? estDj : estVt;
    }

    private void terminarBolsa(Bolsa bolsa) {
        bolsa.setProgreso(100);
        bolsa.setTerminada(true);
        bolsasTerminadas.add(bolsa);
        estadistica(bolsa).agregarBolsa(BLOQUESPORBOLSA);
        if (listener != null) listener.onBolsaTerminada(bolsa);
        if (bolsa.esDonJose()) {
            bolsasHoyDj++;
            bolsaDj = null;
            if (bolsa.getNumero() == 3 && !almuerzoMostrado) mostrarAlmuerzo();
            else crearBolsaSiCorresponde("Don Jose");
        } else {
            bolsasHoyVt++;
            bolsaVt = null;
            crearBolsaSiCorresponde("Victor");
        }
    }

    private void mostrarAlmuerzo() {
        almuerzoMostrado = true;
        almuerzoVisible = true;
        if (listener != null) listener.onAlmuerzo();
    }

    public void quitarAlmuerzo() {
        almuerzoVisible = false;
        crearBolsaSiCorresponde("Don Jose");
        crearBolsaSiCorresponde("Victor");
        if (listener != null) listener.onFinAlmuerzo();
    }

    public void tickProduccion(double dt) {
        if (modo != Modo.PRODUCCION || almuerzoVisible) return;
        crearBolsaSiCorresponde("Don Jose");
        crearBolsaSiCorresponde("Victor");
        if (bolsaDj != null) procesarBolsa(bolsaDj, dt);
        if (bolsaVt != null) procesarBolsa(bolsaVt, dt);
        revisarFinDia();
    }

    private void revisarFinDia() {
        boolean djFin = bolsasHoyDj >= objetivoDj && bolsaDj == null;
        boolean vtFin = objetivoVt == 0 || (bolsasHoyVt >= objetivoVt && bolsaVt == null);
        if (!(djFin && vtFin)) return;
        estDj.incrementarDias();
        if (objetivoVt > 0) estVt.incrementarDias();
        if (diaActual == MAXDIAS) {
            modo = Modo.PRODUCCIONTERMINADA;
            if (listener != null) listener.onProduccionTerminada();
            return;
        }
        diaActual++;
        prepararDia();
        if (listener != null) listener.onFinDia();
    }

    public boolean iniciarProceso2() {
        if (modo != Modo.PRODUCCIONTERMINADA) return false;
        modo = Modo.SECADO;
        tiempoSecadoActual = 0;
        colaDj.clear();
        colaVt.clear();
        for (Bolsa b : bolsasTerminadas) {
            if (b.esDonJose()) colaDj.add(b); else colaVt.add(b);
        }
        retiradoDj = 0;
        retiradoVt = 0;
        almacenBloques = 0;
        loteEnRetiro = null;
        tiempoRetiroActual = 0;
        turnoRetiro = "Don Jose";
        return true;
    }

    public void tickProceso2(double dt) {
        if (modo != Modo.SECADO) return;
        if (tiempoSecadoActual < config.getSecado()) {
            tiempoSecadoActual = Math.min(config.getSecado(), tiempoSecadoActual + dt);
        } else {
            avanzarRetiro(dt);
        }
    }

    private void avanzarRetiro(double dt) {
        if (loteEnRetiro == null) {
            loteEnRetiro = siguienteLote();
            tiempoRetiroActual = 0;
            if (loteEnRetiro == null) {
                modo = Modo.PROCESO2TERMINADO;
                if (listener != null) listener.onProceso2Terminado();
                return;
            }
        }
        tiempoRetiroActual += dt;
        if (tiempoRetiroActual >= config.getRetiro()) {
            if (loteEnRetiro.esDonJose()) retiradoDj++; else retiradoVt++;
            almacenBloques += BLOQUESPORBOLSA;
            loteEnRetiro = null;
        }
    }

    private Bolsa siguienteLote() {
        List<Bolsa> preferida = "Don Jose".equals(turnoRetiro) ? colaDj : colaVt;
        List<Bolsa> alternativa = "Don Jose".equals(turnoRetiro) ? colaVt : colaDj;
        Bolsa bolsa = null;
        if (!preferida.isEmpty()) bolsa = preferida.remove(0);
        else if (!alternativa.isEmpty()) bolsa = alternativa.remove(0);
        if (bolsa == null) return null;
        turnoRetiro = bolsa.esDonJose() ? "Victor" : "Don Jose";
        return bolsa;
    }

    public String textoResultados() {
        int totalBolsas = estDj.getBolsas() + estVt.getBolsas();
        int totalBloques = estDj.getBloques() + estVt.getBloques();
        return "RESULTADOS · " + escenario.getDescripcion() + "\n\n"
                + String.format("Don Jose: %d bolsas · %d bloques%n", estDj.getBolsas(), estDj.getBloques())
                + String.format("  Tiempo laboral: %s%n", TiempoUtil.formatearMinutos(estDj.getTiempoLaboral()))
                + String.format("  Espera revolvedora: %s%n", TiempoUtil.formatearMinutos(estDj.getTiempoEspera()))
                + String.format("  Utilizacion: %.1f%%%n%n", estDj.getUtilizacion())
                + String.format("Victor: %d bolsas · %d bloques%n", estVt.getBolsas(), estVt.getBloques())
                + String.format("  Tiempo laboral: %s%n", TiempoUtil.formatearMinutos(estVt.getTiempoLaboral()))
                + String.format("  Espera revolvedora: %s%n", TiempoUtil.formatearMinutos(estVt.getTiempoEspera()))
                + String.format("  Utilizacion: %.1f%%%n%n", estVt.getUtilizacion())
                + String.format("TOTAL: %d bolsas · %d bloques%n", totalBolsas, totalBloques)
                + String.format("Almacen: %d bloques%n", almacenBloques)
                + String.format("Revolvedora(s) ocupada(s): %s%n",
                TiempoUtil.formatearMinutos(tiempoRevolvedoraOcupada));
    }
}
