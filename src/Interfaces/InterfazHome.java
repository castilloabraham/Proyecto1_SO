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


import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author adcd_
 */
public class InterfazHome extends javax.swing.JFrame implements clock.ClockListener {
    
    
    public static SystemClock clock;
    public static SchedulerManager schedulerManager;
    private static javax.swing.JTextArea logEventosRef;
    


    /**
     * Creates new form InterfazHome
     */
    public InterfazHome() {
       
        super("Simulador de Planificación de Procesos");
        this.clock = SystemClock.getInstance();
        this.schedulerManager = new SchedulerManager(clock, 10);
        
        initComponents();
        org.netbeans.lib.awtextra.AbsoluteConstraints ac;

        labelModoSO = new javax.swing.JLabel("⚙ SISTEMA OPERATIVO");
        labelModoSO.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
        labelModoSO.setForeground(new java.awt.Color(180, 0, 0));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 75, 280, 25);
        jPanel1.add(labelModoSO, ac);

        javax.swing.JLabel labelVelocidadTitulo = new javax.swing.JLabel("Velocidad del ciclo:");
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 130, 150, 20);
        jPanel1.add(labelVelocidadTitulo, ac);

        sliderVelocidad = new javax.swing.JSlider(50, 2000, 1000);
        sliderVelocidad.setInverted(true);
        sliderVelocidad.setMajorTickSpacing(500);
        sliderVelocidad.setMinorTickSpacing(100);
        sliderVelocidad.setPaintTicks(true);
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 150, 260, 45);
        jPanel1.add(sliderVelocidad, ac);

        labelVelocidadActual = new javax.swing.JLabel("1000 ms/ciclo");
        labelVelocidadActual.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 11));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 195, 260, 20);
        jPanel1.add(labelVelocidadActual, ac);

        javax.swing.JLabel labelRapido = new javax.swing.JLabel("◀ Lento");
        labelRapido.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 10));
        labelRapido.setForeground(new java.awt.Color(0, 120, 0));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 212, 70, 15);
        jPanel1.add(labelRapido, ac);
        
        
        
        sliderVelocidad.addChangeListener(e -> {
            int val = sliderVelocidad.getValue();
            clock.setCycleDuration(val);
            labelVelocidadActual.setText(val + " ms/ciclo");
        });

        javax.swing.JLabel labelLento = new javax.swing.JLabel("Rapido ▶");
        labelLento.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 10));
        labelLento.setForeground(new java.awt.Color(180, 0, 0));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 212, 60, 15);
        jPanel1.add(labelLento, ac);
        
        labelTasaExito = new javax.swing.JLabel("Éxito de Misión: 100%");
        labelTasaExito.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
        labelTasaExito.setForeground(new java.awt.Color(0, 100, 180));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 230, 260, 20);
        jPanel1.add(labelTasaExito, ac);

        javax.swing.JButton btnGrafico = new javax.swing.JButton("Ver Gráfico CPU");
        btnGrafico.addActionListener(e -> Graficocpu.getInstance().setVisible(true));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 255, 260, 30);
        jPanel1.add(btnGrafico, ac);
        
        labelThroughput = new javax.swing.JLabel("Throughput: 0 proc/ciclo");
        labelThroughput.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 11));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 290, 260, 20);
        jPanel1.add(labelThroughput, ac);

        labelEsperaPromedio = new javax.swing.JLabel("Espera promedio: 0 ciclos");
        labelEsperaPromedio.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 11));
        ac = new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 310, 260, 20);
        jPanel1.add(labelEsperaPromedio, ac);

        logEventosRef = LogEventos;
        clock.addListener(this);
        
        
    }
    
    public static void logEvento(String mensaje) {
        if (logEventosRef == null) return;
        SwingUtilities.invokeLater(() -> {
            logEventosRef.append(mensaje + "\n");
            logEventosRef.setCaretPosition(logEventosRef.getDocument().getLength());
        });
    }
    
    public void onClockTick(int currentCycle) {
        // Registrar utilización del CPU
        boolean cpuOcupado = schedulerManager.getRunningProcess() != null &&
            schedulerManager.getRunningProcess().getCurrentState() == ProcessState.RUNNING;
        Graficocpu.getInstance().registrarCiclo(currentCycle, cpuOcupado);

        SwingUtilities.invokeLater(() -> {
            labelReloj.setText("Ciclo: " + currentCycle);
            actualizarColas();
            actualizarCPU();
        });
    }
    
    private void actualizarCulminados() {
        Culminados.setText("");

        Nodo<PCB> cabeza = null;
        switch (schedulerManager.getActivePolicy()) {
            case FCFS:        cabeza = ((Politicas.FCFS)       schedulerManager.getActiveScheduler()).getTerminatedList().getCabeza(); break;
            case ROUND_ROBIN: cabeza = ((Politicas.RoundRobin) schedulerManager.getActiveScheduler()).getTerminatedList().getCabeza(); break;
            case SRT:         cabeza = ((Politicas.SRT)        schedulerManager.getActiveScheduler()).getTerminatedList().getCabeza(); break;
            case PEP:         cabeza = ((Politicas.PEP)        schedulerManager.getActiveScheduler()).getTerminatedList().getCabeza(); break;
            case EDF:         cabeza = ((Politicas.EDF)        schedulerManager.getActiveScheduler()).getTerminatedList().getCabeza(); break;
        }

        Nodo<PCB> actual = cabeza;
        while (actual != null) {
            PCB p = actual.getDato();
            String deadline = p.isMissedDeadline() ? "❌ FALLÓ" : "✅ OK";
            Culminados.append(
                "► " + p.getProcessName() + " [" + p.getProcessID() + "]\n" +
                "  Espera: " + p.getWaitingTime() + " ciclos\n" +
                "  CPU usado: " + p.getCpuTimeUsed() + " ciclos\n" +
                "  Deadline: " + deadline + "\n" +
                "─────────────────────\n"
            );
            actual = actual.getSiguiente();
        }
        
        int completados = schedulerManager.getMTS().getSwapInCount() >= 0 ? 0 : 0;
        // Recorrer terminados para calcular espera promedio
        Nodo<PCB> nodo = cabeza;
        int totalEspera = 0;
        int count = 0;
        while (nodo != null) {
            totalEspera += nodo.getDato().getWaitingTime();
            count++;
            nodo = nodo.getSiguiente();
        }

        int cicloActual = clock.getCurrentCycle();
        double throughput = cicloActual > 0 ? (count * 1.0) / cicloActual : 0;
        double esperaProm = count > 0 ? (totalEspera * 1.0) / count : 0;

        labelThroughput.setText(String.format("Throughput: %.3f proc/ciclo", throughput));
        labelEsperaPromedio.setText(String.format("Espera promedio: %.1f ciclos", esperaProm));
    }
    
    private void actualizarCPU() {
        PCB proceso = schedulerManager.getRunningProcess();

        if (proceso != null && proceso.getCurrentState() == ProcessState.RUNNING) {
            labelModoSO.setText("▶ PROGRAMA DE USUARIO");
            labelModoSO.setForeground(new java.awt.Color(0, 150, 0));

            labelNombreCPU.setText("Nombre: " + proceso.getProcessName()
                + " [" + proceso.getProcessID() + "]");
            labelEstadoCPU.setText("Estado: " + proceso.getCurrentState().getDisplayName()
                + " | Prior: " + proceso.getPriority());
            labelPCCPU.setText("PC: " + proceso.getProgramCounter()
                + " | MAR: " + proceso.getMemoryAddressRegister());
            labelInstruccionesCPU.setText("Restantes: " + proceso.getRemainingInstructions()
                + " | Deadline: " + proceso.getRemainingDeadline());
        } else {
            labelModoSO.setText("⚙ SISTEMA OPERATIVO");
            labelModoSO.setForeground(new java.awt.Color(180, 0, 0));

            labelNombreCPU.setText("Nombre: -");
            labelEstadoCPU.setText("Estado: -");
            labelPCCPU.setText("PC: - | MAR: -");
            labelInstruccionesCPU.setText("Restantes: -");
        }
        
        labelTasaExito.setText(String.format("Éxito de Misión: %.1f%%", 
            schedulerManager.getMissionSuccessRate()));
    }

    private void actualizarColas() {
        verListos.setText("");
        verBloqueados.setText("");
        verListosSuspendidos.setText("");
        verBloqueadosSuspendidos.setText("");

        mostrarCola(verListos, obtenerCabezaListos());
        mostrarCola(verBloqueados, obtenerCabezaBloqueados());
        mostrarListaSuspendidos(verListosSuspendidos, 
            schedulerManager.getMTS().getReadySuspendedList().getCabeza());
        mostrarListaSuspendidos(verBloqueadosSuspendidos, 
            schedulerManager.getMTS().getBlockedSuspendedList().getCabeza());

        // Temporal para debug
//        InterfazHome.logEvento("[MTS] En memoria: " + schedulerManager.getMTS().getProcessesInMemory() 
//            + "/" + schedulerManager.getMTS().getMaxProcessesInMemory()
//            + " | Suspendidos listos: " + schedulerManager.getMTS().getReadySuspendedCount()
//            + " | Suspendidos bloqueados: " + schedulerManager.getMTS().getBlockedSuspendedCount());
        actualizarCulminados();
    }
    
    private Nodo<PCB> obtenerCabezaBloqueados() {
        switch (schedulerManager.getActivePolicy()) {
            case FCFS:        return ((Politicas.FCFS)       schedulerManager.getActiveScheduler()).getBlockedList().getCabeza();
            case ROUND_ROBIN: return ((Politicas.RoundRobin) schedulerManager.getActiveScheduler()).getBlockedList().getCabeza();
            case SRT:         return ((Politicas.SRT)        schedulerManager.getActiveScheduler()).getBlockedList().getCabeza();
            case PEP:         return ((Politicas.PEP)        schedulerManager.getActiveScheduler()).getBlockedList().getCabeza();
            case EDF:         return ((Politicas.EDF)        schedulerManager.getActiveScheduler()).getBlockedList().getCabeza();
            default:          return null;
        }
    }

    private Nodo<PCB> obtenerCabezaListos() {
        switch (schedulerManager.getActivePolicy()) {
            case FCFS:        return ((Politicas.FCFS)       schedulerManager.getActiveScheduler()).getReadyQueue() != null ?
                                     ((Politicas.FCFS)       schedulerManager.getActiveScheduler()).getReadyQueue().getFrente() : null;
            case ROUND_ROBIN: return ((Politicas.RoundRobin) schedulerManager.getActiveScheduler()).getReadyQueue() != null ?
                                     ((Politicas.RoundRobin) schedulerManager.getActiveScheduler()).getReadyQueue().getFrente() : null;
            case SRT:         return ((Politicas.SRT)        schedulerManager.getActiveScheduler()).getReadyList().getCabeza();
            case PEP:         return ((Politicas.PEP)        schedulerManager.getActiveScheduler()).getReadyList().getCabeza();
            case EDF:         return ((Politicas.EDF)        schedulerManager.getActiveScheduler()).getReadyList().getCabeza();
            default:          return null;
        }
    }

    private void mostrarCola(javax.swing.JTextArea area, Nodo<PCB> cabeza) {
        Nodo<PCB> actual = cabeza;
        while (actual != null) {
            PCB p = actual.getDato();
            area.append("• " + p.getProcessName() + " | PC: " + p.getProgramCounter() + "\n");
            actual = actual.getSiguiente();
        }
    }

    private void mostrarListaSuspendidos(javax.swing.JTextArea area, Nodo<PCB> cabeza) {
        Nodo<PCB> actual = cabeza;
        while (actual != null) {
            PCB p = actual.getDato();
            area.append("• " + p.getProcessName() + " | Deadline: " + p.getRemainingDeadline() + "\n");
            actual = actual.getSiguiente();
        }
    }

    public String getListenerName() {
        return "InterfazHome";
    }

    public int getPriority() {
        return 99; // Baja prioridad, la UI se actualiza de última
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        verBloqueadosSuspendidos = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        verListos = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        verBloqueados = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        verListosSuspendidos = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        iniciar = new javax.swing.JButton();
        cancelar = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        LogEventos = new javax.swing.JTextArea();
        jLabel7 = new javax.swing.JLabel();
        politicas = new javax.swing.JComboBox<>();
        labelReloj = new javax.swing.JLabel();
        labelInstruccionesCPU = new javax.swing.JLabel();
        labelNombreCPU = new javax.swing.JLabel();
        labelEstadoCPU = new javax.swing.JLabel();
        labelPCCPU = new javax.swing.JLabel();
        VariosProcesos = new javax.swing.JButton();
        labelModoSO = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        Culminados = new javax.swing.JTextArea();
        jLabel8 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Simulador");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 10, -1, -1));

        jLabel2.setText("Bloqueados/Suspendidos");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 480, -1, -1));

        verBloqueadosSuspendidos.setColumns(20);
        verBloqueadosSuspendidos.setRows(5);
        jScrollPane1.setViewportView(verBloqueadosSuspendidos);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 500, 230, -1));

        jLabel3.setText("Visualización de Colas");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, -1, -1));

        verListos.setColumns(20);
        verListos.setRows(5);
        jScrollPane2.setViewportView(verListos);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, 230, -1));

        verBloqueados.setColumns(20);
        verBloqueados.setRows(5);
        jScrollPane3.setViewportView(verBloqueados);

        jPanel1.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 250, 230, -1));

        verListosSuspendidos.setColumns(20);
        verListosSuspendidos.setRows(5);
        jScrollPane4.setViewportView(verListosSuspendidos);

        jPanel1.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 370, 230, -1));

        jLabel4.setText("Culminados");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 240, -1, -1));

        jLabel5.setText("Bloqueados");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 230, -1, -1));

        jLabel6.setText("Listos/Suspendidos");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 350, -1, -1));

        iniciar.setText("Iniciar");
        iniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                iniciarActionPerformed(evt);
            }
        });
        jPanel1.add(iniciar, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 40, -1, 40));

        cancelar.setText("Cancelar");
        cancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelarActionPerformed(evt);
            }
        });
        jPanel1.add(cancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 40, -1, 40));

        LogEventos.setColumns(20);
        LogEventos.setRows(5);
        jScrollPane5.setViewportView(LogEventos);

        jPanel1.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 540, 630, 150));

        jLabel7.setText("Listos");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, -1, -1));

        politicas.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "FCFS - First Come First Served", "Round Robin - Con quantum", "SRT - Shortest Remaining Time", "PEP - Planificación por Prioridad", "EDF - Earliest Deadline First" }));
        politicas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                politicasActionPerformed(evt);
            }
        });
        jPanel1.add(politicas, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 40, 270, 40));

        labelReloj.setText("Ciclo: ");
        jPanel1.add(labelReloj, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 20, 120, -1));

        labelInstruccionesCPU.setText("Restantes:");
        jPanel1.add(labelInstruccionesCPU, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 200, -1, -1));

        labelNombreCPU.setText("Nombre: ");
        jPanel1.add(labelNombreCPU, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 110, -1, -1));

        labelEstadoCPU.setText("Estado:");
        jPanel1.add(labelEstadoCPU, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 140, -1, -1));

        labelPCCPU.setText("PC:");
        jPanel1.add(labelPCCPU, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 170, -1, -1));

        VariosProcesos.setText("Crear 20 procesos de una vez");
        VariosProcesos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VariosProcesosActionPerformed(evt);
            }
        });
        jPanel1.add(VariosProcesos, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 90, 260, -1));

        labelModoSO.setText("labelModoSO");
        jPanel1.add(labelModoSO, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, -1, -1));

        Culminados.setColumns(20);
        Culminados.setRows(5);
        jScrollPane6.setViewportView(Culminados);

        jPanel1.add(jScrollPane6, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 260, -1, 130));

        jLabel8.setText("Log de eventos");
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 520, -1, -1));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 990, 700));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void iniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_iniciarActionPerformed
        if (!clock.isRunning()) {
            clock.startClock();
            iniciar.setEnabled(false);
            cancelar.setEnabled(true);
            System.out.println("[UI] Simulación iniciada.");
            InterfazHome.logEvento("Simulación iniciada");
        }
    }//GEN-LAST:event_iniciarActionPerformed

    private void cancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelarActionPerformed
        // TODO add your handling code here:
        if (clock.isRunning()) {
            clock.stopClock();
            iniciar.setEnabled(true);
            iniciar.setText("Iniciar");
            cancelar.setEnabled(false);
            System.out.println("[UI] Simulación cancelada.");
        }
    }//GEN-LAST:event_cancelarActionPerformed

    private void politicasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_politicasActionPerformed
        String seleccion = (String) politicas.getSelectedItem();
    
        switch (seleccion) {
            case "FCFS - First Come First Served":
                schedulerManager.setPolicy(SchedulerManager.Policy.FCFS);
                InterfazHome.logEvento("[Política] Cambiada a FCFS");
                break;
            case "Round Robin - Con quantum":
                schedulerManager.setPolicy(SchedulerManager.Policy.ROUND_ROBIN);
                InterfazHome.logEvento("[Política] Cambiada a Round Robin");
                break;
            case "SRT - Shortest Remaining Time":
                schedulerManager.setPolicy(SchedulerManager.Policy.SRT);
                InterfazHome.logEvento("[Política] Cambiada a SRT");
                break;
            case "PEP - Planificación por Prioridad":
                schedulerManager.setPolicy(SchedulerManager.Policy.PEP);
                InterfazHome.logEvento("[Política] Cambiada a PEP");
                break;
            case "EDF - Earliest Deadline First":
                schedulerManager.setPolicy(SchedulerManager.Policy.EDF);
                InterfazHome.logEvento("[Política] Cambiada a EDF");
                break;
        }
    }//GEN-LAST:event_politicasActionPerformed

    private void VariosProcesosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VariosProcesosActionPerformed
        VariosProcesos.setEnabled(false);
        new Thread(() -> {
            clock.pauseClock();
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            String[] prefijos = {"Proc", "Task", "Job", "Sys", "App", "Net", "IO", "Mem"};
            String[] sufijos = {"Alpha", "Beta", "Core", "Max", "Pro", "Neo", "Plus", "X"};

            java.util.Random rand = new java.util.Random();

            for (int i = 0; i < 20; i++) {
                String nombre = prefijos[rand.nextInt(prefijos.length)]
                              + "-" + sufijos[rand.nextInt(sufijos.length)]
                              + "-" + (i + 1);

                int instrucciones = rand.nextInt(15) + 5;
                int prioridad     = rand.nextInt(10) + 1;
                int deadline      = rand.nextInt(30) + 20;

                InstructionType tipo = rand.nextBoolean() ?
                    InstructionType.CPU : InstructionType.IO;

                ProcessType tipoProceso = rand.nextBoolean() ?
                    ProcessType.PERIODIC : ProcessType.APERIODIC;

                PCB proceso = new PCB(
                    nombre, instrucciones, tipo,
                    prioridad, deadline, tipoProceso,
                    clock.getCurrentCycle()
                );

                if (tipo == InstructionType.IO) {
                    proceso.setCyclesUntilIOException(2);
                    proceso.setCyclesForIOCompletion(5);
                }

                schedulerManager.admitProcess(proceso);
                final String nombreFinal = nombre;
                SwingUtilities.invokeLater(() ->
                    InterfazHome.logEvento("[Nuevo] Proceso creado: " + nombreFinal));
            }

            clock.resumeClock();
            SwingUtilities.invokeLater(() -> {
                VariosProcesos.setEnabled(true);
                actualizarColas();
                JOptionPane.showMessageDialog(null, "Se crearon 20 procesos.");
            });
        }).start();
    }//GEN-LAST:event_VariosProcesosActionPerformed

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
            java.util.logging.Logger.getLogger(InterfazHome.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(InterfazHome.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(InterfazHome.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(InterfazHome.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new InterfazHome().setVisible(true);
            }
        });
    }

    private javax.swing.JLabel labelThroughput;
    private javax.swing.JLabel labelEsperaPromedio;
    private javax.swing.JLabel labelTasaExito;
    private javax.swing.JSlider sliderVelocidad;
    private javax.swing.JLabel labelVelocidadActual;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea Culminados;
    private javax.swing.JTextArea LogEventos;
    private javax.swing.JButton VariosProcesos;
    private javax.swing.JButton cancelar;
    private javax.swing.JButton iniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JLabel labelEstadoCPU;
    private javax.swing.JLabel labelInstruccionesCPU;
    private javax.swing.JLabel labelModoSO;
    private javax.swing.JLabel labelNombreCPU;
    private javax.swing.JLabel labelPCCPU;
    private javax.swing.JLabel labelReloj;
    private javax.swing.JComboBox<String> politicas;
    private javax.swing.JTextArea verBloqueados;
    private javax.swing.JTextArea verBloqueadosSuspendidos;
    private javax.swing.JTextArea verListos;
    private javax.swing.JTextArea verListosSuspendidos;
    // End of variables declaration//GEN-END:variables
}
