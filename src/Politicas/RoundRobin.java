/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Estructuras.Cola;
import Estructuras.Lista;
import Estructuras.Nodo;
import Process.PCB;
import Process.ProcessState;
import clock.ClockListener;
import clock.ResourceManager;

/**
 * @author Miguel / freya blanca
 */
public class RoundRobin {
    
    private final Cola<PCB> readyQueue;
    private final Lista<PCB> blockedList;
    private final Lista<PCB> terminatedList;
    private PCB runningProcess;
    private final ResourceManager rm;
    private final int quantum;

    private int completedCount;
    private int missedDeadlines;

    public RoundRobin(int quantum) {
        this.readyQueue     = new Cola<>();
        this.blockedList    = new Lista<>();
        this.terminatedList = new Lista<>();
        this.runningProcess = null;
        this.rm             = ResourceManager.getInstance();
        this.quantum        = Math.max(1, quantum);
    }

    public RoundRobin() { this(3); }
    
    
    //admision
    public void admitProcess(PCB process) {
        if (process == null) return;
        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            process.resetQuantum(quantum);
            readyQueue.encolar(process);
            System.out.println("[RR] Admitido: " + process.getProcessName()
                    + " (Q=" + quantum + ") | Cola: " + readyQueue.size());
        } finally {
            rm.releaseReadyQueue();
        }
    }
    
    // ClockListener 
    public void onClockTick(int currentCycle) {
        processBlockedIO();

        if (runningProcess == null && !readyQueue.estaVacia()) {
            dispatch(currentCycle);
        }

        if (runningProcess != null) {
            executeCycle(currentCycle);

            // Si el proceso sigue vivo, verificar si agotó su quantum
            if (runningProcess != null) {
                boolean quantumExpired = runningProcess.decrementQuantum();
                if (quantumExpired) {
                    preempt(currentCycle);
                }
            }
        }

        updateTimers();
    }


    public String getListenerName() { return "RoundRobin"; }
    public int getPriority() { return 10; }
    
    
    //logica interna
    private void dispatch(int cycle) {
        rm.acquireReadyQueue();
        try {
            runningProcess = readyQueue.desencolar();
        } finally {
            rm.releaseReadyQueue();
        }
        if (runningProcess != null) {
            runningProcess.changeState(ProcessState.RUNNING);
            runningProcess.resetQuantum(quantum);
            System.out.println("[RR][T=" + cycle + "] CPU → " + runningProcess.getProcessName()
                    + " | Q=" + quantum);
        }
    }

    private void executeCycle(int cycle) {
        boolean finished = runningProcess.executeOneCycle();
        if (finished || runningProcess.isCompleted()) {
            terminateProcess(runningProcess);
        } else if (runningProcess.isIsBlockedForIO()) {
            blockProcess(runningProcess);
        }
    }

    private void preempt(int cycle) {
        PCB preempted = runningProcess;
        runningProcess = null;
        System.out.println("[RR][T=" + cycle + "] Quantum agotado → "
                + preempted.getProcessName() + " vuelve a la cola.");
        rm.acquireReadyQueue();
        try {
            preempted.changeState(ProcessState.READY);
            preempted.resetQuantum(quantum);
            readyQueue.encolar(preempted);
        } finally {
            rm.releaseReadyQueue();
        }
    }

    private void blockProcess(PCB process) {
        rm.acquireBlockedQueue();
        try {
            process.changeState(ProcessState.BLOCKED);
            blockedList.agregarAlFinal(process);
            if (runningProcess == process) runningProcess = null;
            System.out.println("[RR] Bloqueado: " + process.getProcessName());
        } finally {
            rm.releaseBlockedQueue();
        }
    }

    private void terminateProcess(PCB process) {
        process.changeState(ProcessState.TERMINATED);
        if (runningProcess == process) runningProcess = null;
        terminatedList.agregarAlFinal(process);
        completedCount++;
        if (process.isMissedDeadline()) {
            missedDeadlines++;
            System.out.println("[RR] !! Fallo de Deadline: " + process.getProcessName());
        }
        System.out.println("[RR] Terminado: " + process.getProcessName()
                + " | Espera=" + process.getWaitingTime());
    }

    public void onProcessUnblocked(PCB process) {
        if (process == null) return;
        rm.acquireBlockedQueue();
        blockedList.eliminar(process);
        rm.releaseBlockedQueue();
        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            process.resetQuantum(quantum);
            readyQueue.encolar(process);
            System.out.println("[RR] Desbloqueado → listo: " + process.getProcessName());
        } finally {
            rm.releaseReadyQueue();
        }
    }

    private void processBlockedIO() {
        if (blockedList.estaVacia()) return;
        Lista<PCB> toUnblock = new Lista<>();
        Nodo<PCB> node = blockedList.getCabeza();
        while (node != null) {
            if (node.getDato().processIOCycle()) toUnblock.agregarAlFinal(node.getDato());
            node = node.getSiguiente();
        }
        Nodo<PCB> u = toUnblock.getCabeza();
        while (u != null) { onProcessUnblocked(u.getDato()); u = u.getSiguiente(); }
    }

    private void updateTimers() {
        if (runningProcess != null) runningProcess.decrementDeadline();
        Cola<PCB> temp = new Cola<>();
        while (!readyQueue.estaVacia()) {
            PCB p = readyQueue.desencolar();
            p.decrementDeadline();
            p.incrementWaitingTime();
            temp.encolar(p);
        }
        while (!temp.estaVacia()) readyQueue.encolar(temp.desencolar());
    }

    public void ejecutar(int quantum) {
        // Usamos tu clase Cola personalizada
        Cola<Proceso> colaListos = new Cola<>();

        // Agregamos procesos de prueba
        colaListos.encolar(new Proceso("P1", 8, 0));
        colaListos.encolar(new Proceso("P2", 4, 0));
        colaListos.encolar(new Proceso("P3", 10, 0));

        int tiempoActual = 0;

        System.out.println("=== Ejecución Round Robin (Quantum: " + quantum + ") ===");

        while (!colaListos.estaVacia()) {
            // Sacamos el proceso que toca
            Proceso p = colaListos.desencolar();
            
            // El tiempo que ejecutará es el mínimo entre lo que le queda y el quantum
            int tiempoEjecucion = (p.tiempoRestante > quantum) ? quantum : p.tiempoRestante;
            
            System.out.println("[T = " + tiempoActual + "] Ejecutando " + p.nombre + " por " + tiempoEjecucion + " unidades.");
            
            p.tiempoRestante -= tiempoEjecucion;
            tiempoActual += tiempoEjecucion;

            if (p.tiempoRestante > 0) {
                // Si aún tiene ráfaga, vuelve al final de la cola (encolar)
                colaListos.encolar(p);
                System.out.println("   -> " + p.nombre + " vuelve a la cola (Restante: " + p.tiempoRestante + ")");
            } else {
                // El proceso terminó
                p.tiempoFinal = tiempoActual;
                p.tiempoEspera = p.tiempoFinal - p.tiempoRafaga;
                System.out.println("   >> " + p.nombre + " FINALIZADO. (Espera: " + p.tiempoEspera + ")");
            }
        }
        
        System.out.println("\nSimulación completada en T = " + tiempoActual);
    }
    
    //getter y setter
    public PCB getRunningProcess(){ return runningProcess; }
    public int getReadyQueueSize(){ return readyQueue.size(); }
    public boolean hasReadyProcesses(){ return !readyQueue.estaVacia(); }
    public int getQuantum(){ return quantum; }
    public int getCompletedCount(){ return completedCount; }
    public int getMissedDeadlines(){ return missedDeadlines; }
    public Lista<PCB> getTerminatedList(){ return terminatedList; }
    public Lista<PCB> getBlockedList(){ return blockedList; }

    public double getMissionSuccessRate() {
        if (completedCount == 0) return 100.0;
        return ((completedCount - missedDeadlines) * 100.0) / completedCount;
    }

    public void reset() {
        readyQueue.vaciar();
        while (!blockedList.estaVacia())    blockedList.eliminar(0);
        while (!terminatedList.estaVacia()) terminatedList.eliminar(0);
        runningProcess = null;
        completedCount = 0;
        missedDeadlines = 0;
    }

    public Cola<PCB> getReadyQueueSnapshot() {
        Cola<PCB> snap = new Cola<>(), temp = new Cola<>();
        while (!readyQueue.estaVacia()) {
            PCB p = readyQueue.desencolar();
            snap.encolar(p); temp.encolar(p);
        }
        while (!temp.estaVacia()) readyQueue.encolar(temp.desencolar());
        return snap;
    }

    
}