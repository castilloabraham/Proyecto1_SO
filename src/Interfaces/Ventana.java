/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Estructuras.Lista;
import Estructuras.Nodo;
import Politicas.SchedulerManager;
import Politicas.FCFS;
import Politicas.RoundRobin;
import Politicas.SRT;
import Politicas.PEP;
import Politicas.EDF;
import Process.PCB;
import Process.InstructionType;
import Process.ProcessType;
import Process.Mediumtermscheduler;
import clock.SystemClock;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
/**
 *
 * @author Miguel
 */
public class Ventana extends javax.swing.JFrame {
     // ─── PALETA ────────────────────────────────────────────────
    private static final Color C_FONDO   = new Color(15, 23, 42);    // #0F172A
    private static final Color C_PANEL   = new Color(30, 41, 59);    // #1E293B
    private static final Color C_CARD    = new Color(51, 65, 85);    // #334155
    private static final Color C_BORDE   = new Color(56, 189, 248);  // #38BDF8
    private static final Color C_TEXTO   = new Color(226, 232, 240); // #E2E8F0
    private static final Color C_VERDE   = new Color(34, 197, 94);   // #22C55E
    private static final Color C_ROJO    = new Color(239, 68, 68);   // #EF4444
    private static final Color C_NARANJA = new Color(245, 158, 11);  // #F59E0B
    private static final Color C_MORADO  = new Color(168, 85, 247);  // #A855F7
    private static final Color C_GRIS    = new Color(100, 116, 139); // #64748B

    // ─── COMPONENTES PRINCIPALES ──────────────────────────────
    // Paneles contenedores de tarjetas (scroll horizontal)
    private JPanel panelContenedorListos;
    private JPanel panelContenedorBloqueados;
    private JPanel panelContenedorListosSusp;
    private JPanel panelContenedorBloqSusp;
    private JPanel panelContenedorTerminados;

    // CPU y estado
    private JLabel lblReloj, lblModo, lblMemoria;
    private JLabel lblNombreCPU, lblIdCPU, lblPcCPU, lblMarCPU, lblEstadoCPU, lblPrioCPU, lblDeadlineCPU;
    private JProgressBar barraDeadline;

    // Controles
    private JComboBox<String> cmbAlgoritmos;
    private JSlider sliderVelocidad;
    private JLabel lblVelocidad;
    private JSpinner spnQuantum, spnMaxMem;
    private JButton btnIniciar, btnPausar, btnReset, btnCrear1, btnCrear20, btnEmergencia, btnCargar;

    // Log de eventos (estático para que cualquier clase pueda escribir)
    private static JTextArea consolaEventos;

    // ─── BACKEND ───────────────────────────────────────────────
    private SystemClock      clock;
    private SchedulerManager scheduler;
    private final Random     rng = new Random();

    // ─── ESTADO DE SIMULACIÓN ──────────────────────────────────
    private volatile boolean simulacionCorriendo = false;
    private volatile boolean pausado             = false;
    private Thread hiloSimulacion;
    private Thread hiloInterrupciones;
    private Timer  guiTimer;  // Refresca la UI cada 250ms (sin bloquear)

    // ─── CONSTRUCTOR ───────────────────────────────────────────
    public Ventana() {
        // 1. Backend primero (sin tocar la UI)
        clock     = SystemClock.getInstance();
        scheduler = new SchedulerManager(clock, 6);

        // 2. Configuración del JFrame
        setTitle("UNIMET-SAT RTOS | Mission Control Center");
        setSize(1400, 820);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_FONDO);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        // 3. Construir UI
        inicializarComponentes();

        // 4. Procesos iniciales y mostrar
        generarProcesosAleatorios(5);
        actualizarTodosPaneles();
        actualizarEstadoBotones();
        setVisible(true);

        logEvento("Sistema iniciado. 5 procesos en cola. Presiona INICIAR.");
    }

    // ═══════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA UI
    // ═══════════════════════════════════════════════════════════
    private void inicializarComponentes() {

        // ── PANEL NORTE: Header ─────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(C_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, C_BORDE),
            new EmptyBorder(8, 14, 8, 14)));

        JLabel titulo = new JLabel("◈  UNIMET-SAT RTOS SIMULATOR");
        titulo.setFont(new Font("Impact", Font.PLAIN, 26));
        titulo.setForeground(C_BORDE);
        header.add(titulo, BorderLayout.WEST);

        lblReloj = new JLabel("MISSION CLOCK: Cycle 0000", SwingConstants.CENTER);
        lblReloj.setFont(new Font("Monospaced", Font.BOLD, 18));
        lblReloj.setForeground(C_VERDE);

        lblModo = new JLabel("[ STANDBY ]", SwingConstants.CENTER);
        lblModo.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblModo.setForeground(Color.YELLOW);

        lblMemoria = new JLabel("Memory: 0/6", SwingConstants.CENTER);
        lblMemoria.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblMemoria.setForeground(C_VERDE);

        JPanel centerHeader = new JPanel(new GridLayout(3, 1, 2, 2));
        centerHeader.setOpaque(false);
        centerHeader.add(lblReloj);
        centerHeader.add(lblModo);
        centerHeader.add(lblMemoria);
        header.add(centerHeader, BorderLayout.CENTER);

        // Algoritmo y velocidad a la derecha del header
        JPanel derHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        derHeader.setOpaque(false);
        cmbAlgoritmos = new JComboBox<>(new String[]{"FCFS", "Round Robin", "SRT", "PEP", "EDF"});
        cmbAlgoritmos.setFont(new Font("Monospaced", Font.BOLD, 12));
        cmbAlgoritmos.setBackground(C_CARD);
        cmbAlgoritmos.setForeground(C_BORDE);
        cmbAlgoritmos.addActionListener(e -> {
            if (simulacionCorriendo) cambiarPolitica();
        });
        derHeader.add(label("Algoritmo:")); derHeader.add(cmbAlgoritmos);

        sliderVelocidad = new JSlider(50, 3000, 1000);
        sliderVelocidad.setOpaque(false);
        sliderVelocidad.setPreferredSize(new Dimension(130, 24));
        sliderVelocidad.setInverted(true);
        lblVelocidad = label("1000ms");
        sliderVelocidad.addChangeListener(e -> {
            lblVelocidad.setText(sliderVelocidad.getValue() + "ms");
        });
        derHeader.add(label("Vel:")); derHeader.add(sliderVelocidad); derHeader.add(lblVelocidad);

        spnQuantum = spinnerEstilizado(3, 1, 20, 1);
        spnMaxMem  = spinnerEstilizado(6, 1, 20, 1);
        spnQuantum.addChangeListener(e -> scheduler.setQuantum((Integer) spnQuantum.getValue()));
        spnMaxMem.addChangeListener(e  -> scheduler.getMTS().setMaxProcessesInMemory((Integer) spnMaxMem.getValue()));
        derHeader.add(label("Quantum:")); derHeader.add(spnQuantum);
        derHeader.add(label("MaxRAM:"));  derHeader.add(spnMaxMem);
        header.add(derHeader, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── PANEL CENTRAL: Colas de estado ─────────────────────
        // Organizadas verticalmente: Fila1(Listos | CPU | Bloqueados)
        //                            Fila2(ListosSusp | Terminados | BloqSusp)
        JPanel centro = new JPanel(new GridLayout(2, 1, 6, 6));
        centro.setOpaque(false);

        // Fila 1
        JPanel fila1 = new JPanel(new BorderLayout(6, 0));
        fila1.setOpaque(false);
        JScrollPane spListos = configurarPanelCola("READY QUEUE (RAM)", C_VERDE, true);
        panelContenedorListos = extraerPanel(spListos);
        JScrollPane spBloq = configurarPanelCola("BLOCKED QUEUE (I/O)", C_ROJO, true);
        panelContenedorBloqueados = extraerPanel(spBloq);

        fila1.add(spListos, BorderLayout.WEST);
        fila1.add(construirPanelCPU(), BorderLayout.CENTER);
        fila1.add(spBloq,   BorderLayout.EAST);
        centro.add(fila1);

        // Fila 2
        JPanel fila2 = new JPanel(new GridLayout(1, 3, 6, 0));
        fila2.setOpaque(false);
        JScrollPane spListosSusp = configurarPanelCola("READY-SUSPENDED (SWAP)", C_GRIS, true);
        panelContenedorListosSusp = extraerPanel(spListosSusp);
        JScrollPane spTerm = configurarPanelCola("TERMINATED", C_MORADO, true);
        panelContenedorTerminados = extraerPanel(spTerm);
        JScrollPane spBloqSusp = configurarPanelCola("BLOCKED-SUSPENDED (SWAP)", C_GRIS, true);
        panelContenedorBloqSusp = extraerPanel(spBloqSusp);
        fila2.add(spListosSusp);
        fila2.add(spTerm);
        fila2.add(spBloqSusp);
        centro.add(fila2);

        add(centro, BorderLayout.CENTER);

        // ── PANEL SUR: Botones + Log ───────────────────────────
        JPanel sur = new JPanel(new BorderLayout(8, 0));
        sur.setOpaque(false);

        // Botones
        btnIniciar    = boton("▶  INICIAR",      C_VERDE);
        btnPausar     = boton("⏸  PAUSAR",        C_NARANJA);
        btnReset      = boton("↺  RESET",         C_ROJO);
        btnCrear1     = boton("＋  Crear 1",      C_BORDE);
        btnCrear20    = boton("⚡  Generar 20",   C_BORDE);
        btnEmergencia = boton("☢  Emergencia",    C_ROJO);
        btnCargar     = boton("📂  Cargar CSV",   C_GRIS);

        btnPausar.setEnabled(false);
        btnReset.setEnabled(false);

        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        filaBotones.setBackground(C_PANEL);
        filaBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDE));
        filaBotones.add(btnIniciar); filaBotones.add(btnPausar); filaBotones.add(btnReset);
        filaBotones.add(new JSeparator(SwingConstants.VERTICAL));
        filaBotones.add(btnCrear1); filaBotones.add(btnCrear20);
        filaBotones.add(btnEmergencia); filaBotones.add(btnCargar);
        sur.add(filaBotones, BorderLayout.NORTH);

        // Log de eventos
        consolaEventos = new JTextArea(5, 40);
        consolaEventos.setEditable(false);
        consolaEventos.setBackground(Color.BLACK);
        consolaEventos.setForeground(C_VERDE);
        consolaEventos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consolaEventos.setLineWrap(true);
        consolaEventos.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(consolaEventos);
        scrollLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollLog.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_GRIS),
            "  System Log  ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 11), Color.WHITE));
        sur.add(scrollLog, BorderLayout.CENTER);

        add(sur, BorderLayout.SOUTH);

        // ── EVENTOS DE BOTONES ─────────────────────────────────
        btnIniciar.addActionListener(e    -> iniciarSimulacion());
        btnPausar.addActionListener(e     -> togglePausa());
        btnReset.addActionListener(e      -> resetearSistema());
        btnCrear1.addActionListener(e     -> { generarProcesosAleatorios(1);  actualizarTodosPaneles(); });
        btnCrear20.addActionListener(e    -> { generarProcesosAleatorios(20); actualizarTodosPaneles(); logEvento("20 procesos generados."); });
        btnEmergencia.addActionListener(e -> generarEmergencia());
        btnCargar.addActionListener(e     -> cargarCSV());
    }

    // ─── Panel CPU (centro de fila 1) ──────────────────────────
    private JPanel construirPanelCPU() {
        JPanel p = new JPanel(new GridLayout(8, 2, 4, 6));
        p.setBackground(C_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_BORDE, 2),
                "  ⚙  CPU EN EJECUCIÓN  ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Monospaced", Font.BOLD, 12), C_BORDE),
            new EmptyBorder(10, 10, 10, 10)));

        p.add(label("Proceso:")); lblNombreCPU  = valorCPU("IDLE");     p.add(lblNombreCPU);
        p.add(label("ID:"));      lblIdCPU      = valorCPU("—");        p.add(lblIdCPU);
        p.add(label("PC:"));      lblPcCPU      = valorCPU("—");        p.add(lblPcCPU);
        p.add(label("MAR:"));     lblMarCPU     = valorCPU("—");        p.add(lblMarCPU);
        p.add(label("Estado:"));  lblEstadoCPU  = valorCPU("STANDBY");  p.add(lblEstadoCPU);
        p.add(label("Prioridad:"));lblPrioCPU   = valorCPU("—");        p.add(lblPrioCPU);
        p.add(label("Deadline:")); lblDeadlineCPU = valorCPU("—");      p.add(lblDeadlineCPU);

        barraDeadline = new JProgressBar(0, 100);
        barraDeadline.setStringPainted(true);
        barraDeadline.setString("Deadline: —");
        barraDeadline.setFont(new Font("Monospaced", Font.BOLD, 11));
        barraDeadline.setForeground(C_VERDE);
        barraDeadline.setBackground(C_FONDO);
        p.add(label("Urgencia:")); p.add(barraDeadline);

        return p;
    }

    // ═══════════════════════════════════════════════════════════
    //  TARJETAS DE PROCESO (equivalente a PanelProcesoVista)
    // ═══════════════════════════════════════════════════════════

    /**
     * Crea y devuelve una tarjeta visual para un PCB dado.
     * Equivalente a PanelProcesoVista de la guía pero para tu PCB.
     */
    private JPanel crearTarjetaPCB(PCB p, Color colorBorde) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorBorde, 1),
            new EmptyBorder(6, 8, 6, 8)));
        card.setPreferredSize(new Dimension(130, 140));
        card.setMaximumSize(new Dimension(130, 140));

        card.add(filaCard(p.getProcessName(), new Font("Monospaced", Font.BOLD, 11), colorBorde));
        card.add(Box.createVerticalStrut(3));
        card.add(filaCard("ID: "  + p.getProcessName(), new Font("Monospaced", Font.PLAIN, 10), C_TEXTO));
        card.add(filaCard("PC: "  + p.getProgramCounter(), new Font("Monospaced", Font.PLAIN, 10), C_TEXTO));
        card.add(filaCard("MAR: " + p.getMemoryAddressRegister(), new Font("Monospaced", Font.PLAIN, 10), C_TEXTO));
        card.add(filaCard("Prio: "+ p.getPriority(), new Font("Monospaced", Font.PLAIN, 10), C_NARANJA));
        card.add(filaCard("DL: "  + p.getRemainingDeadline(), new Font("Monospaced", Font.PLAIN, 10),
                          p.getRemainingDeadline() < 10 ? C_ROJO : C_VERDE));

        // Mini barra de deadline
        int pct = p.getDeadline() > 0 ? Math.max(0, (p.getRemainingDeadline() * 100) / p.getDeadline()) : 0;
        JProgressBar mini = new JProgressBar(0, 100);
        mini.setValue(pct);
        mini.setPreferredSize(new Dimension(110, 6));
        mini.setMaximumSize(new Dimension(110, 6));
        mini.setForeground(pct < 25 ? C_ROJO : pct < 50 ? C_NARANJA : C_VERDE);
        mini.setBackground(C_FONDO);
        mini.setBorderPainted(false);
        card.add(Box.createVerticalStrut(4));
        card.add(mini);

        return card;
    }

    private JLabel filaCard(String txt, Font f, Color c) {
        JLabel l = new JLabel(txt);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ═══════════════════════════════════════════════════════════
    //  TIMER GUI (patrón guiTimer de la referencia)
    // ═══════════════════════════════════════════════════════════

    /** Inicia el timer que refresca todos los paneles cada 250ms */
    private void iniciarTimerGUI() {
        if (guiTimer != null && guiTimer.isRunning()) guiTimer.stop();

        guiTimer = new Timer(250, e -> {
            lblReloj.setText("MISSION CLOCK: Cycle " + String.format("%04d", clock.getCurrentCycle()));
            actualizarPanelCPU();
            actualizarTodosPaneles();
            actualizarMemoria();
        });
        guiTimer.setInitialDelay(100);
        guiTimer.setRepeats(true);
        guiTimer.start();
    }

    // ═══════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN DE PANELES
    // ═══════════════════════════════════════════════════════════

    private void actualizarTodosPaneles() {
        SwingUtilities.invokeLater(() -> {
            SchedulerManager.Policy pol = scheduler.getActivePolicy();

            // Ready queue
            Lista<PCB> ready = null;
            Lista<PCB> blocked = null;
            try {
                switch (pol) {
                    case FCFS:
                        blocked = ((FCFS) scheduler.getActiveScheduler()).getBlockedList();
                        break;
                    case ROUND_ROBIN:
                        blocked = ((RoundRobin) scheduler.getActiveScheduler()).getBlockedList();
                        break;
                    case SRT:
                        ready   = ((SRT) scheduler.getActiveScheduler()).getReadyList();
                        blocked = ((SRT) scheduler.getActiveScheduler()).getBlockedList();
                        break;
                    case PEP:
                        ready   = ((PEP) scheduler.getActiveScheduler()).getReadyList();
                        blocked = ((PEP) scheduler.getActiveScheduler()).getBlockedList();
                        break;
                    case EDF:
                        ready   = ((EDF) scheduler.getActiveScheduler()).getReadyList();
                        blocked = ((EDF) scheduler.getActiveScheduler()).getBlockedList();
                        break;
                }
            } catch (ClassCastException ex) { /* política en transición */ }

            llenarPanelConTarjetas(panelContenedorListos, ready, scheduler.getReadyQueueSize(), C_VERDE);
            llenarPanelConTarjetas(panelContenedorBloqueados, blocked, 0, C_ROJO);

            Mediumtermscheduler mts = scheduler.getMTS();
            llenarPanelConTarjetas(panelContenedorListosSusp, mts.getReadySuspendedList(),  0, C_GRIS);
            llenarPanelConTarjetas(panelContenedorBloqSusp,   mts.getBlockedSuspendedList(), 0, C_GRIS);

            // Terminados: acceso directo a la lista del scheduler
            Lista<PCB> terminados = null;
            try {
                switch (pol) {
                    case FCFS:         terminados = ((FCFS)       scheduler.getActiveScheduler()).getTerminatedList(); break;
                    case ROUND_ROBIN:  terminados = ((RoundRobin) scheduler.getActiveScheduler()).getTerminatedList(); break;
                    case SRT:          terminados = ((SRT)        scheduler.getActiveScheduler()).getTerminatedList(); break;
                    case PEP:          terminados = ((PEP)        scheduler.getActiveScheduler()).getTerminatedList(); break;
                    case EDF:          terminados = ((EDF)        scheduler.getActiveScheduler()).getTerminatedList(); break;
                }
            } catch (ClassCastException ex) {}
            llenarPanelConTarjetas(panelContenedorTerminados, terminados, 0, C_MORADO);
        });
    }

    /**
     * Llena un panel horizontal con tarjetas de PCB.
     * Si lista == null pero cantidadFallback > 0, muestra un contador
     * (para FCFS/RR que usan Cola sin acceso directo).
     */
    private void llenarPanelConTarjetas(JPanel panel, Lista<PCB> lista, int cantidadFallback, Color color) {
        if (panel == null) return;
        panel.removeAll();

        if (lista == null || lista.getCabeza() == null) {
            // FCFS / RR: la Cola no expone nodos, solo mostramos conteo
            if (cantidadFallback > 0) {
                JLabel lbl = new JLabel("  " + cantidadFallback + " proceso(s) en cola  ");
                lbl.setForeground(color);
                lbl.setFont(new Font("Monospaced", Font.BOLD, 13));
                lbl.setAlignmentY(Component.CENTER_ALIGNMENT);
                panel.add(Box.createHorizontalGlue());
                panel.add(lbl);
                panel.add(Box.createHorizontalGlue());
            } else {
                JLabel vacio = new JLabel("  Vacío  ");
                vacio.setForeground(C_GRIS);
                vacio.setFont(new Font("Monospaced", Font.ITALIC, 12));
                vacio.setAlignmentY(Component.CENTER_ALIGNMENT);
                panel.add(Box.createHorizontalGlue());
                panel.add(vacio);
                panel.add(Box.createHorizontalGlue());
            }
        } else {
            panel.add(Box.createHorizontalStrut(6));
            Nodo<PCB> n = lista.getCabeza();
            while (n != null) {
                panel.add(crearTarjetaPCB(n.getDato(), color));
                panel.add(Box.createHorizontalStrut(6));
                n = n.getSiguiente();
            }
        }

        panel.revalidate();
        panel.repaint();
    }

    private void actualizarPanelCPU() {
        PCB p = scheduler.getRunningProcess();
        if (p == null) {
            lblNombreCPU.setText("IDLE");
            lblIdCPU.setText("—"); lblPcCPU.setText("—"); lblMarCPU.setText("—");
            lblEstadoCPU.setText("STANDBY"); lblPrioCPU.setText("—"); lblDeadlineCPU.setText("—");
            lblModo.setText("MODO: OS / PLANIFICADOR"); lblModo.setForeground(Color.YELLOW);
            barraDeadline.setValue(0); barraDeadline.setString("Deadline: —");
            barraDeadline.setForeground(C_VERDE);
        } else {
            lblNombreCPU.setText(p.getProcessName());
            lblIdCPU.setText(p.getProcessName());
            lblPcCPU.setText(String.valueOf(p.getProgramCounter()));
            lblMarCPU.setText(String.valueOf(p.getMemoryAddressRegister()));
            lblEstadoCPU.setText(p.getCurrentState().getDisplayName());
            lblPrioCPU.setText(String.valueOf(p.getPriority()));
            lblDeadlineCPU.setText(String.valueOf(p.getRemainingDeadline()));
            lblModo.setText("MODO: USUARIO (" + p.getProcessName() + ")");
            lblModo.setForeground(C_BORDE);

            int dl  = p.getRemainingDeadline(), orig = p.getDeadline();
            int pct = orig > 0 ? Math.max(0, (dl * 100) / orig) : 0;
            barraDeadline.setValue(pct);
            barraDeadline.setString("Deadline: " + dl + " ciclos (" + pct + "%)");
            barraDeadline.setForeground(pct < 25 ? C_ROJO : pct < 50 ? C_NARANJA : C_VERDE);
        }
    }

    private void actualizarMemoria() {
        Mediumtermscheduler mts = scheduler.getMTS();
        int mem = mts.getProcessesInMemory(), max = mts.getMaxProcessesInMemory();
        lblMemoria.setText("Memory: " + mem + "/" + max
            + "  |  SwapOut: " + mts.getSwapOutCount()
            + "  SwapIn: " + mts.getSwapInCount());
        lblMemoria.setForeground(mem >= max ? C_ROJO : C_VERDE);
    }

    private void actualizarEstadoBotones() {
        btnIniciar.setEnabled(!simulacionCorriendo);
        btnCrear1.setEnabled(!simulacionCorriendo);
        btnCrear20.setEnabled(!simulacionCorriendo);
        btnCargar.setEnabled(!simulacionCorriendo);
        cmbAlgoritmos.setEnabled(true); // Siempre habilitado
        btnPausar.setEnabled(simulacionCorriendo);
        btnReset.setEnabled(simulacionCorriendo);
        btnEmergencia.setEnabled(simulacionCorriendo);
    }

    // ═══════════════════════════════════════════════════════════
    //  HILO DE SIMULACIÓN (mismo patrón que BotonIniciarActionPerformed)
    // ═══════════════════════════════════════════════════════════

    private void iniciarSimulacion() {
        if (simulacionCorriendo) return;

        limpiarSistemaParaNuevaSimulacion();
        simulacionCorriendo = true;
        pausado = false;
        actualizarEstadoBotones();
        cambiarPolitica(); // aplica la política del combo

        logEvento("▶ Simulación iniciada — Política: " + scheduler.getActivePolicy());

        // Hilo principal de simulación
        hiloSimulacion = new Thread(() -> {
            clock.startClock();

            while (simulacionCorriendo) {
                try {
                    while (pausado && simulacionCorriendo) Thread.sleep(80);
                    if (!simulacionCorriendo) break;

                    int vel = sliderVelocidad.getValue();
                    Thread.sleep(vel);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SwingUtilities.invokeLater(() -> {
                if (guiTimer != null) guiTimer.stop();
                logEvento("■ Simulación detenida.");
            });
        }, "HiloSimulacion");
        hiloSimulacion.setDaemon(true);
        hiloSimulacion.start();

        // Hilo de interrupciones aleatorias (hardware)
        hiloInterrupciones = new Thread(() -> {
            while (simulacionCorriendo) {
                try {
                    Thread.sleep(rng.nextInt(20000) + 15000);
                    if (!simulacionCorriendo) break;

                    String[] eventos = {"MICRO-METEORITO", "TORMENTA-SOLAR", "FALLO-SENSOR", "SOBRECARGA-TERMICA"};
                    String ev = eventos[rng.nextInt(eventos.length)];

                    PCB isr = new PCB("ISR_" + ev, rng.nextInt(5) + 2,
                        InstructionType.CPU, 1,
                        clock.getCurrentCycle() + 8,
                        ProcessType.APERIODIC, clock.getCurrentCycle());
                    scheduler.admitProcess(isr);
                    logEvento("☢ INTERRUPCIÓN HARDWARE: " + ev + " → ISR inyectada con prioridad 1");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "HiloInterrupciones");
        hiloInterrupciones.setDaemon(true);
        hiloInterrupciones.start();

        // Inicia el Timer de GUI (refresco cada 250ms, sin bloquear)
        iniciarTimerGUI();
    }

    // ═══════════════════════════════════════════════════════════
    //  LIMPIEZA (patrón limpiarSistemaParaNuevaSimulacion)
    // ═══════════════════════════════════════════════════════════

    private void limpiarSistemaParaNuevaSimulacion() {
        if (guiTimer != null && guiTimer.isRunning()) guiTimer.stop();

        clock.pauseClock();
        clock.resetClock();
        scheduler.reset();

        SwingUtilities.invokeLater(() -> {
            limpiarPanel(panelContenedorListos);
            limpiarPanel(panelContenedorBloqueados);
            limpiarPanel(panelContenedorListosSusp);
            limpiarPanel(panelContenedorBloqSusp);
            limpiarPanel(panelContenedorTerminados);
            lblReloj.setText("MISSION CLOCK: Cycle 0000");
            lblModo.setText("[ STANDBY ]"); lblModo.setForeground(Color.YELLOW);
            lblNombreCPU.setText("IDLE");
        });
    }

    private void limpiarPanel(JPanel panel) {
        if (panel == null) return;
        panel.removeAll();
        panel.revalidate();
        panel.repaint();
    }

    // ═══════════════════════════════════════════════════════════
    //  ACCIONES DE BOTONES
    // ═══════════════════════════════════════════════════════════

    private void togglePausa() {
        pausado = !pausado;
        if (pausado) {
            clock.pauseClock();
            btnPausar.setText("▶  REANUDAR");
            logEvento("⏸ Simulación pausada.");
        } else {
            clock.resumeClock();
            btnPausar.setText("⏸  PAUSAR");
            logEvento("▶ Simulación reanudada.");
        }
    }

    private void resetearSistema() {
        if (JOptionPane.showConfirmDialog(this, "¿Resetear toda la simulación?",
                "Confirmar Reset", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        simulacionCorriendo = false;
        pausado = false;
        if (hiloSimulacion != null)     hiloSimulacion.interrupt();
        if (hiloInterrupciones != null) hiloInterrupciones.interrupt();
        if (guiTimer != null)           guiTimer.stop();

        limpiarSistemaParaNuevaSimulacion();
        barraDeadline.setValue(0); barraDeadline.setString("Deadline: —");
        btnPausar.setText("⏸  PAUSAR");
        actualizarEstadoBotones();
        logEvento("↺ Sistema reseteado.");
    }

    private void cambiarPolitica() {
        String sel = (String) cmbAlgoritmos.getSelectedItem();
        SchedulerManager.Policy p;
        switch (sel) {
            case "Round Robin": p = SchedulerManager.Policy.ROUND_ROBIN; break;
            case "SRT":         p = SchedulerManager.Policy.SRT;         break;
            case "PEP":         p = SchedulerManager.Policy.PEP;         break;
            case "EDF":         p = SchedulerManager.Policy.EDF;         break;
            default:            p = SchedulerManager.Policy.FCFS;
        }
        scheduler.setPolicy(p);
        logEvento("⚙ Política → " + p.name());
    }

    private void generarProcesosAleatorios(int cantidad) {
        String[] nombres = {
            "P_Altitud","P_Temperatura","P_Camara","P_Telemetria","P_Downlink",
            "P_Actitud","P_Bateria","P_Sensores","P_Orbita","P_Com",
            "P_GPS","P_Propulsion","P_Radar","P_Solar","P_Ciencia","P_Monitoreo"
        };
        for (int i = 0; i < cantidad; i++) {
            String name  = nombres[rng.nextInt(nombres.length)] + "_" + rng.nextInt(100);
            int instr    = rng.nextInt(20) + 5;
            int prio     = rng.nextInt(5) + 1;
            int dl       = rng.nextInt(40) + 20;
            boolean io   = rng.nextBoolean();
            PCB pcb = new PCB(name, instr,
                io ? InstructionType.IO : InstructionType.CPU,
                prio, dl,
                rng.nextBoolean() ? ProcessType.PERIODIC : ProcessType.APERIODIC,
                clock.getCurrentCycle());
            if (io) {
                pcb.setCyclesUntilIOException(rng.nextInt(4) + 2);
                pcb.setCyclesForIOCompletion(rng.nextInt(3) + 1);
            }
            scheduler.admitProcess(pcb);
        }
        logEvento("＋ " + cantidad + " proceso(s) generado(s).");
    }

    private void generarEmergencia() {
        PCB em = new PCB(
            "ISR_EMERG_" + clock.getCurrentCycle(),
            rng.nextInt(6) + 2, InstructionType.CPU,
            1,
            clock.getCurrentCycle() + rng.nextInt(8) + 4,
            ProcessType.APERIODIC, clock.getCurrentCycle());
        scheduler.admitProcess(em);
        actualizarTodosPaneles();
        logEvento("☢ EMERGENCIA manual: " + em.getProcessName() + " inyectada (prioridad 1).");
    }

    private void cargarCSV() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
            String linea; int ok = 0;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] d = linea.split(",");
                if (d.length >= 4) {
                    try {
                        PCB pcb = new PCB(d[0].trim(), Integer.parseInt(d[2].trim()),
                            InstructionType.CPU, Integer.parseInt(d[1].trim()),
                            clock.getCurrentCycle() + Integer.parseInt(d[3].trim()),
                            ProcessType.APERIODIC, clock.getCurrentCycle());
                        scheduler.admitProcess(pcb);
                        ok++;
                    } catch (NumberFormatException ignored) {
                        logEvento("  Línea ignorada: " + linea);
                    }
                }
            }
            actualizarTodosPaneles();
            logEvento("📂 CSV cargado: " + ok + " procesos admitidos.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LOG ESTÁTICO (accesible desde cualquier clase del proyecto)
    // ═══════════════════════════════════════════════════════════

    /**
     * Escribe un mensaje en el log de eventos de la ventana.
     * Es estático: cualquier clase puede llamar Ventana.logEvento("...").
     */
    public static void logEvento(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            if (consolaEventos != null) {
                consolaEventos.append(mensaje + "\n");
                consolaEventos.setCaretPosition(consolaEventos.getDocument().getLength());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS VISUALES
    // ═══════════════════════════════════════════════════════════

    /**
     * Crea un JScrollPane con scroll HORIZONTAL para mostrar tarjetas de proceso.
     * Equivalente a configurarUnPanelConScroll de la guía.
     */
    private JScrollPane configurarPanelCola(String titulo, Color colorTitulo, boolean horizontal) {
        JPanel panelInterno = new JPanel();
        panelInterno.setLayout(new BoxLayout(panelInterno, BoxLayout.X_AXIS));
        panelInterno.setBackground(C_PANEL);
        panelInterno.setOpaque(true);

        JScrollPane sp = new JScrollPane(panelInterno);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(350, 160));
        sp.setBackground(C_FONDO);
        sp.getViewport().setBackground(C_PANEL);
        sp.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(colorTitulo, 1),
            "  " + titulo + "  ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 11), colorTitulo));

        // Guardamos referencia al panel interno con client property
        sp.putClientProperty("panelInterno", panelInterno);
        return sp;
    }

    /** Extrae el panel interno de un ScrollPane creado con configurarPanelCola */
    private JPanel extraerPanel(JScrollPane sp) {
        return (JPanel) sp.getClientProperty("panelInterno");
    }

    private JLabel label(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(C_TEXTO);
        l.setFont(new Font("Monospaced", Font.BOLD, 12));
        return l;
    }

    private JLabel valorCPU(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return l;
    }

    private JButton boton(String txt, Color color) {
        JButton b = new JButton(txt);
        b.setBackground(color.darker().darker());
        b.setForeground(color);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(color, 1));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(color.darker().darker()); }
        });
        return b;
    }

    private JSpinner spinnerEstilizado(int val, int min, int max, int step) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, step));
        sp.setPreferredSize(new Dimension(65, 24));
        if (sp.getEditor() instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) sp.getEditor()).getTextField();
            tf.setBackground(C_CARD);
            tf.setForeground(C_BORDE);
            tf.setFont(new Font("Monospaced", Font.BOLD, 12));
        }
        return sp;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 416, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // ===================== MAIN =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Ventana();
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
