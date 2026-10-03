import tkinter as tk
from tkinter import ttk
from dataclasses import dataclass


# ============================================================
# D4 - SIMULACIÓN GRÁFICA
# INDUSTRIA BLOQUERA DEL SURESTE
#
# PROCESO:
#     Mezclado
#        ↓
#      Paleado
#        ↓
#      Moldeo
#        ↓
#      Secado
#        ↓
#     Almacén
#
# Requiere:
# Python 3.x
# Tkinter
# ============================================================


# ============================================================
# TIEMPOS DEL MODELO - MINUTOS
# ============================================================

TIEMPO_MEZCLADO = 15
TIEMPO_PALEADO = 10
TIEMPO_MOLDEO = 20
TIEMPO_SECADO = 60

NUM_MOLDEADORES = 2
NUM_PALEADORES = 1
NUM_MEZCLADORAS = 1

MAX_BLOQUES = 30
MAX_BLOQUES_PROCESO = 8


# ============================================================
# COLORES
# ============================================================

BG = "#F4F6F8"
PANEL = "#FFFFFF"
DARK = "#263238"
GRAY = "#607D8B"
GREEN = "#2E7D32"
ORANGE = "#EF6C00"
BLUE = "#1565C0"
BROWN = "#795548"

LIGHT_BLUE = "#E3F2FD"
LIGHT_GREEN = "#E8F5E9"
LIGHT_ORANGE = "#FFF3E0"
LIGHT_GRAY = "#ECEFF1"


# ============================================================
# DATOS DEL BLOQUE
# ============================================================

@dataclass
class Bloque:
    numero: int
    etapa: int = 0
    progreso: float = 0.0
    tiempo_etapa: float = 0.0
    canvas_id: int = None


# ============================================================
# SIMULACIÓN
# ============================================================

class SimulacionBloquera:

    def __init__(self, root):

        self.root = root

        self.root.title(
            "D4 - Simulación | Industria Bloquera del Sureste"
        )

        self.root.geometry("1280x760")
        self.root.resizable(False, False)
        self.root.configure(bg=BG)

        # ====================================================
        # VARIABLES
        # ====================================================

        self.corriendo = False
        self.velocidad = 1.0

        self.tiempo = 0.0

        self.blocks_producidos = 0
        self.blocks_almacen = 0

        self.numero_bloque = 0

        self.bloques = []

        self.estado = "LISTO PARA INICIAR"

        self.ultimo_bloque_creado = -999

        # ====================================================
        # ESTACIONES (5 estaciones)
        # ====================================================

        self.estaciones = [
            ("Mezclado", 150),
            ("Paleado", 380),
            ("Moldeo", 610),
            ("Secado", 840),
            ("Almacén", 1070)
        ]

        self.crear_interfaz()
        self.dibujar_escenario()


    # ========================================================
    # INTERFAZ
    # ========================================================

    def crear_interfaz(self):

        # ----------------------------------------------------
        # ENCABEZADO
        # ----------------------------------------------------

        header = tk.Frame(
            self.root,
            bg=DARK,
            height=75
        )

        header.pack(fill="x")
        header.pack_propagate(False)

        tk.Label(
            header,
            text="INDUSTRIA BLOQUERA DEL SURESTE",
            bg=DARK,
            fg="white",
            font=("Arial", 21, "bold")
        ).pack(pady=(12, 0))

        tk.Label(
            header,
            text="D4 · Simulación gráfica del proceso productivo",
            bg=DARK,
            fg="#CFD8DC",
            font=("Arial", 10)
        ).pack()


        # ----------------------------------------------------
        # CONTENIDO
        # ----------------------------------------------------

        contenido = tk.Frame(
            self.root,
            bg=BG
        )

        contenido.pack(
            fill="both",
            expand=True,
            padx=18,
            pady=12
        )


        # ----------------------------------------------------
        # INDICADORES
        # ----------------------------------------------------

        indicadores = tk.Frame(
            contenido,
            bg=BG
        )

        indicadores.pack(
            fill="x",
            pady=(0, 10)
        )

        self.crear_indicador(
            indicadores,
            "ESTADO",
            "LISTO",
            GREEN
        )

        self.crear_indicador(
            indicadores,
            "TIEMPO",
            "0.0 min",
            BLUE
        )

        self.crear_indicador(
            indicadores,
            "PRODUCCIÓN",
            "0 bloques",
            BROWN
        )

        self.crear_indicador(
            indicadores,
            "ALMACÉN",
            "0 bloques",
            ORANGE
        )


        # ----------------------------------------------------
        # CANVAS
        # ----------------------------------------------------

        self.canvas = tk.Canvas(
            contenido,
            width=1240,
            height=405,
            bg=PANEL,
            highlightthickness=1,
            highlightbackground="#CFD8DC"
        )

        self.canvas.pack()


        # ----------------------------------------------------
        # CONTROLES
        # ----------------------------------------------------

        controles = tk.Frame(
            contenido,
            bg=BG
        )

        controles.pack(
            fill="x",
            pady=12
        )


        self.btn_iniciar = ttk.Button(
            controles,
            text="▶  Iniciar",
            command=self.iniciar
        )

        self.btn_iniciar.pack(
            side="left",
            padx=5
        )


        self.btn_pausa = ttk.Button(
            controles,
            text="Ⅱ  Pausar",
            command=self.pausar
        )

        self.btn_pausa.pack(
            side="left",
            padx=5
        )


        self.btn_reiniciar = ttk.Button(
            controles,
            text="↻  Reiniciar",
            command=self.reiniciar
        )

        self.btn_reiniciar.pack(
            side="left",
            padx=5
        )


        tk.Label(
            controles,
            text="Velocidad:",
            bg=BG,
            fg=DARK,
            font=("Arial", 10, "bold")
        ).pack(
            side="left",
            padx=(30, 5)
        )


        self.combo_velocidad = ttk.Combobox(
            controles,
            values=[
                "Lenta",
                "Normal",
                "Rápida"
            ],
            state="readonly",
            width=10
        )

        self.combo_velocidad.set("Normal")

        self.combo_velocidad.bind(
            "<<ComboboxSelected>>",
            self.cambiar_velocidad
        )

        self.combo_velocidad.pack(
            side="left"
        )


        # ----------------------------------------------------
        # RECURSOS
        # ----------------------------------------------------

        recursos = tk.Frame(
            contenido,
            bg=PANEL,
            highlightbackground="#CFD8DC",
            highlightthickness=1
        )

        recursos.pack(
            fill="x",
            pady=(0, 5)
        )


        tk.Label(
            recursos,
            text="RECURSOS DISPONIBLES",
            bg=PANEL,
            fg=DARK,
            font=("Arial", 10, "bold")
        ).pack(
            side="left",
            padx=15,
            pady=8
        )


        tk.Label(
            recursos,
            text=(
                "👷 2 Moldeadores     "
                "🧑‍🌾 1 Paleador     "
                "⚙ 1 Mezcladora"
            ),
            bg=PANEL,
            fg=GRAY,
            font=("Arial", 10)
        ).pack(
            side="left"
        )


        # ----------------------------------------------------
        # DATOS DEL MODELO
        # ----------------------------------------------------

        tk.Label(
            contenido,
            text=(
                "Datos utilizados: mezclado 15 min · "
                "paleado 10 min · moldeo 20 min · "
                "secado 60 min"
            ),
            bg=BG,
            fg=GRAY,
            font=("Arial", 8)
        ).pack(pady=3)


    # ========================================================
    # INDICADORES
    # ========================================================

    def crear_indicador(
        self,
        parent,
        titulo,
        valor,
        color
    ):

        frame = tk.Frame(
            parent,
            bg=PANEL,
            width=290,
            height=70,
            highlightbackground="#D5DDE1",
            highlightthickness=1
        )

        frame.pack(
            side="left",
            padx=5,
            fill="x",
            expand=True
        )

        frame.pack_propagate(False)


        tk.Label(
            frame,
            text=titulo,
            bg=PANEL,
            fg=GRAY,
            font=("Arial", 8, "bold")
        ).pack(
            pady=(8, 0)
        )


        label = tk.Label(
            frame,
            text=valor,
            bg=PANEL,
            fg=color,
            font=("Arial", 14, "bold")
        )

        label.pack()


        if titulo == "ESTADO":
            self.lbl_estado = label

        elif titulo == "TIEMPO":
            self.lbl_tiempo = label

        elif titulo == "PRODUCCIÓN":
            self.lbl_producidos = label

        elif titulo == "ALMACÉN":
            self.lbl_almacen = label


    # ========================================================
    # DIBUJAR ESCENARIO
    # ========================================================

    def dibujar_escenario(self):

        self.canvas.delete("all")


        # ----------------------------------------------------
        # TÍTULO
        # ----------------------------------------------------

        self.canvas.create_text(
            620,
            22,
            text="FLUJO DEL PROCESO PRODUCTIVO",
            fill=DARK,
            font=("Arial", 15, "bold")
        )


        # ----------------------------------------------------
        # LÍNEA PRINCIPAL
        # ----------------------------------------------------

        self.canvas.create_line(
            90,
            190,
            1130,
            190,
            fill="#B0BEC5",
            width=5
        )


        # ----------------------------------------------------
        # ESTACIONES
        # ----------------------------------------------------

        for i, (nombre, x) in enumerate(self.estaciones):

            self.canvas.create_oval(
                x - 8,
                182,
                x + 8,
                198,
                fill=GRAY,
                outline=""
            )

            self.canvas.create_text(
                x,
                220,
                text=str(i + 1),
                fill=GRAY,
                font=("Arial", 8, "bold")
            )

            self.canvas.create_text(
                x,
                245,
                text=nombre,
                fill=DARK,
                font=("Arial", 9, "bold")
            )


        # ----------------------------------------------------
        # MEZCLADORA
        # ----------------------------------------------------

        x = self.estaciones[0][1]

        self.canvas.create_oval(
            x - 30,
            145,
            x + 30,
            205,
            outline=ORANGE,
            width=4
        )

        self.canvas.create_text(
            x,
            175,
            text="MEZCLA",
            fill=ORANGE,
            font=("Arial", 8, "bold")
        )


        # ----------------------------------------------------
        # PALEADOR
        # ----------------------------------------------------

        x = self.estaciones[1][1]

        self.canvas.create_oval(
            x - 8,
            140,
            x + 8,
            156,
            fill="#FFCC80",
            outline=""
        )

        self.canvas.create_line(
            x,
            156,
            x,
            185,
            fill=BLUE,
            width=5
        )

        self.canvas.create_line(
            x,
            165,
            x - 18,
            177,
            fill=BLUE,
            width=3
        )

        self.canvas.create_line(
            x,
            165,
            x + 18,
            177,
            fill=BLUE,
            width=3
        )

        self.canvas.create_line(
            x,
            185,
            x - 10,
            198,
            fill=DARK,
            width=3
        )

        self.canvas.create_line(
            x,
            185,
            x + 10,
            198,
            fill=DARK,
            width=3
        )


        # ----------------------------------------------------
        # MOLDEO
        # ----------------------------------------------------

        x = self.estaciones[2][1]

        self.canvas.create_rectangle(
            x - 32,
            155,
            x + 32,
            195,
            outline=BROWN,
            width=4
        )

        self.canvas.create_line(
            x - 20,
            175,
            x + 20,
            175,
            fill=BROWN,
            width=3
        )

        self.canvas.create_text(
            x,
            135,
            text="MOLDE",
            fill=BROWN,
            font=("Arial", 8, "bold")
        )


        # ----------------------------------------------------
        # SECADO
        # ----------------------------------------------------

        x = self.estaciones[3][1]

        for fila in range(2):

            for col in range(3):

                xx = x - 28 + col * 28
                yy = 155 + fila * 25

                self.canvas.create_rectangle(
                    xx,
                    yy,
                    xx + 22,
                    yy + 18,
                    fill="#D7CCC8",
                    outline=BROWN
                )


        # ----------------------------------------------------
        # ALMACÉN
        # ----------------------------------------------------

        x = self.estaciones[4][1]

        self.canvas.create_rectangle(
            x - 40,
            145,
            x + 40,
            200,
            fill=LIGHT_GRAY,
            outline=GRAY,
            width=2
        )

        self.canvas.create_text(
            x,
            125,
            text="📦",
            font=("Arial", 20)
        )


        # ----------------------------------------------------
        # PERSONAL OPERATIVO
        # ----------------------------------------------------

        self.canvas.create_text(
            620,
            320,
            text="PERSONAL OPERATIVO",
            fill=DARK,
            font=("Arial", 10, "bold")
        )

        self.canvas.create_text(
            620,
            345,
            text=(
                "👷 Moldeador 1     "
                "👷 Moldeador 2     "
                "🧑‍🌾 Paleador"
            ),
            fill=GRAY,
            font=("Arial", 10)
        )


        # ----------------------------------------------------
        # LEYENDA
        # ----------------------------------------------------

        self.canvas.create_rectangle(
            40,
            365,
            1240,
            395,
            fill="#FAFAFA",
            outline="#ECEFF1"
        )

        self.canvas.create_text(
            640,
            380,
            text=(
                "● Proceso activo     "
                "■ Producto     "
                "→ Flujo de producción     "
                "✓ Producto almacenado"
            ),
            fill=GRAY,
            font=("Arial", 9)
        )


    # ========================================================
    # CAMBIAR VELOCIDAD
    # ========================================================

    def cambiar_velocidad(self, event=None):

        valor = self.combo_velocidad.get()

        if valor == "Lenta":
            self.velocidad = 0.5

        elif valor == "Rápida":
            self.velocidad = 3.0

        else:
            self.velocidad = 1.0


    # ========================================================
    # INICIAR
    # ========================================================

    def iniciar(self):

        if not self.corriendo:

            self.corriendo = True

            self.estado = "SIMULACIÓN EN EJECUCIÓN"

            self.lbl_estado.config(
                text=self.estado,
                fg=GREEN
            )

            self.animar()


    # ========================================================
    # PAUSAR
    # ========================================================

    def pausar(self):

        self.corriendo = False

        self.estado = "SIMULACIÓN PAUSADA"

        self.lbl_estado.config(
            text=self.estado,
            fg=ORANGE
        )


    # ========================================================
    # REINICIAR
    # ========================================================

    def reiniciar(self):

        self.corriendo = False

        self.tiempo = 0.0

        self.blocks_producidos = 0
        self.blocks_almacen = 0

        self.numero_bloque = 0

        self.bloques = []

        self.ultimo_bloque_creado = -999

        self.estado = "LISTO PARA INICIAR"

        self.lbl_estado.config(
            text=self.estado,
            fg=GREEN
        )

        self.actualizar_datos()

        self.dibujar_escenario()


    # ========================================================
    # CREAR BLOQUE
    # ========================================================

    def crear_bloque(self):

        if self.numero_bloque >= MAX_BLOQUES:
            return

        if len(self.bloques) >= MAX_BLOQUES_PROCESO:
            return


        self.numero_bloque += 1

        bloque = Bloque(
            numero=self.numero_bloque
        )


        x = self.estaciones[0][1]


        bloque.canvas_id = self.canvas.create_rectangle(
            x - 10,
            175,
            x + 10,
            195,
            fill=ORANGE,
            outline=""
        )


        self.bloques.append(bloque)


    # ========================================================
    # TIEMPO DE CADA ETAPA
    # ========================================================

    def tiempo_de_etapa(self, etapa):

        tiempos = [
            TIEMPO_MEZCLADO,   # 0 Mezclado
            TIEMPO_PALEADO,    # 1 Paleado
            TIEMPO_MOLDEO,     # 2 Moldeo
            TIEMPO_SECADO,     # 3 Secado
            0                  # 4 Almacén (final)
        ]

        return tiempos[etapa]


    # ========================================================
    # MOVER BLOQUE
    # ========================================================

    def mover_bloque(self, bloque):

        etapa = bloque.etapa


        # ----------------------------------------------------
        # YA ESTÁ EN ALMACÉN → se elimina visualmente
        # ----------------------------------------------------

        if etapa == 4:

            self.canvas.delete(bloque.canvas_id)

            if bloque in self.bloques:
                self.bloques.remove(bloque)

            return


        # ----------------------------------------------------
        # TIEMPO DE LA ETAPA
        # ----------------------------------------------------

        tiempo_requerido = self.tiempo_de_etapa(etapa)


        # ----------------------------------------------------
        # MOVER VISUALMENTE
        # ----------------------------------------------------

        if etapa < 4:

            x_actual = self.estaciones[etapa][1]
            x_siguiente = self.estaciones[etapa + 1][1]

            distancia = x_siguiente - x_actual

            bloque.progreso += (
                1.8 * self.velocidad
            )


            if bloque.progreso >= distancia:

                bloque.progreso = 0
                bloque.etapa += 1
                bloque.tiempo_etapa = 0


            x = (
                self.estaciones[bloque.etapa][1]
                + bloque.progreso
            )


            self.canvas.coords(
                bloque.canvas_id,
                x - 10,
                175,
                x + 10,
                195
            )


        # ----------------------------------------------------
        # COLORES DEL BLOQUE
        # ----------------------------------------------------

        colores = [
            ORANGE,      # Mezclado
            BLUE,        # Paleado
            BROWN,       # Moldeo
            "#8D6E63",   # Secado
            ORANGE       # Almacén
        ]


        if bloque.etapa < len(colores):

            self.canvas.itemconfig(
                bloque.canvas_id,
                fill=colores[bloque.etapa]
            )


        # ----------------------------------------------------
        # TIEMPO DE PROCESAMIENTO
        # ----------------------------------------------------

        if bloque.etapa in [0, 1, 2, 3]:

            bloque.tiempo_etapa += (
                0.5 * self.velocidad
            )


            if bloque.tiempo_etapa >= tiempo_requerido:

                bloque.progreso = 0
                bloque.etapa += 1
                bloque.tiempo_etapa = 0


                # --------------------------------------------
                # LLEGÓ A ALMACÉN
                # --------------------------------------------

                if bloque.etapa == 4:

                    self.blocks_producidos += 1
                    self.blocks_almacen += 1


    # ========================================================
    # ANIMACIÓN
    # ========================================================

    def animar(self):

        if not self.corriendo:
            return


        # ----------------------------------------------------
        # RELOJ
        # ----------------------------------------------------

        self.tiempo += (
            0.5 * self.velocidad
        )


        # ----------------------------------------------------
        # CREACIÓN DE BLOQUES
        # ----------------------------------------------------

        if (

            self.tiempo -
            self.ultimo_bloque_creado >= 12

            and self.numero_bloque < MAX_BLOQUES

            and len(self.bloques)
            < MAX_BLOQUES_PROCESO

        ):

            self.crear_bloque()

            self.ultimo_bloque_creado = self.tiempo


        # ----------------------------------------------------
        # MOVER BLOQUES
        # ----------------------------------------------------

        for bloque in self.bloques[:]:

            self.mover_bloque(bloque)


        # ----------------------------------------------------
        # ESTADO
        # ----------------------------------------------------

        if len(self.bloques) == 0:

            if self.numero_bloque >= MAX_BLOQUES:

                self.estado = "PRODUCCIÓN FINALIZADA"

            else:

                self.estado = "ESPERANDO PRODUCCIÓN"

        else:

            self.estado = "PROCESANDO BLOQUES"


        self.lbl_estado.config(
            text=self.estado,
            fg=GREEN
        )


        self.actualizar_datos()


        # ----------------------------------------------------
        # SIGUIENTE CICLO
        # ----------------------------------------------------

        self.root.after(
            100,
            self.animar
        )


    # ========================================================
    # ACTUALIZAR INDICADORES
    # ========================================================

    def actualizar_datos(self):

        self.lbl_tiempo.config(
            text=f"{self.tiempo:.1f} min"
        )

        self.lbl_producidos.config(
            text=f"{self.blocks_producidos} bloques"
        )

        self.lbl_almacen.config(
            text=f"{self.blocks_almacen} bloques"
        )


# ============================================================
# PROGRAMA PRINCIPAL
# ============================================================

if __name__ == "__main__":

    root = tk.Tk()

    app = SimulacionBloquera(root)

    root.mainloop()