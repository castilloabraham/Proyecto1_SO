/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Estructuras.Lista;
import Estructuras.Nodo;
import Process.PCB;
import Process.ProcessState;
import clock.ClockListener;
import clock.ResourceManager;

/**
 * @author Miguel //Freya Blanca
 */
public class SRT {
    
    private final Lista<PCB> readyList;
    private final Lista<PCB> blockedList;
    private final Lista<PCB> terminatedList;
    private PCB runningProcess;
    private final ResourceManager rm;

    private int completedCount;
    private int missedDeadlines;

    public SRT() {
        this.readyList      = new Lista<>();
        this.blockedList    = new Lista<>();
        this.terminatedList = new Lista<>();
        this.runningProcess = null;
        this.rm             = ResourceManager.getInstance();
    }

    // admision
    public void admitProcess(PCB process) {
        if (process == null) return;
        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            readyList.agregarAlFinal(process);
            System.out.println("[SRT] Admitido: " + process.getProcessName()
                    + " | Restante: " + process.getRemainingInstructions()
                    + " | Cola: " + readyList.size());
        } finally {
            rm.releaseReadyQueue();
        }
    }

    //ClockListener
    public void onClockTick(int currentCycle) {
        processBlockedIO();

        if (runningProcess == null && !readyList.estaVacia()) {
            dispatch(currentCycle);
        }

        // Preemption: ¿hay alguno más corto que el que está en CPU?
        if (runningProcess != null) {
            checkPreemption(currentCycle);
        }

        if (runningProcess != null) {
            executeCycle(currentCycle);
        }

        updateTimers();
    }


    public String getListenerName() { return "SRT"; }
    public int getPriority() { return 10; }

    // logica interna 

    private void dispatch(int cycle) {
        PCB shortest = findShortest();
        if (shortest == null) return;
        rm.acquireReadyQueue();
        readyList.eliminar(shortest);
        rm.releaseReadyQueue();
        runningProcess = shortest;
        runningProcess.changeState(ProcessState.RUNNING);
        System.out.println("[SRT][T=" + cycle + "] CPU → " + runningProcess.getProcessName()
                + " | Restante: " + runningProcess.getRemainingInstructions());
    }

    private void checkPreemption(int cycle) {
        PCB shortest = findShortest();
        if (shortest == null) return;
        if (shortest.getRemainingInstructions() < runningProcess.getRemainingInstructions()) {
            PCB preempted = runningProcess;
            runningProcess = null;
            System.out.println("[SRT][T=" + cycle + "] Preemption: "
                    + preempted.getProcessName() + "(rest=" + preempted.getRemainingInstructions()
                    + ") → " + shortest.getProcessName()
                    + "(rest=" + shortest.getRemainingInstructions() + ")");
            rm.acquireReadyQueue();
            try {
                preempted.changeState(ProcessState.READY);
                readyList.agregarAlFinal(preempted);
            } finally {
                rm.releaseReadyQueue();
            }
            dispatch(cycle);
        }
    }

    private PCB findShortest() {
        PCB shortest = null;
        int min = Integer.MAX_VALUE;
        Nodo<PCB> node = readyList.getCabeza();
        while (node != null) {
            if (node.getDato().getRemainingInstructions() < min) {
                min = node.getDato().getRemainingInstructions();
                shortest = node.getDato();
            }
            node = node.getSiguiente();
        }
        return shortest;
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
            System.out.println("[SRT] Bloqueado: " + process.getProcessName());
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
            System.out.println("[SRT] !! Fallo de Deadline: " + process.getProcessName());
        }
        System.out.println("[SRT] Terminado: " + process.getProcessName()
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
            readyList.agregarAlFinal(process);
            System.out.println("[SRT] Desbloqueado → listo: " + process.getProcessName());
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
        Nodo<PCB> node = readyList.getCabeza();
        while (node != null) {
            node.getDato().decrementDeadline();
            node.getDato().incrementWaitingTime();
            node = node.getSiguiente();
        }
    }
    
    public void ejecutar() {
        Lista<Proceso> procesos = new Lista<>();
        procesos.agregarAlFinal(new Proceso("P1", 8, 0));
        procesos.agregarAlFinal(new Proceso("P2", 4, 0));
        procesos.agregarAlFinal(new Proceso("P3", 2, 0));

        int tiempoActual = 0;
        int completados = 0;
        int n = procesos.size();

        System.out.println("=== SRT (Estructuras Propias) ===");

        while (completados < n) {
            Proceso masCorto = null;
            int minRestante = Integer.MAX_VALUE;

            // Recorrido manual usando tu método getCabeza()
            Nodo<Proceso> actual = procesos.getCabeza();
            while (actual != null) {
                Proceso p = actual.getDato();
                if (p.tiempoRestante > 0 && p.tiempoRestante < minRestante) {
                    minRestante = p.tiempoRestante;
                    masCorto = p;
                }
                actual = actual.getSiguiente();
            }

            if (masCorto != null) {
                System.out.println("[T=" + tiempoActual + "] Ejecutando " + masCorto.nombre);
                masCorto.tiempoRestante--;
                tiempoActual++;

                if (masCorto.tiempoRestante == 0) {
                    completados++;
                    System.out.println("   >> " + masCorto.nombre + " terminado.");
                }
            } else {
                tiempoActual++;
            }
        }
    }
    
    public PCB getRunningProcess(){ return runningProcess; }
    public int getReadyQueueSize(){ return readyList.size(); }
    public boolean hasReadyProcesses(){ return !readyList.estaVacia(); }
    public int getCompletedCount(){ return completedCount; }
    public int getMissedDeadlines(){ return missedDeadlines; }
    public Lista<PCB> getTerminatedList(){ return terminatedList; }
    public Lista<PCB> getBlockedList(){ return blockedList; }
    public Lista<PCB> getReadyList(){ return readyList; }

    public double getMissionSuccessRate() {
        if (completedCount == 0) return 100.0;
        return ((completedCount - missedDeadlines) * 100.0) / completedCount;
    }

    public void reset() {
        while (!readyList.estaVacia())      readyList.eliminar(0);
        while (!blockedList.estaVacia())    blockedList.eliminar(0);
        while (!terminatedList.estaVacia()) terminatedList.eliminar(0);
        runningProcess = null;
        completedCount = 0;
        missedDeadlines = 0;
    }
}