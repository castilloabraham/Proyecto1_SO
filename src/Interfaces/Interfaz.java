/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;



import Estructuras.Cola;
import Estructuras.Lista;
import Estructuras.Nodo;
import Process.PCB;
import Process.ProcessState;
import Process.InstructionType;
import Process.ProcessType;
import Process.ProcessIDGenerator;
import Politicas.SchedulerManager;
import clock.SystemClock;
import clock.ResourceManager;

import java.util.Random;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

// JFreeChart imports
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author 
 */


public class Interfaz extends javax.swing.JFrame {
    
    
    // =====================================================================
    // ESTADO DE LA SIMULACIÓN
    // =====================================================================
    private volatile boolean simulacionCorriendo = false;

    // =====================================================================
    // BACKEND — INSTANCIAS PRINCIPALES
    // =====================================================================
    /** Reloj global del sistema (Singleton). */
    public static SystemClock systemClock;

    /** Gestor de políticas de planificación. */
    public static SchedulerManager schedulerManager;

    /** Cola de procesos nuevos (esperando admisión). */
    public static final Cola<PCB> colaNuevos = new Cola<>();

    /** Semáforo propio para proteger colaNuevos. */
    public static final java.util.concurrent.Semaphore semNuevos =
            new java.util.concurrent.Semaphore(1, true);

    // =====================================================================
    // GRÁFICO
    // =====================================================================
    public static final XYSeries seriesUtilizacion = new XYSeries("Utilización CPU");

    // =====================================================================
    // COMPONENTES VISUALES — PANELES INTERNOS (contenedores de tarjetas)
    // =====================================================================
    private JPanel panelContenedorListos;
    private JPanel panelContenedorBloqueados;
    private JPanel panelContenedorListosSusp;
    private JPanel panelContenedorBloqueadosSusp;
    private JPanel panelContenedorTerminados;

    // Log de eventos
    private static JTextArea consolaDeEventos;

    // Timer de refresco de GUI
    private Timer guiTimer;

    /**
     * Creates new form Interfaz
     */
    public Interfaz() {
        initComponents();
        configurarPanelesDeColasVisualmente();
        iniciarGrafico();
        configurarPanelDeEventos();

        // Conectar slider de velocidad
        cycleDurationSlider.addChangeListener(e -> {
            if (!cycleDurationSlider.getValueIsAdjusting()) {
                int ms = cycleDurationSlider.getValue();
                if (systemClock != null) {
                    systemClock.setCycleDuration(ms);
                }
                logEvento("GUI: Velocidad de ciclo cambiada a " + ms + " ms.");
            }
        });

        actualizarEstadoBotones();
    }
    
   
    private void limpiarSistemaParaNuevaSimulacion() {
    System.out.println("GUI: Limpiando sistema anterior...");

        // Detener reloj anterior si existe
        if (systemClock != null && systemClock.isRunning()) {
            systemClock.stopClock();
            SystemClock.resetInstance();
        }

        // Limpiar cola de nuevos
        try {
            if (semNuevos.tryAcquire(200, TimeUnit.MILLISECONDS)) {
                try { while (!colaNuevos.estaVacia()) colaNuevos.desencolar(); }
                finally { semNuevos.release(); }
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Limpiar gráfico
        seriesUtilizacion.clear();

        // Resetar ProcessIDGenerator
        ProcessIDGenerator.getInstance().reset();

        // Resetar la GUI
        TextRelojGlobal.setText("Reloj Global: 0");
        if (consolaDeEventos != null) consolaDeEventos.setText("");
        actualizarEstadoBotones();

        System.out.println("GUI: Sistema limpio.");
}
    
    private void configurarPanelDeEventos() {
    consolaDeEventos = new JTextArea();
        consolaDeEventos.setEditable(false);
        consolaDeEventos.setLineWrap(true);
        consolaDeEventos.setWrapStyleWord(true);
        consolaDeEventos.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollLog = new JScrollPane(consolaDeEventos);
        scrollLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollLog.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        PanelTerminadosEventos.setLayout(new BorderLayout());
        PanelTerminadosEventos.add(scrollLog, BorderLayout.CENTER);
        PanelTerminadosEventos.validate();
    }
    
    public static void logEvento(String mensaje) {
        long ciclo = (systemClock != null) ? systemClock.getCurrentCycle() : 0;
        final String texto = "[" + ciclo + "] " + mensaje + "\n";
        SwingUtilities.invokeLater(() -> {
            if (consolaDeEventos != null) {
                consolaDeEventos.append(texto);
                consolaDeEventos.setCaretPosition(consolaDeEventos.getDocument().getLength());
            } else {
                System.out.print("LOG: " + texto);
            }
        });
    }
    
    private void limpiarCola(Cola<?> cola, SimpleSemaphore sem) {
    try {
        if (sem.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            try {
                while (!cola.isEmpty()) {
                    cola.pop();
                }
            } finally {
                sem.release();
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("Error al limpiar cola: " + e.getMessage());
    }
}
    
    
    
    private void actualizarEstadoBotones() {
    
        boolean hayProcesosNuevos;
        try {
            semNuevos.acquire();
            try { hayProcesosNuevos = !colaNuevos.estaVacia(); }
            finally { semNuevos.release(); }
        } catch (InterruptedException e) { hayProcesosNuevos = false; }

        BotonCrearProceso.setEnabled(!simulacionCorriendo);
        Boton20.setEnabled(!simulacionCorriendo);
        jComboAlgoritmos.setEnabled(!simulacionCorriendo);
        BotonIniciar.setEnabled(!simulacionCorriendo && hayProcesosNuevos);
        BotonCambiarAlgoritmo.setEnabled(simulacionCorriendo);
        cycleDurationSlider.setEnabled(true);
}
    
    private void actualizarPanelesDeColas() {
        if (schedulerManager == null) return;

        // Obtener el scheduler activo y sus listas
        SchedulerManager.Policy policy = schedulerManager.getActivePolicy();

        // Cola de listos
        Lista<PCB> listos = getReadySnapshot(policy);
        // Cola de bloqueados
        Lista<PCB> bloqueados = getBlockedSnapshot(policy);
        // Suspendidos (del MTS)
        Lista<PCB> listosSusp   = schedulerManager.getMTS().getReadySuspendedList();
        Lista<PCB> bloqueadosSusp = schedulerManager.getMTS().getBlockedSuspendedList();
        // Terminados — acumulados en el scheduler
        Lista<PCB> terminados = getTerminatedSnapshot(policy);

        actualizarUnPanel(panelContenedorListos,          listos);
        actualizarUnPanel(panelContenedorBloqueados,      bloqueados);
        actualizarUnPanel(panelContenedorListosSusp,      listosSusp);
        actualizarUnPanel(panelContenedorBloqueadosSusp,  bloqueadosSusp);
        actualizarUnPanel(panelContenedorTerminados,      terminados);
    
    }
    
    /** Obtiene snapshot de la cola de listos según la política activa. */
    private Lista<PCB> getReadySnapshot(SchedulerManager.Policy policy) {
        Lista<PCB> snap = new Lista<>();
        try {
            clock.ClockListener sched = schedulerManager.getActiveScheduler();
            Lista<PCB> src = null;
            switch (policy) {
                case SRT: src = ((Politicas.SRT) sched).getReadyList();  break;
                case PEP: src = ((Politicas.PEP) sched).getReadyList();  break;
                case EDF: src = ((Politicas.EDF) sched).getReadyList();  break;
                // FCFS y RR usan Cola, la recorremos diferente
                default: break;
            }
            if (src != null) {
                Nodo<PCB> n = src.getCabeza();
                while (n != null) { snap.agregarAlFinal(n.getDato()); n = n.getSiguiente(); }
            } else if (policy == SchedulerManager.Policy.FCFS) {
                // Para FCFS usamos reflexión o exponemos vía getReadyQueueSize — solo mostramos contador
                // (Si deseas visibilidad completa, añade getReadyList() a FCFS)
            }
        } catch (Exception ignored) {}
        return snap;
    }
    
    /** Obtiene snapshot de la cola de bloqueados según la política activa. */
    private Lista<PCB> getBlockedSnapshot(SchedulerManager.Policy policy) {
        Lista<PCB> snap = new Lista<>();
        try {
            clock.ClockListener sched = schedulerManager.getActiveScheduler();
            Lista<PCB> src = null;
            switch (policy) {
                case FCFS:        src = ((Politicas.FCFS)       sched).getBlockedList(); break;
                case ROUND_ROBIN: src = ((Politicas.RoundRobin) sched).getBlockedList(); break;
                case SRT:         src = ((Politicas.SRT)        sched).getBlockedList(); break;
                case PEP:         src = ((Politicas.PEP)        sched).getBlockedList(); break;
                case EDF:         src = ((Politicas.EDF)        sched).getBlockedList(); break;
            }
            if (src != null) {
                Nodo<PCB> n = src.getCabeza();
                while (n != null) { snap.agregarAlFinal(n.getDato()); n = n.getSiguiente(); }
            }
        } catch (Exception ignored) {}
        return snap;
    }
    
    /** Obtiene snapshot de la lista de terminados según la política activa. */
    private Lista<PCB> getTerminatedSnapshot(SchedulerManager.Policy policy) {
        Lista<PCB> snap = new Lista<>();
        try {
            clock.ClockListener sched = schedulerManager.getActiveScheduler();
            Lista<PCB> src = null;
            switch (policy) {
                case FCFS:        src = ((Politicas.FCFS)       sched).getTerminatedList(); break;
                case ROUND_ROBIN: src = ((Politicas.RoundRobin) sched).getTerminatedList(); break;
                case SRT:         src = ((Politicas.SRT)        sched).getTerminatedList(); break;
                case PEP:         src = ((Politicas.PEP)        sched).getTerminatedList(); break;
                case EDF:         src = ((Politicas.EDF)        sched).getTerminatedList(); break;
            }
            if (src != null) {
                Nodo<PCB> n = src.getCabeza();
                while (n != null) { snap.agregarAlFinal(n.getDato()); n = n.getSiguiente(); }
            }
        } catch (Exception ignored) {}
        return snap;
    }
    
    /**
     * Inicia el Timer de la GUI que refrescará los paneles y el reloj global.
     * Este método debe ser llamado por el ActionListener del botón "Iniciar".
     */
     /**
     * Inicia el Timer de la GUI que refrescará los paneles de colas y CPU
     * periódicamente (cada 250ms).
     * LLAMADO POR BotonIniciarActionPerformed.
     */
    
    
    private void iniciarTimerGUI() {
        if (guiTimer != null && guiTimer.isRunning()) guiTimer.stop();

        guiTimer = new Timer(250, e -> {
            if (systemClock != null) {
                final String textoReloj = "Reloj Global: " + systemClock.getCurrentCycle();
                SwingUtilities.invokeLater(() -> TextRelojGlobal.setText(textoReloj));
            }
            actualizarPanelesDeColas();
            actualizarPanelCPU();
            PanelRendimiento.repaint();
        });
        guiTimer.setInitialDelay(100);
        guiTimer.setRepeats(true);
        guiTimer.start();
    }
    
    private void iniciarGrafico() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesUtilizacion);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Utilización de CPU vs. Tiempo",
                "Tiempo (Ciclos)", "Utilización (%)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0.0, 100.0);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setPreferredSize(new Dimension(300, 200));

        Dimension sizeOriginal = PanelRendimiento.getPreferredSize();
        PanelRendimiento.setLayout(new BorderLayout());
        PanelRendimiento.add(chartPanel, BorderLayout.CENTER);
        if (sizeOriginal != null && sizeOriginal.width > 0) {
            PanelRendimiento.setPreferredSize(sizeOriginal);
            PanelRendimiento.setMaximumSize(sizeOriginal);
            PanelRendimiento.setMinimumSize(sizeOriginal);
        }
        PanelRendimiento.validate();
    }
    
    
    private void configurarPanelesDeColasVisualmente() {
        configurarUnPanelConScroll(PanelListos, "panelContenedorListos");
        configurarUnPanelConScroll(PanelBloqueados, "panelContenedorBloqueados");
        configurarUnPanelConScroll(PanelListos_Suspendidos, "panelContenedorListosSusp");
        configurarUnPanelConScroll(PanelBloqueados_Suspendidos, "panelContenedorBloqueadosSusp");
        configurarUnPanelConScroll(PanelTerminados, "panelContenedorTerminados");
    }
    
    
    private void configurarUnPanelConScroll(JPanel panelExternoExistente, String nombreVariablePanelInterno) {
        panelExterno.setLayout(new BorderLayout());
        panelExterno.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel panelInterno = new JPanel();
        panelInterno.setLayout(new BoxLayout(panelInterno, BoxLayout.X_AXIS));
        panelInterno.setBackground(Color.WHITE);
        panelInterno.setOpaque(true);

        switch (nombreVariable) {
            case "panelContenedorListos":         panelContenedorListos = panelInterno;         break;
            case "panelContenedorBloqueados":     panelContenedorBloqueados = panelInterno;     break;
            case "panelContenedorListosSusp":     panelContenedorListosSusp = panelInterno;     break;
            case "panelContenedorBloqueadosSusp": panelContenedorBloqueadosSusp = panelInterno; break;
            case "panelContenedorTerminados":     panelContenedorTerminados = panelInterno;     break;
        }

        JScrollPane scroll = new JScrollPane(panelInterno);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);

        panelExterno.add(scroll, BorderLayout.CENTER);
        panelExterno.revalidate();
        panelExterno.repaint();
    }
    
    
    /**
     * MÉTODO CLAVE: Lee una cola del backend (protegida por semáforo)
     * y crea TARJETAS (PanelProcesoVista) en el panel interno correspondiente. <--- CAMBIO
     */
    private void actualizarUnPanel(JPanel panelInterno, Cola<Process> cola, SimpleSemaphore sem) {
        if (panelInterno == null) return;
        panelInterno.removeAll();

        if (procesos == null || procesos.estaVacia()) {
            panelInterno.add(Box.createHorizontalGlue());
            panelInterno.add(new JLabel(" Vacío "));
            panelInterno.add(Box.createHorizontalGlue());
        } else {
            panelInterno.add(Box.createHorizontalStrut(5));
            Nodo<PCB> nodo = procesos.getCabeza();
            while (nodo != null) {
                PCB p = nodo.getDato();
                JPanel tarjeta = crearTarjetaPCB(p);
                panelInterno.add(tarjeta);
                panelInterno.add(Box.createHorizontalStrut(5));
                nodo = nodo.getSiguiente();
            }
        }

        panelInterno.revalidate();
        panelInterno.repaint();
    }
    
    /**
     * Crea una tarjeta visual pequeña para un PCB.
     * Muestra: ID, Nombre, Estado, PC, Deadline restante.
     */
    private JPanel crearTarjetaPCB(PCB p) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(colorEstado(p.getCurrentState()));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        card.setPreferredSize(new Dimension(120, 100));
        card.setMaximumSize(new Dimension(120, 100));

        // Etiquetas
        card.add(label("ID: " + p.getProgramCounter(), Font.BOLD, 10));
        card.add(label(p.getProcessName(), Font.BOLD, 11));
        card.add(label(p.getCurrentState().getDisplayName(), Font.PLAIN, 10));
        card.add(label("PC: " + p.getProgramCounter(), Font.PLAIN, 10));
        card.add(label("DL: " + p.getRemainingDeadline(), Font.PLAIN, 10));

        return card;
    }
    
    private void actualizarPanelListos() {
        // (Este método parece ser una versión antigua de actualizarUnPanel)
        // (Se recomienda usar actualizarUnPanel en su lugar)
        panelContenedorListos.removeAll();
        Nodo<ProccesFabrication.Process> actual = colaListos.getpFirst();
        if (actual == null) {
            panelContenedorListos.add(new JLabel(" (Cola de Listos Vacía) "));
        } else {
            while (actual != null) {
                ProccesFabrication.Process p = actual.getData();
                PanelProcesoVista nuevaTarjeta = new PanelProcesoVista();
                nuevaTarjeta.actualizarDatos(p);
                panelContenedorListos.add(nuevaTarjeta);
                panelContenedorListos.add(Box.createHorizontalStrut(5));
                actual = actual.getPnext();
            }
        }
        panelContenedorListos.revalidate();
        panelContenedorListos.repaint();
    }
    
    private JLabel label(String text, int style, int size) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", style, size));
        lbl.setForeground(Color.BLACK);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
    
    /** Color de fondo de tarjeta según estado. */
    private Color colorEstado(ProcessState estado) {
        switch (estado) {
            case READY:             return new Color(200, 255, 200);
            case RUNNING:           return new Color(255, 255, 150);
            case BLOCKED:           return new Color(255, 200, 200);
            case READY_SUSPENDED:   return new Color(200, 230, 255);
            case BLOCKED_SUSPENDED: return new Color(230, 200, 255);
            case TERMINATED:        return new Color(220, 220, 220);
            default:                return Color.WHITE;
        }
    }

    
    /**
     * Selecciona y devuelve la instancia del algoritmo de planificación
     * basado en la selección del JComboBox.
     */
     private SchedulerManager.Policy seleccionarPolitica(String nombre) {
        switch (nombre) {
            case "First-Come, First-Served":   return SchedulerManager.Policy.FCFS;
            case "Round Robin":                return SchedulerManager.Policy.ROUND_ROBIN;
            case "Shortest Remaining Time":    return SchedulerManager.Policy.SRT;
            case "Prioridad Estática (PEP)":   return SchedulerManager.Policy.PEP;
            case "Earliest Deadline First":    return SchedulerManager.Policy.EDF;
            default:
                JOptionPane.showMessageDialog(this, "Algoritmo no reconocido: " + nombre);
                return SchedulerManager.Policy.FCFS;
        }
    }
    
    /**
     * Actualiza los JLabels del panel de ejecución de CPU
     * basado en la variable global Interfaz.procesoEnCPU.
     * LLAMADO POR EL TIMER.
     */
    private void actualizarPanelCPU() {
        PCB p = (schedulerManager != null) ? schedulerManager.getRunningProcess() : null;

        if (p != null) {
            lblNombreProcesoCPU.setText(p.getProcessName());
            lblIdProcesoCPU.setText(String.valueOf(p.getProgramCounter()));
            lblPcProcesoCPU.setText(String.valueOf(p.getProgramCounter()));
            lblMarProcesoCPU.setText(String.valueOf(p.getMemoryAddressRegister()));
            lblStatusProcesoCPU.setText(p.getCurrentState().getDisplayName());
            lblTipoProcesoCPU.setText(
                    p.getInstructionType() == InstructionType.IO ? "I/O Bound" : "CPU Bound");
        } else {
            lblNombreProcesoCPU.setText("Sistema (Idle)");
            lblIdProcesoCPU.setText("N/A");
            lblPcProcesoCPU.setText("N/A");
            lblMarProcesoCPU.setText("N/A");
            lblStatusProcesoCPU.setText("N/A");
            lblTipoProcesoCPU.setText("N/A");
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        TextRelojGlobal = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        BotonIniciar = new javax.swing.JButton();
        jComboAlgoritmos = new javax.swing.JComboBox<>();
        jLabel17 = new javax.swing.JLabel();
        Boton20 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        BotonCrearProceso = new javax.swing.JButton();
        btnVerNuevos = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        PanelBloqueados_Suspendidos = new javax.swing.JPanel();
        PanelBloqueados = new javax.swing.JPanel();
        PanelListos_Suspendidos = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        PanelListos = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        PanelProcesoEjecucion = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblNombreProcesoCPU = new javax.swing.JLabel();
        lblIdProcesoCPU = new javax.swing.JLabel();
        lblPcProcesoCPU = new javax.swing.JLabel();
        lblMarProcesoCPU = new javax.swing.JLabel();
        lblStatusProcesoCPU = new javax.swing.JLabel();
        lblTipoProcesoCPU = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        PanelTerminadosEventos = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        PanelRendimiento = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        btnVerMetricas = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        BotonCambiarAlgoritmo = new javax.swing.JButton();
        jLabel28 = new javax.swing.JLabel();
        cycleDurationSlider = new javax.swing.JSlider();
        jPanel16 = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        BotonEscribir = new javax.swing.JButton();
        PanelTerminados = new javax.swing.JPanel();
        jLabel29 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel7.setBackground(new java.awt.Color(204, 102, 0));
        jPanel7.setBorder(new javax.swing.border.MatteBorder(null));

        TextRelojGlobal.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 14)); // NOI18N
        TextRelojGlobal.setForeground(new java.awt.Color(255, 255, 255));
        TextRelojGlobal.setText("Reloj Global: ");

        jLabel12.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Algoritmo:");

        BotonIniciar.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonIniciar.setText("Iniciar");
        BotonIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonIniciarActionPerformed(evt);
            }
        });

        jComboAlgoritmos.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jComboAlgoritmos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "First-Come, First-Served", "Round Robin", "Shortest Process Next", "Shortest Remaining Time", "Highest Response-Ratio Next", "Feedback" }));
        jComboAlgoritmos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboAlgoritmosActionPerformed(evt);
            }
        });

        jLabel17.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("Control y Estado del Sistema");

        Boton20.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        Boton20.setText("Crear 20 Procesos Automaticos");
        Boton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Boton20ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap(32, Short.MAX_VALUE)
                .addComponent(Boton20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(BotonIniciar, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addComponent(TextRelojGlobal, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboAlgoritmos, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                    .addContainerGap(201, Short.MAX_VALUE)
                    .addComponent(jLabel17)
                    .addGap(152, 152, 152)))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboAlgoritmos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(TextRelojGlobal, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(BotonIniciar)
                            .addComponent(Boton20))))
                .addContainerGap(107, Short.MAX_VALUE))
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup()
                    .addGap(16, 16, 16)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(192, Short.MAX_VALUE)))
        );

        jPanel1.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 610, 260));

        jPanel2.setBackground(new java.awt.Color(0, 204, 51));
        jPanel2.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel13.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Creación de Proceso");

        BotonCrearProceso.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonCrearProceso.setText("Crear");
        BotonCrearProceso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonCrearProcesoActionPerformed(evt);
            }
        });

        btnVerNuevos.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        btnVerNuevos.setText("Ver Cola de Nuevos");
        btnVerNuevos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerNuevosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(87, 87, 87)
                        .addComponent(BotonCrearProceso, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnVerNuevos, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))))
                .addContainerGap(38, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(BotonCrearProceso, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnVerNuevos, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 10, 270, 140));

        jPanel3.setBackground(new java.awt.Color(0, 102, 204));
        jPanel3.setBorder(new javax.swing.border.MatteBorder(null));

        PanelBloqueados_Suspendidos.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelBloqueados_Suspendidos.setLayout(new java.awt.BorderLayout());

        PanelBloqueados.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelBloqueados.setLayout(new java.awt.BorderLayout());

        PanelListos_Suspendidos.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelListos_Suspendidos.setLayout(new java.awt.BorderLayout());

        jLabel16.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("Bloqueados");

        PanelListos.setLayout(new java.awt.BorderLayout());

        jLabel22.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(255, 255, 255));
        jLabel22.setText("Visualización de Colas");

        jLabel23.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(255, 255, 255));
        jLabel23.setText("Listos");

        jLabel25.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(255, 255, 255));
        jLabel25.setText("Listos/Suspendidos");

        jLabel26.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(255, 255, 255));
        jLabel26.setText("Bloqueados/Suspendidos");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(119, 119, 119)
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel16)
                .addGap(132, 132, 132))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(75, 75, 75)
                                .addComponent(jLabel25))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(PanelListos, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(PanelListos_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(48, 48, 48)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addComponent(PanelBloqueados_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(PanelBloqueados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(196, 196, 196)
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jLabel23))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel16)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(PanelBloqueados, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PanelListos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(PanelBloqueados_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(PanelListos_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 270, 610, 460));

        jPanel9.setBackground(new java.awt.Color(204, 0, 204));
        jPanel9.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel18.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("<html><center>Cpu y Proceso en Ejecucion</center></html>");
        jLabel18.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel1.setFont(new java.awt.Font("Segoe UI Emoji", 1, 14)); // NOI18N
        jLabel1.setText("CPU");

        jLabel2.setText("Proceso en Ejecucion:");

        jLabel3.setText("ID:");

        jLabel4.setText("PC:");

        jLabel5.setText("Status:");

        jLabel6.setText("MAR:");

        jLabel7.setText("Tipo:");

        lblNombreProcesoCPU.setText("N/A");

        lblIdProcesoCPU.setText("N/A");

        lblPcProcesoCPU.setText("N/A");

        lblMarProcesoCPU.setText("N/A");

        lblStatusProcesoCPU.setText("N/A");

        lblTipoProcesoCPU.setText("N/A");

        javax.swing.GroupLayout PanelProcesoEjecucionLayout = new javax.swing.GroupLayout(PanelProcesoEjecucion);
        PanelProcesoEjecucion.setLayout(PanelProcesoEjecucionLayout);
        PanelProcesoEjecucionLayout.setHorizontalGroup(
            PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblNombreProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTipoProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                        .addGap(8, 8, 8))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblIdProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblPcProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMarProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblStatusProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(91, 91, 91))
        );
        PanelProcesoEjecucionLayout.setVerticalGroup(
            PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblNombreProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblIdProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPcProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMarProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblStatusProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTipoProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9))
        );

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PanelProcesoEjecucion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PanelProcesoEjecucion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 150, 270, 300));

        jPanel11.setBackground(new java.awt.Color(102, 102, 102));
        jPanel11.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel19.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("<html><cenater>Log de Eventos</center></html>");
        jLabel19.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        PanelTerminadosEventos.setPreferredSize(new java.awt.Dimension(223, 174));

        javax.swing.GroupLayout PanelTerminadosEventosLayout = new javax.swing.GroupLayout(PanelTerminadosEventos);
        PanelTerminadosEventos.setLayout(PanelTerminadosEventosLayout);
        PanelTerminadosEventosLayout.setHorizontalGroup(
            PanelTerminadosEventosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 197, Short.MAX_VALUE)
        );
        PanelTerminadosEventosLayout.setVerticalGroup(
            PanelTerminadosEventosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 188, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel19)
                    .addComponent(PanelTerminadosEventos, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE))
                .addGap(36, 36, 36))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(PanelTerminadosEventos, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jLabel19.getAccessibleContext().setAccessibleDescription("");

        jPanel1.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 450, 270, 280));

        jPanel13.setBackground(new java.awt.Color(0, 153, 153));
        jPanel13.setBorder(new javax.swing.border.MatteBorder(null));

        javax.swing.GroupLayout PanelRendimientoLayout = new javax.swing.GroupLayout(PanelRendimiento);
        PanelRendimiento.setLayout(PanelRendimientoLayout);
        PanelRendimientoLayout.setHorizontalGroup(
            PanelRendimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 359, Short.MAX_VALUE)
        );
        PanelRendimientoLayout.setVerticalGroup(
            PanelRendimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 222, Short.MAX_VALUE)
        );

        jLabel21.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel21.setText("<html><center>Grafico de Rendimiento</center></html>");
        jLabel21.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        btnVerMetricas.setText("Ver Metricas");
        btnVerMetricas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerMetricasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addGap(72, 72, 72)
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnVerMetricas)
                            .addComponent(PanelRendimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(49, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(PanelRendimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnVerMetricas)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 150, 450, 330));

        jPanel15.setBackground(new java.awt.Color(153, 153, 255));
        jPanel15.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel20.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel20.setText("<html><center>Tiempo de Ejecucion en tiempo real</center></html>");
        jLabel20.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel27.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(255, 255, 255));
        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel27.setText("<html><center>Cambio de ciclo de ejecucion</center></html>");
        jLabel27.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        BotonCambiarAlgoritmo.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonCambiarAlgoritmo.setText("Cambiar");
        BotonCambiarAlgoritmo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonCambiarAlgoritmoActionPerformed(evt);
            }
        });

        jLabel28.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(255, 255, 255));
        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel28.setText("<html><center>Intercambiar Algoritmo</center></html>");
        jLabel28.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        cycleDurationSlider.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 12)); // NOI18N
        cycleDurationSlider.setMajorTickSpacing(1000);
        cycleDurationSlider.setMaximum(5000);
        cycleDurationSlider.setMinimum(500);
        cycleDurationSlider.setMinorTickSpacing(500);
        cycleDurationSlider.setPaintLabels(true);
        cycleDurationSlider.setPaintTicks(true);
        cycleDurationSlider.setSnapToTicks(true);
        cycleDurationSlider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cycleDurationSlider.setEnabled(false);
        cycleDurationSlider.setOpaque(true);
        cycleDurationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cycleDurationSliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(64, 64, 64)
                .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(88, Short.MAX_VALUE))
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(BotonCambiarAlgoritmo)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(40, 40, 40))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                        .addComponent(cycleDurationSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20))))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BotonCambiarAlgoritmo)
                        .addGap(12, 12, 12))
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cycleDurationSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 10, 450, 140));

        jPanel16.setBackground(new java.awt.Color(204, 0, 153));
        jPanel16.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel24.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel24.setText("<html><center>Escritura en JSON/CSV</center></html>");
        jLabel24.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        BotonEscribir.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        BotonEscribir.setText("Escribir");
        BotonEscribir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonEscribirActionPerformed(evt);
            }
        });

        PanelTerminados.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelTerminados.setLayout(new java.awt.BorderLayout());

        jLabel29.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel29.setForeground(new java.awt.Color(255, 255, 255));
        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel29.setText("<html><cenater>Terminados</center></html>");
        jLabel29.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(PanelTerminados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(51, 51, 51))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                        .addComponent(BotonEscribir, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41))))
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BotonEscribir)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(PanelTerminados, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1.add(jPanel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 480, 450, 250));

        jTabbedPane1.addTab("Simulador", jPanel1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1388, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 746, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Estadisticas", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1388, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void mostrarMetricas() {
        if (schedulerManager == null) {
            JOptionPane.showMessageDialog(this, "La simulación no ha iniciado.");
            return;
        }

        int cicloActual = (systemClock != null) ? systemClock.getCurrentCycle() : 0;
        if (cicloActual == 0) {
            JOptionPane.showMessageDialog(this, "La simulación aún no ha avanzado.");
            return;
        }

        Lista<PCB> terminados = getTerminatedSnapshot(schedulerManager.getActivePolicy());
        int numTerminados = terminados.size();

        if (numTerminados == 0) {
            JOptionPane.showMessageDialog(this, "Aún no ha terminado ningún proceso.");
            return;
        }

        long tiempoOcupadoCPU = 0;
        long sumaRespuesta    = 0;
        Lista<Long> tiemposCPU = new Lista<>();

        Nodo<PCB> n = terminados.getCabeza();
        while (n != null) {
            PCB p = n.getDato();
            tiempoOcupadoCPU += p.getCpuTimeUsed();
            sumaRespuesta    += p.getWaitingTime();
            tiemposCPU.agregarAlFinal((long) p.getCpuTimeUsed());
            n = n.getSiguiente();
        }

        double throughput          = (double) numTerminados / cicloActual;
        double utilizacion         = (cicloActual > 0) ? ((double) tiempoOcupadoCPU / cicloActual) * 100 : 0;
        double tiempoRespProm      = (double) sumaRespuesta / numTerminados;
        double misionExito         = schedulerManager.getMissionSuccessRate();

        // Desviación estándar del tiempo de CPU (equidad)
        double media = (double) tiempoOcupadoCPU / numTerminados;
        double sumCuad = 0;
        Nodo<Long> lt = tiemposCPU.getCabeza();
        while (lt != null) { sumCuad += Math.pow(lt.getDato() - media, 2); lt = lt.getSiguiente(); }
        double desv = Math.sqrt(sumCuad / numTerminados);

        String msg = String.format(
            "Métricas de Rendimiento (%d procesos terminados):\n\n" +
            "Tiempo Total de Simulación : %d ciclos\n\n" +
            "1. Throughput              : %.4f proc/ciclo\n" +
            "2. Utilización del CPU     : %.2f %% (%d / %d ciclos)\n" +
            "3. Tiempo Espera Promedio  : %.2f ciclos\n" +
            "4. Tasa de Éxito (Deadline): %.1f %%\n" +
            "5. Equidad (Desv. Est.)    : %.2f ciclos\n\n" +
            "Swap-Outs : %d | Swap-Ins : %d",
            numTerminados, cicloActual,
            throughput,
            utilizacion, tiempoOcupadoCPU, cicloActual,
            tiempoRespProm,
            misionExito,
            desv,
            schedulerManager.getMTS().getSwapOutCount(),
            schedulerManager.getMTS().getSwapInCount()
        );

        JTextArea ta = new JTextArea(msg);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Métricas de Rendimiento", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void BotonCrearProcesoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonCrearProcesoActionPerformed
        // Abre el diálogo de creación de proceso
        // Si tienes CrearProcesoDialog, úsalo; si no, usamos JOptionPane directamente:
        crearProcesoManualmente();
        actualizarEstadoBotones();
    }//GEN-LAST:event_BotonCrearProcesoActionPerformed

    private void crearProcesoManualmente() {
        try {
            String nombre = JOptionPane.showInputDialog(this, "Nombre del proceso:", "Crear Proceso", JOptionPane.QUESTION_MESSAGE);
            if (nombre == null || nombre.trim().isEmpty()) return;

            String strInstr = JOptionPane.showInputDialog(this, "Número de instrucciones (ej: 20):", "10");
            if (strInstr == null) return;
            int instrucciones = Integer.parseInt(strInstr.trim());

            String strDeadline = JOptionPane.showInputDialog(this, "Deadline (ciclos, ej: 50):", "50");
            if (strDeadline == null) return;
            int deadline = Integer.parseInt(strDeadline.trim());

            String strPrioridad = JOptionPane.showInputDialog(this, "Prioridad (1=alta, ej: 3):", "3");
            if (strPrioridad == null) return;
            int prioridad = Integer.parseInt(strPrioridad.trim());

            String[] tiposProc = {"CPU (solo CPU)", "E/S (I/O Bound)"};
            int tipoIdx = JOptionPane.showOptionDialog(this, "Tipo de proceso:", "Tipo",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, tiposProc, tiposProc[0]);
            if (tipoIdx < 0) return;
            InstructionType tipo = (tipoIdx == 1) ? InstructionType.IO : InstructionType.CPU;

            int ciclosExc = 0, ciclosRes = 0;
            if (tipo == InstructionType.IO) {
                ciclosExc = Integer.parseInt(JOptionPane.showInputDialog(this, "Ciclos para generar E/S:", "5").trim());
                ciclosRes = Integer.parseInt(JOptionPane.showInputDialog(this, "Ciclos para resolver E/S:", "3").trim());
            }

            // Crear PCB
            PCB p = new PCB(nombre.trim(), instrucciones, tipo, prioridad, deadline,
                    ProcessType.APERIODIC, (systemClock != null) ? systemClock.getCurrentCycle() : 0);
            p.setCyclesUntilIOException(ciclosExc);
            p.setCyclesForIOCompletion(ciclosRes);

            semNuevos.acquire();
            try { colaNuevos.encolar(p); }
            finally { semNuevos.release(); }

            logEvento("NUEVO proceso creado: " + nombre + " | DL=" + deadline + " | Pri=" + prioridad);
            JOptionPane.showMessageDialog(this, "Proceso '" + nombre + "' creado y añadido a la cola de Nuevos.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error: ingresa sólo números válidos.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void BotonCambiarAlgoritmoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonCambiarAlgoritmoActionPerformed
        if (schedulerManager == null) {
            JOptionPane.showMessageDialog(this, "Debe iniciar la simulación primero.");
            return;
        }

        String[] opciones = {
            "First-Come, First-Served", "Round Robin",
            "Shortest Remaining Time", "Prioridad Estática (PEP)", "Earliest Deadline First"
        };
        String actual = (String) jComboAlgoritmos.getSelectedItem();
        Object sel = JOptionPane.showInputDialog(this,
                "Seleccione el nuevo algoritmo:", "Cambiar Algoritmo",
                JOptionPane.PLAIN_MESSAGE, null, opciones, actual);

        if (sel == null) return;
        String nuevo = (String) sel;
        SchedulerManager.Policy pol = seleccionarPolitica(nuevo);

        schedulerManager.setPolicy(pol);
        jComboAlgoritmos.setSelectedItem(nuevo);
        logEvento("ALGORITMO cambiado a: " + nuevo);
        JOptionPane.showMessageDialog(this, "Algoritmo cambiado a: " + nuevo);
    
    }//GEN-LAST:event_BotonCambiarAlgoritmoActionPerformed

    private void btnVerNuevosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerNuevosActionPerformed
        StringBuilder sb = new StringBuilder("Cola de Nuevos (sin admitir):\n\n");
        try {
            semNuevos.acquire();
            try {
                if (colaNuevos.estaVacia()) {
                    sb.append("  (vacía)");
                } else {
                    // Cola no es recorrible sin desencolar; usamos una Lista espejo
                    // construida durante encolar (ver crearProcesoManualmente / Boton20)
                    sb.append("  Hay " + colaNuevos.size() + " procesos esperando admisión.");
                }
            } finally { semNuevos.release(); }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        JOptionPane.showMessageDialog(this, sb.toString(), "Cola de Nuevos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initComponents() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    }//GEN-LAST:event_btnVerNuevosActionPerformed

    private void BotonIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonIniciarActionPerformed
        limpiarSistemaParaNuevaSimulacion();

        // Verificar que hay procesos
        int totalProcesos;
        try {
            semNuevos.acquire();
            try { totalProcesos = colaNuevos.size(); }
            finally { semNuevos.release(); }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }

        if (totalProcesos == 0) {
            JOptionPane.showMessageDialog(this, "No hay procesos en la cola de Nuevos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtener algoritmo seleccionado
        String nombreAlgo = (String) jComboAlgoritmos.getSelectedItem();
        SchedulerManager.Policy politica = seleccionarPolitica(nombreAlgo);

        // Crear SystemClock nuevo
        systemClock = SystemClock.getInstance();
        int velocidadMs = cycleDurationSlider.getValue();
        systemClock.setCycleDuration(velocidadMs);

        // Crear SchedulerManager conectado al clock
        schedulerManager = new SchedulerManager(systemClock);
        schedulerManager.setPolicy(politica);

        simulacionCorriendo = true;
        actualizarEstadoBotones();

        final int total = totalProcesos;
        final SchedulerManager.Policy pol = politica;

        // Hilo del motor de simulación
        new Thread(() -> {
            // 1. Admitir todos los procesos de la cola de nuevos
            try {
                semNuevos.acquire();
                try {
                    while (!colaNuevos.estaVacia()) {
                        PCB p = colaNuevos.desencolar();
                        p.setArrivalTime(systemClock.getCurrentCycle());
                        schedulerManager.admitProcess(p);
                        logEvento("ADMITIDO: " + p.getProcessName()
                                + " | DL=" + p.getRemainingDeadline()
                                + " | Pri=" + p.getPriority());
                    }
                } finally {
                    semNuevos.release(); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // 2. Arrancar el reloj
            systemClock.startClock();
            logEvento("SIMULACIÓN iniciada con " + total + " procesos | Algoritmo: " + nombreAlgo);

            // 3. Esperar hasta que todos terminen
            // Chequeamos periódicamente la cantidad de terminados
            while (true) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }

                int terminados = getTerminatedSnapshot(schedulerManager.getActivePolicy()).size();
                if (terminados >= total) break;

                // Actualizar gráfico de utilización
                int ciclo = systemClock.getCurrentCycle();
                PCB running = schedulerManager.getRunningProcess();
                double util = (running != null) ? 100.0 : 0.0;
                final int c = ciclo; final double u = util;
                SwingUtilities.invokeLater(() -> seriesUtilizacion.add(c, u));
            }

            // 4. Detener reloj
            systemClock.stopClock();
            logEvento("SIMULACIÓN completada.");

            SwingUtilities.invokeLater(() -> {
                if (guiTimer != null) guiTimer.stop();
                simulacionCorriendo = false;
                actualizarEstadoBotones();
                actualizarPanelesDeColas();
                JOptionPane.showMessageDialog(Interfaz.this,
                        "Simulación completada con éxito.",
                        "Simulación Terminada", JOptionPane.INFORMATION_MESSAGE);
            });

        }, "HiloMotorSimulacion").start();

        // Iniciar timer de refresco de GUI
        iniciarTimerGUI();
        actualizarEstadoBotones();
    }//GEN-LAST:event_BotonIniciarActionPerformed

    private void jComboAlgoritmosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboAlgoritmosActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboAlgoritmosActionPerformed

    private void cycleDurationSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cycleDurationSliderStateChanged
       // Sin acción directa
    }//GEN-LAST:event_cycleDurationSliderStateChanged

    private void btnVerMetricasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerMetricasActionPerformed
        mostrarMetricas();

    }//GEN-LAST:event_btnVerMetricasActionPerformed

    private void Boton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Boton20ActionPerformed
        Random rand = new Random();
        int creados = 0;
        int cicloActual = (systemClock != null) ? systemClock.getCurrentCycle() : 0;

        for (int i = 0; i < 20; i++) {
            try {
                String nombre = "P_Rand_" + (i + 1);
                int instrucciones  = rand.nextInt(26) + 10;   // 10-35
                int prioridad      = rand.nextInt(5) + 1;     // 1-5
                int deadline       = instrucciones + rand.nextInt(20) + 5; // > instrucciones
                boolean esIO       = rand.nextBoolean();
                InstructionType tipo = esIO ? InstructionType.IO : InstructionType.CPU;
                int ciclosExc = esIO ? rand.nextInt(6) + 3 : 0;
                int ciclosRes = esIO ? rand.nextInt(4) + 2 : 0;

                PCB p = new PCB(nombre, instrucciones, tipo, prioridad, deadline,
                        ProcessType.APERIODIC, cicloActual);
                p.setCyclesUntilIOException(ciclosExc);
                p.setCyclesForIOCompletion(ciclosRes);

                semNuevos.acquire();
                try { colaNuevos.encolar(p); }
                finally { semNuevos.release(); }

                creados++;
            } catch (Exception e) {
                System.err.println("Error creando proceso aleatorio: " + e.getMessage());
            }
        }

        JOptionPane.showMessageDialog(this, "Se crearon " + creados + " procesos aleatorios.");
        actualizarEstadoBotones();
    }
    
    JOptionPane.showMessageDialog(this, "Se crearon " + procesosCreados + " procesos aleatorios.");
    
    // Actualiza los botones "Ver Nuevos" e "Iniciar"
    actualizarEstadoBotones();
    }//GEN-LAST:event_Boton20ActionPerformed

    private void BotonEscribirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonEscribirActionPerformed
        // TODO add your handling code here:
        try {
            // 1. Carga la configuración actual (para mostrarla como defecto)
            CargaProcesoConfig configActual = CargaIO.load();

            // 2. Pedir los valores al usuario uno por uno
            String strInstrucciones = (String) JOptionPane.showInputDialog(
                    this,
                    "Número de instrucciones por defecto:",
                    "Guardar Carga",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    configActual.getInstrucciones()); // Valor por defecto
            if (strInstrucciones == null) return; // Usuario canceló

            // ---
            String[] opcionesTipo = {"CPU Bound", "I/O Bound"};
            int tipoSeleccionado = JOptionPane.showOptionDialog(
                    this,
                    "Tipo de proceso por defecto:",
                    "Guardar Carga",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcionesTipo,
                    configActual.isEsIoBound() ? opcionesTipo[1] : opcionesTipo[0]);
            if (tipoSeleccionado == -1) return; // Usuario canceló
            boolean esIoBound = (tipoSeleccionado == 1);

            // ---
            String strCiclosExcepcion = (String) JOptionPane.showInputDialog(
                    this,
                    "Ciclos para generar E/S (si es I/O Bound):",
                    "Guardar Carga",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    configActual.getCiclosExcepcion());
            if (strCiclosExcepcion == null) return;

            // ---
            String strCiclosResolver = (String) JOptionPane.showInputDialog(
                    this,
                    "Ciclos para resolver E/S (si es I/O Bound):",
                    "Guardar Carga",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    configActual.getCiclosResolver());
            if (strCiclosResolver == null) return;
            

            // 3. Crear el nuevo objeto de configuración
            CargaProcesoConfig configNueva = new CargaProcesoConfig();
            configNueva.setInstrucciones(Integer.parseInt(strInstrucciones));
            configNueva.setEsIoBound(esIoBound);
            configNueva.setCiclosExcepcion(Integer.parseInt(strCiclosExcepcion));
            configNueva.setCiclosResolver(Integer.parseInt(strCiclosResolver));

            // 4. Guardar en el archivo
            CargaIO.save(configNueva);
            
            JOptionPane.showMessageDialog(this, 
                    "Configuración de carga guardada exitosamente en 'carga.json'.",
                    "Guardado",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                    "Error: Ingrese solo números válidos.",
                    "Error de Formato",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error al guardar: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    
    }//GEN-LAST:event_BotonEscribirActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Interfaz().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Boton20;
    private javax.swing.JButton BotonCambiarAlgoritmo;
    private javax.swing.JButton BotonCrearProceso;
    private javax.swing.JButton BotonEscribir;
    private javax.swing.JButton BotonIniciar;
    private javax.swing.JPanel PanelBloqueados;
    private javax.swing.JPanel PanelBloqueados_Suspendidos;
    private javax.swing.JPanel PanelListos;
    private javax.swing.JPanel PanelListos_Suspendidos;
    private javax.swing.JPanel PanelProcesoEjecucion;
    private javax.swing.JPanel PanelRendimiento;
    private javax.swing.JPanel PanelTerminados;
    private javax.swing.JPanel PanelTerminadosEventos;
    private javax.swing.JLabel TextRelojGlobal;
    private javax.swing.JButton btnVerMetricas;
    private javax.swing.JButton btnVerNuevos;
    private javax.swing.JSlider cycleDurationSlider;
    private javax.swing.JComboBox<String> jComboAlgoritmos;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblIdProcesoCPU;
    private javax.swing.JLabel lblMarProcesoCPU;
    private javax.swing.JLabel lblNombreProcesoCPU;
    private javax.swing.JLabel lblPcProcesoCPU;
    private javax.swing.JLabel lblStatusProcesoCPU;
    private javax.swing.JLabel lblTipoProcesoCPU;
    // End of variables declaration//GEN-END:variables
}
