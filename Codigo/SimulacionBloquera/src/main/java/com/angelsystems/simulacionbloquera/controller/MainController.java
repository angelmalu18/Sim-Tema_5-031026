package com.angelsystems.simulacionbloquera.controller;

import com.angelsystems.simulacionbloquera.model.*;
import com.angelsystems.simulacionbloquera.ui.Colores;
import com.angelsystems.simulacionbloquera.util.TiempoUtil;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class MainController {

    private static final double[] POSICIONES_X = {145, 380, 620, 900, 1130, 1280};
    private static final long ALMUERZO_MS = 3000;

    @FXML private Label lblEstado;
    @FXML private Label lblRelojDj;
    @FXML private Label lblRelojVt;
    @FXML private Label lblDia;
    @FXML private Label lblRev;
    @FXML private Label lblAlmacen;
    @FXML private ComboBox<Escenario> comboEscenario;
    @FXML private ComboBox<String> comboVelocidad;
    @FXML private Canvas canvas;

    private MotorSimulacion motor;
    private GraphicsContext gc;
    private boolean corriendo = false;
    private double velocidad = 1.0;
    private long ultimoNanos = 0;
    private long almuerzoInicioMs = 0;
    private AnimationTimer timer;
    private Stage stage;

    @FXML
    public void initialize() {
        motor = new MotorSimulacion();
        motor.setListener(new MotorSimulacion.Listener() {
            @Override public void onAlmuerzo() { almuerzoInicioMs = System.currentTimeMillis(); }
            @Override public void onFinAlmuerzo() {}
            @Override public void onFinDia() {}
            @Override public void onProduccionTerminada() {
                corriendo = false;
                mostrarInfo("Producción terminada",
                        "Se completaron los seis días.\nYa puede abrir el Proceso 2.");
            }
            @Override public void onProceso2Terminado() {
                corriendo = false;
                mostrarInfo("Proceso 2 terminado",
                        "Bloques almacenados: " + motor.getAlmacenBloques());
            }
            @Override public void onBolsaTerminada(Bolsa bolsa) {}
        });

        gc = canvas.getGraphicsContext2D();

        comboEscenario.getItems().setAll(Escenario.values());
        comboEscenario.setValue(Escenario.ESCENARIO1);

        comboVelocidad.getItems().setAll("Lenta", "Normal", "Rápida", "Muy rápida");
        comboVelocidad.setValue("Normal");

        motor.reiniciar();
        dibujar();
        actualizarIndicadores();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!corriendo) {
                    ultimoNanos = 0;
                    return;
                }
                if (ultimoNanos == 0) {
                    ultimoNanos = now;
                    return;
                }
                double elapsedSec = (now - ultimoNanos) / 1_000_000_000.0;
                ultimoNanos = now;
                double dt = MotorSimulacion.PASOSIMULACION * velocidad * (elapsedSec / 0.04);

                if (motor.isAlmuerzoVisible()) {
                    if (System.currentTimeMillis() - almuerzoInicioMs >= ALMUERZO_MS) {
                        motor.quitarAlmuerzo();
                    }
                } else if (motor.getModo() == MotorSimulacion.Modo.PRODUCCION) {
                    motor.tickProduccion(dt);
                } else if (motor.getModo() == MotorSimulacion.Modo.SECADO) {
                    motor.tickProceso2(dt);
                }
                dibujar();
                actualizarIndicadores();
            }
        };
        timer.start();
    }

    @FXML
    private void onIniciar() {
        if (motor.getModo() != MotorSimulacion.Modo.PRODUCCION) {
            mostrarInfo("Producción", "La producción ya terminó. Ejecute el Proceso 2 o reinicie.");
            return;
        }
        corriendo = true;
        ultimoNanos = 0;
    }

    @FXML
    private void onPausar() {
        corriendo = false;
        actualizarIndicadores();
    }

    @FXML
    private void onReiniciar() {
        corriendo = false;
        motor.setEscenario(comboEscenario.getValue());
        motor.reiniciar();
        dibujar();
        actualizarIndicadores();
        mostrarInfo("Reinicio", "Se creó un nuevo escenario de seis días.");
    }

    @FXML
    private void onProceso2() {
        if (!motor.iniciarProceso2()) {
            mostrarInfo("Proceso 2", "Primero complete los seis días de producción.");
            return;
        }
        corriendo = true;
        ultimoNanos = 0;
        dibujar();
    }

    @FXML
    private void onResultados() {
        Stage dialog = new Stage();
        if (stage == null && canvas.getScene() != null) {
            stage = (Stage) canvas.getScene().getWindow();
        }
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Resultados - " + motor.getEscenario().getDescripcion());
        TextArea area = new TextArea(motor.textoResultados());
        area.setEditable(false);
        area.setFont(Font.font("Consolas", 12));
        area.setPrefSize(620, 480);
        area.setStyle("-fx-text-fill: #1A237E;");
        dialog.setScene(new Scene(new StackPane(area), 640, 500));
        dialog.show();
    }

    @FXML
    private void onGuardarCsv() {
        if (stage == null && canvas.getScene() != null) {
            stage = (Stage) canvas.getScene().getWindow();
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar resultados");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("resultados_bloquera.csv");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Indicador,Valor");
            pw.println("Fecha," + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.println("Escenario," + motor.getEscenario().getDescripcion());
            pw.println("Bolsas Don Jose," + motor.getEstDj().getBolsas());
            pw.println("Bolsas Victor," + motor.getEstVt().getBolsas());
            pw.println("Bloques Don Jose," + motor.getEstDj().getBloques());
            pw.println("Bloques Victor," + motor.getEstVt().getBloques());
            pw.println("Tiempo laboral Don Jose," + TiempoUtil.formatearMinutos(motor.getEstDj().getTiempoLaboral()));
            pw.println("Tiempo laboral Victor," + TiempoUtil.formatearMinutos(motor.getEstVt().getTiempoLaboral()));
            pw.println("Espera Don Jose (min)," + String.format("%.2f", motor.getEstDj().getTiempoEspera()));
            pw.println("Espera Victor (min)," + String.format("%.2f", motor.getEstVt().getTiempoEspera()));
            pw.println("Utilización Don Jose (%)," + String.format("%.1f", motor.getEstDj().getUtilizacion()));
            pw.println("Utilización Victor (%)," + String.format("%.1f", motor.getEstVt().getUtilizacion()));
            pw.println("Revolvedora ocupada (min)," + String.format("%.2f", motor.getTiempoRevolvedoraOcupada()));
            pw.println("Almacén (bloques)," + motor.getAlmacenBloques());
            mostrarInfo("CSV", "Resultados guardados en:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo guardar:\n" + ex.getMessage());
        }
    }

    @FXML
    private void onTiempos() {
        if (stage == null && canvas.getScene() != null) {
            stage = (Stage) canvas.getScene().getWindow();
        }
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Tiempos del modelo");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20));

        ConfigTiempos cfg = motor.getConfig();
        String[] labels = {
                "Preparación de mezcla por bolsa", "Carretilla por bolsa",
                "Moldeo Don Jose", "Moldeo Victor", "Hidratación", "Secado", "Retiro por lote"
        };
        double[] valores = {
                cfg.getMezcla(), cfg.getTransporte(), cfg.getMoldeoDj(), cfg.getMoldeoVt(),
                cfg.getHidratacion(), cfg.getSecado(), cfg.getRetiro()
        };
        TextField[] fields = new TextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            fields[i] = new TextField(String.format("%.4f", valores[i]));
            fields[i].setPrefWidth(100);
            grid.add(fields[i], 1, i);
            grid.add(new Label("minutos"), 2, i);
        }

        Button guardar = new Button("Guardar tiempos");
        guardar.setOnAction(e -> {
            try {
                double m = Double.parseDouble(fields[0].getText());
                double t = Double.parseDouble(fields[1].getText());
                double mdj = Double.parseDouble(fields[2].getText());
                double mvt = Double.parseDouble(fields[3].getText());
                double h = Double.parseDouble(fields[4].getText());
                double s = Double.parseDouble(fields[5].getText());
                double r = Double.parseDouble(fields[6].getText());
                if (m <= 0 || t <= 0 || mdj <= 0 || mvt <= 0 || h <= 0 || s <= 0 || r <= 0) {
                    throw new NumberFormatException();
                }
                cfg.setMezcla(m);
                cfg.setTransporte(t);
                cfg.setMoldeoDj(mdj);
                cfg.setMoldeoVt(mvt);
                cfg.setHidratacion(h);
                cfg.setSecado(s);
                cfg.setRetiro(r);
                dialog.close();
                dibujar();
                mostrarInfo("Tiempos", "Los tiempos se actualizaron.");
            } catch (NumberFormatException ex) {
                mostrarError("Dato inválido", "Escriba valores numéricos mayores que cero.");
            }
        });
        grid.add(guardar, 0, labels.length, 3, 1);
        dialog.setScene(new Scene(grid, 520, 380));
        dialog.show();
    }

    @FXML
    private void onCambioEscenario() {
        if (motor.getModo() == MotorSimulacion.Modo.PRODUCCION
                && motor.getDiaActual() == 1
                && motor.getBolsasHoyDj() == 0) {
            motor.setEscenario(comboEscenario.getValue());
            dibujar();
        } else {
            mostrarInfo("Escenario", "Reinicie la simulación para cambiar de escenario.");
            comboEscenario.setValue(motor.getEscenario());
        }
    }

    @FXML
    private void onCambioVelocidad() {
        Map<String, Double> map = Map.of(
                "Lenta", 0.35, "Normal", 1.0, "Rápida", 3.5, "Muy rápida", 9.0);
        velocidad = map.getOrDefault(comboVelocidad.getValue(), 1.0);
    }

    // ===================== DIBUJO =====================

    private void dibujar() {
        if (motor.getModo() == MotorSimulacion.Modo.SECADO
                || motor.getModo() == MotorSimulacion.Modo.PROCESO2TERMINADO) {
            dibujarProceso2();
        } else {
            dibujarProduccion();
        }
    }

    private void dibujarProduccion() {
        gc.setFill(Colores.PANEL);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Colores.AZUL);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PROCESO 1 · PRODUCCION — " + motor.getEscenario().getDescripcion(), 710, 22);

        gc.setFill(Colores.GRIS);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("La animacion es acelerada; los valores sobre cada estacion son tiempos reales.", 710, 42);

        boolean dosRev = motor.getEscenario().tieneDosRevolvedoras();

        dibujarLinea(175, "CUADRILLA DON JOSE", Colores.DONJOSE, Color.web("#FFF8E1"),
                motor.getObjetivoDj(), dosRev);
        dibujarLinea(430, "CUADRILLA VICTOR", Colores.VICTOR, Color.web("#F3E5F5"),
                motor.getObjetivoVt(), dosRev);

        if (dosRev) {
            // Revolvedoras circulares al INICIO de cada linea (posicion mezcla)
            dibujarRevolvedoraCirculo(380, 175, "REV. DJ", motor.isRevolvedoraDjOcupada());
            dibujarRevolvedoraCirculo(380, 430, "REV. VT", motor.isRevolvedoraVtOcupada());
        } else {
            gc.setFill(Color.web("#FFF3E0"));
            gc.setStroke(Colores.NARANJA);
            gc.setLineWidth(3);
            gc.fillRoundRect(450, 280, 210, 80, 8, 8);
            gc.strokeRoundRect(450, 280, 210, 80, 8, 8);
            gc.setFill(Colores.NARANJA);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            String estado = motor.isRevolvedoraCompartidaOcupada()
                    ? "OCUPADA\n" + motor.getQuienUsaRevolvedora()
                    : "LIBRE";
            gc.fillText("REVOLVEDORA COMPARTIDA\n1 CUADRILLA A LA VEZ\n" + estado, 555, 305);
        }

        dibujarBolsa(motor.getBolsaDj(), 175, dosRev);
        dibujarBolsa(motor.getBolsaVt(), 430, dosRev);

        if (motor.isAlmuerzoVisible()) {
            gc.setFill(Color.web("#FFECB3"));
            gc.setStroke(Colores.NARANJA);
            gc.setLineWidth(4);
            gc.fillRoundRect(480, 270, 460, 85, 10, 10);
            gc.strokeRoundRect(480, 270, 460, 85, 10, 10);
            gc.setFill(Colores.NARANJA);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            gc.fillText("PAUSA DE ALMUERZO\nAmbas cuadrillas descansan", 710, 305);
        }

        gc.setFill(Colores.GRIS);
        gc.setFont(Font.font("Arial", 8));
        gc.fillText(dosRev
                ? "Escenario 2: cada cuadrilla tiene su revolvedora al inicio. Sin espera."
                : "Tras la B3 se muestra la pausa de almuerzo.", 710, 640);
    }

    private void dibujarRevolvedora(double x, double y, String titulo, boolean ocupada) {
        dibujarRevolvedoraCirculo(x, y, titulo, ocupada);
    }

    private void dibujarRevolvedoraCirculo(double cx, double cy, String titulo, boolean ocupada) {
        double r = 42;
        gc.setFill(ocupada ? Color.web("#FFEBEE") : Color.web("#E8F5E9"));
        gc.setStroke(ocupada ? Colores.ROJO : Colores.VERDE);
        gc.setLineWidth(3);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        gc.setFill(ocupada ? Colores.ROJO : Colores.VERDE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(titulo + "\n" + (ocupada ? "OCUPADA" : "LIBRE"), cx, cy - 4);
    }

    private void dibujarLinea(double y, String titulo, Color color, Color fondo, int objetivo, boolean sinEspera) {
        gc.setFill(fondo);
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.fillRoundRect(25, y - 112, 1370, 224, 6, 6);
        gc.strokeRoundRect(25, y - 112, 1370, 224, 6, 6);

        gc.setFill(color);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("%s · Dia %d / %d · Objetivo: %d bolsas",
                titulo, motor.getDiaActual(), MotorSimulacion.MAXDIAS, objetivo), 40, y - 90);

        ConfigTiempos cfg = motor.getConfig();
        boolean esDj = titulo.contains("JOSE");

        // Escenario 2: sin estacion "Espera" — empieza en Mezcla
        double[] xs;
        String[] nombres;
        String[] tiempos;
        if (sinEspera) {
            xs = new double[]{380, 620, 900, 1130, 1280};
            nombres = new String[]{"Mezcla", "Carretilla", "Moldeo", "Hidratacion", "Terminada"};
            tiempos = new String[]{
                    TiempoUtil.formatearMinutos(cfg.getMezcla()),
                    TiempoUtil.formatearMinutos(cfg.getTransporte()),
                    TiempoUtil.formatearMinutos(esDj ? cfg.getMoldeoDj() : cfg.getMoldeoVt()),
                    TiempoUtil.formatearMinutos(cfg.getHidratacion()),
                    "Bolsa completa"
            };
        } else {
            xs = POSICIONES_X;
            nombres = new String[]{"Espera\nrevolvedora", "Mezcla", "Carretilla", "Moldeo", "Hidratacion", "Terminada"};
            tiempos = new String[]{
                    "No suma reloj",
                    TiempoUtil.formatearMinutos(cfg.getMezcla()),
                    TiempoUtil.formatearMinutos(cfg.getTransporte()),
                    TiempoUtil.formatearMinutos(esDj ? cfg.getMoldeoDj() : cfg.getMoldeoVt()),
                    TiempoUtil.formatearMinutos(cfg.getHidratacion()),
                    "Bolsa completa"
            };
        }

        gc.setStroke(color);
        gc.setLineWidth(5);
        gc.strokeLine(xs[0], y, xs[xs.length - 1], y);

        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < xs.length; i++) {
            double x = xs[i];
            gc.setFill(color);
            gc.fillOval(x - 8, y - 8, 16, 16);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            gc.fillText(tiempos[i], x, y - 42);
            gc.setFill(Colores.AZUL);
            gc.setFont(Font.font("Arial", 8));
            String[] lineas = nombres[i].split("\n");
            for (int j = 0; j < lineas.length; j++) {
                gc.fillText(lineas[j], x, y + 28 + j * 12);
            }
        }
    }

    private void dibujarBolsa(Bolsa bolsa, double y, boolean sinEspera) {
        if (bolsa == null) return;

        double[] xs;
        if (sinEspera) {
            // Mezcla, Transporte, Moldeo, Hidratacion, Terminada
            xs = new double[]{380, 620, 900, 1130, 1280};
        } else {
            xs = POSICIONES_X;
        }

        double x;
        if (bolsa.isTerminada()) {
            x = xs[xs.length - 1];
        } else {
            int idx;
            if (sinEspera) {
                // Etapa.ESPERA no se usa visualmente; MEZCLA=0 en xs
                idx = switch (bolsa.getEtapa()) {
                    case ESPERA, MEZCLA -> 0;
                    case TRANSPORTE -> 1;
                    case MOLDEO -> 2;
                    case HIDRATACION -> 3;
                    default -> 4;
                };
            } else {
                idx = bolsa.getEtapa().getIndice();
            }
            x = xs[Math.min(idx, xs.length - 1)];
            if (idx < xs.length - 1) {
                x += (xs[idx + 1] - x) * bolsa.getProgreso() / 100.0;
            }
        }

        Color color = switch (bolsa.getEtapa()) {
            case ESPERA -> bolsa.esDonJose() ? Colores.DONJOSE : Colores.VICTOR;
            case MEZCLA -> Colores.NARANJA;
            case TRANSPORTE -> Colores.CELESTE;
            case MOLDEO -> Colores.VERDE;
            case HIDRATACION -> Colores.AMARILLO;
            default -> Colores.VERDE;
        };
        if (bolsa.isTerminada()) color = Colores.VERDE;

        gc.setFill(color);
        gc.fillRoundRect(x - 20, y - 12, 40, 24, 4, 4);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(bolsa.getEtiqueta(), x, y + 4);
    }

    private void dibujarProceso2() {
        gc.setFill(Colores.PANEL);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Colores.AZUL);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PROCESO 2 · SECADO, RETIRO Y ALMACÉN", 710, 24);

        String estado;
        if (motor.getTiempoSecadoActual() < motor.getConfig().getSecado()) {
            double pct = motor.getTiempoSecadoActual() / motor.getConfig().getSecado() * 100;
            estado = String.format("SECANDO · %.1f%% · %s / %s",
                    pct,
                    TiempoUtil.formatearMinutos(motor.getTiempoSecadoActual()),
                    TiempoUtil.formatearMinutos(motor.getConfig().getSecado()));
        } else {
            estado = "SECADO TERMINADO · PERSONAL RETIRANDO BLOQUES";
        }
        gc.setFill(Colores.CELESTE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.fillText(estado, 710, 50);

        dibujarEra(105, 115, "ERA DON JOSE", Colores.DONJOSE,
                motor.getEstDj().getBolsas(), motor.getPendientesDj(), motor.getRetiradoDj());
        dibujarEra(705, 115, "ERA VÍCTOR", Colores.VICTOR,
                motor.getEstVt().getBolsas(), motor.getPendientesVt(), motor.getRetiradoVt());

        gc.setFill(Color.web("#FFF3E0"));
        gc.setStroke(Colores.NARANJA);
        gc.setLineWidth(3);
        gc.fillRoundRect(505, 405, 410, 100, 8, 8);
        gc.strokeRoundRect(505, 405, 410, 100, 8, 8);
        gc.setFill(Colores.NARANJA);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText("PERSONAL DE RETIRO", 710, 435);
        gc.setFill(Colores.GRIS);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("Retira un lote de 50 bloques de cada era por turnos", 710, 460);

        Bolsa lote = motor.getLoteEnRetiro();
        if (lote != null) {
            double origen = lote.esDonJose() ? 360 : 1060;
            double progreso = Math.min(1, motor.getTiempoRetiroActual() / motor.getConfig().getRetiro());
            double x = origen + (710 - origen) * progreso;
            gc.setFill(Color.web("#8D6E63"));
            gc.fillRoundRect(x - 30, 520, 60, 35, 4, 4);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText(lote.getEtiqueta(), x, 542);
            gc.setFill(Colores.NARANJA);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.fillText(String.format("Retirando %s: %.1f%% · %s / %s",
                    lote.getEtiqueta(), progreso * 100,
                    TiempoUtil.formatearMinutos(motor.getTiempoRetiroActual()),
                    TiempoUtil.formatearMinutos(motor.getConfig().getRetiro())), 710, 580);
        } else {
            gc.setFill(Colores.GRIS);
            gc.setFont(Font.font("Arial", 9));
            gc.fillText("Esperando lote seco para retirar", 710, 580);
        }

        gc.setFill(Color.web("#E8F5E9"));
        gc.setStroke(Colores.VERDE);
        gc.setLineWidth(3);
        gc.fillRoundRect(1000, 405, 350, 200, 8, 8);
        gc.strokeRoundRect(1000, 405, 350, 200, 8, 8);
        gc.setFill(Colores.VERDE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("ALMACÉN", 1175, 435);
        gc.setFill(Colores.AZUL);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText(motor.getAlmacenBloques() + " bloques", 1175, 470);

        int lotesVis = Math.min(24, motor.getAlmacenBloques() / MotorSimulacion.BLOQUESPORBOLSA);
        for (int i = 0; i < lotesVis; i++) {
            double bx = 1040 + (i % 8) * 33;
            double by = 515 + (i / 8) * 25;
            gc.setFill(Color.web("#78909C"));
            gc.setStroke(Color.web("#455A64"));
            gc.setLineWidth(1);
            gc.fillRect(bx, by, 25, 16);
            gc.strokeRect(bx, by, 25, 16);
        }
    }

    private void dibujarEra(double x, double y, String titulo, Color color,
                            int total, int pendientes, int retirados) {
        gc.setFill(Color.web("#F8F9FA"));
        gc.setStroke(color);
        gc.setLineWidth(3);
        gc.fillRoundRect(x, y, 500, 230, 6, 6);
        gc.strokeRoundRect(x, y, 500, 230, 6, 6);
        gc.setFill(color);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(titulo, x + 250, y + 28);
        gc.setFill(Colores.GRIS);
        gc.setFont(Font.font("Arial", 9));
        gc.fillText(String.format("Producidas: %d bolsas · En era: %d · Retiradas: %d",
                total, pendientes, retirados), x + 250, y + 52);

        int bloquesVis = Math.min(48, pendientes * 4);
        for (int i = 0; i < bloquesVis; i++) {
            double bx = x + 35 + (i % 12) * 36;
            double by = y + 88 + (i / 12) * 27;
            gc.setFill(Color.web("#B0BEC5"));
            gc.setStroke(Color.web("#607D8B"));
            gc.setLineWidth(1);
            gc.fillRect(bx, by, 28, 18);
            gc.strokeRect(bx, by, 28, 18);
        }

        if (motor.getTiempoSecadoActual() < motor.getConfig().getSecado()) {
            double progreso = motor.getTiempoSecadoActual() / motor.getConfig().getSecado();
            gc.setFill(Color.web("#E3F2FD"));
            gc.fillRect(x + 28, y + 205, 444, 12);
            gc.setFill(Colores.CELESTE);
            gc.fillRect(x + 28, y + 205, 444 * progreso, 12);
        }
    }

    private void actualizarIndicadores() {
        lblRelojDj.setText(TiempoUtil.formatearMinutos(motor.getEstDj().getTiempoLaboral()));
        lblRelojVt.setText(TiempoUtil.formatearMinutos(motor.getEstVt().getTiempoLaboral()));
        lblDia.setText(motor.getDiaActual() + " / " + MotorSimulacion.MAXDIAS);
        lblAlmacen.setText(motor.getAlmacenBloques() + " bloques");

        if (motor.getEscenario().tieneDosRevolvedoras()) {
            String s = (motor.isRevolvedoraDjOcupada() ? "DJ:OCUP " : "DJ:LIB ")
                    + (motor.isRevolvedoraVtOcupada() ? "VT:OCUP" : "VT:LIB");
            lblRev.setText(s);
            lblRev.setTextFill(motor.isRevolvedoraDjOcupada() || motor.isRevolvedoraVtOcupada()
                    ? Colores.ROJO : Colores.VERDE);
        } else if (motor.isRevolvedoraCompartidaOcupada()) {
            lblRev.setText("OCUPADA");
            lblRev.setTextFill(Colores.ROJO);
        } else {
            lblRev.setText("LIBRE");
            lblRev.setTextFill(Colores.VERDE);
        }

        String estado;
        Color color;
        if (corriendo) {
            estado = "EN EJECUCIÓN";
            color = Colores.VERDE;
        } else {
            estado = "PAUSADA / LISTO";
            color = Colores.NARANJA;
        }
        switch (motor.getModo()) {
            case PRODUCCIONTERMINADA -> { estado = "PRODUCCIÓN TERMINADA"; color = Colores.VERDE; }
            case SECADO -> { estado = "SECADO / RETIRO"; color = Colores.CELESTE; }
            case PROCESO2TERMINADO -> { estado = "PROCESO 2 TERMINADO"; color = Colores.VERDE; }
            default -> {}
        }
        lblEstado.setText(estado);
        lblEstado.setTextFill(color);
    }

    private void mostrarInfo(String titulo, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    private void mostrarError(String titulo, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}
