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
 * @author Miguel / Freya Blanca
 */
public class FCFS implements ClockListener {
    
    private final Cola<PCB> readyQueue;
    private final Lista<PCB> blockedList;
    private final Lista<PCB> terminatedList;
    private PCB runningProcess;
    private final ResourceManager rm;

    // Métricas
    private int completedCount;
    private int missedDeadlines;

    public FCFS() {
        this.readyQueue     = new Cola<>();
        this.blockedList    = new Lista<>();
        this.terminatedList = new Lista<>();
        this.runningProcess = null;
        this.rm             = ResourceManager.getInstance();
    }

    
    /**
     * Admite un proceso nuevo (NEW → READY) y lo encola.
     */
    public void admitProcess(PCB process) {
        if (process == null) return;
        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            readyQueue.encolar(process);
            System.out.println("[FCFS] Admitido: " + process.getProcessName()
                    + " | Cola: " + readyQueue.size());
        } finally {
            rm.releaseReadyQueue();
        }
    }
    
    
    //ClockListener
    public void onClockTick(int currentCycle) {
        // 1. Procesar E/S de procesos bloqueados
        processBlockedIO();

        // 2. Si CPU libre, despachar el siguiente de la cola
        if (runningProcess == null && !readyQueue.estaVacia()) {
            dispatch(currentCycle);
        }

        // 3. Ejecutar un ciclo del proceso en CPU
        if (runningProcess != null) {
            executeCycle(currentCycle);
        }

        // 4. Actualizar tiempos de espera y deadlines
        updateTimers();
    }
    
    public String getListenerName() { return "FCFS"; }
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
            System.out.println("[FCFS][T=" + cycle + "] CPU → " + runningProcess.getProcessName());
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

    private void blockProcess(PCB process) {
        rm.acquireBlockedQueue();
        try {
            process.changeState(ProcessState.BLOCKED);
            blockedList.agregarAlFinal(process);
            if (runningProcess == process) runningProcess = null;
            System.out.println("[FCFS] Bloqueado: " + process.getProcessName());
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
            System.out.println("[FCFS] !! Fallo de Deadline: " + process.getProcessName());
        }
        System.out.println("[FCFS] Terminado: " + process.getProcessName()
                + " | Espera=" + process.getWaitingTime());
    }

    /**
     * Notifica que un proceso bloqueado terminó su E/S (BLOCKED → READY).
     */
    public void onProcessUnblocked(PCB process) {
        if (process == null) return;
        rm.acquireBlockedQueue();
        blockedList.eliminar(process);
        rm.releaseBlockedQueue();

        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            readyQueue.encolar(process);
            System.out.println("[FCFS] Desbloqueado → listo: " + process.getProcessName());
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

    public void ejecutar() {
        // Usamos tu clase Cola personalizada
        Cola<Proceso> colaListos = new Cola<>();

        // Agregamos procesos de ejemplo usando tu método encolar
        colaListos.encolar(new Proceso("P1", 10, 0));
        colaListos.encolar(new Proceso("P2", 3, 0));
        colaListos.encolar(new Proceso("P3", 5, 0));

        int tiempoActual = 0;
        float sumaEspera = 0;
        int totalProcesos = colaListos.size();

        System.out.println("=== Ejecución FCFS (Estructuras de Abraham Castillo) ===");

        while (!colaListos.estaVacia()) {
            // Extraemos el proceso del frente de la cola
            Proceso p = colaListos.desencolar();
            
            p.tiempoEspera = tiempoActual;
            System.out.println("[T = " + tiempoActual + "] Iniciando " + p.nombre + " (Ráfaga: " + p.tiempoRafaga + ")");
            
            // En FCFS el proceso corre hasta terminar
            tiempoActual += p.tiempoRafaga;
            p.tiempoFinal = tiempoActual;
            sumaEspera += p.tiempoEspera;

            System.out.println("   >> " + p.nombre + " FINALIZADO en T = " + tiempoActual);
        }

        System.out.println("\nPromedio de tiempo de espera: " + (sumaEspera / totalProcesos));
    }

   
    //getter y setter
    public PCB getRunningProcess(){ return runningProcess; }
    public int getReadyQueueSize(){ return readyQueue.size(); }
    public boolean hasReadyProcesses(){ return !readyQueue.estaVacia(); }
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
    
    
}