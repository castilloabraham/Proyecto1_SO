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
 * @author Miguel / freya Blanca
 */
public class PEP implements ClockListener {
    private final Lista<PCB> readyList;
    private final Lista<PCB> blockedList;
    private final Lista<PCB> terminatedList;
    private PCB runningProcess;
    private final ResourceManager rm;

    private int completedCount;
    private int missedDeadlines;

    public PEP() {
        this.readyList      = new Lista<>();
        this.blockedList    = new Lista<>();
        this.terminatedList = new Lista<>();
        this.runningProcess = null;
        this.rm             = ResourceManager.getInstance();
    }
    
    //admision
    public void admitProcess(PCB process) {
        if (process == null) return;
        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            readyList.agregarAlFinal(process);
            System.out.println("[PEP] Admitido: " + process.getProcessName()
                    + " | Prioridad: " + process.getPriority()
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

        // Preemption: ¿hay alguien con mayor prioridad que el actual?
        if (runningProcess != null) {
            checkPreemption(currentCycle);
        }

        if (runningProcess != null) {
            executeCycle(currentCycle);
        }

        updateTimers();
    }

    public String getListenerName() { return "PEP"; }
    public int getPriority() { return 10; }
    
    //logica interna
    private void dispatch(int cycle) {
        PCB highest = findHighestPriority();
        if (highest == null) return;
        rm.acquireReadyQueue();
        readyList.eliminar(highest);
        rm.releaseReadyQueue();
        runningProcess = highest;
        runningProcess.changeState(ProcessState.RUNNING);
        System.out.println("[PEP][T=" + cycle + "] CPU → " + runningProcess.getProcessName()
                + " | Prioridad: " + runningProcess.getPriority());
    }

    private void checkPreemption(int cycle) {
        PCB highest = findHighestPriority();
        if (highest == null) return;
        // Menor número = mayor prioridad
        if (highest.getPriority() < runningProcess.getPriority()) {
            PCB preempted = runningProcess;
            runningProcess = null;
            System.out.println("[PEP][T=" + cycle + "] Preemption: "
                    + preempted.getProcessName() + "(p=" + preempted.getPriority()
                    + ") → " + highest.getProcessName() + "(p=" + highest.getPriority() + ")");
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

    private PCB findHighestPriority() {
        PCB best = null;
        int min = Integer.MAX_VALUE;
        Nodo<PCB> node = readyList.getCabeza();
        while (node != null) {
            if (node.getDato().getPriority() < min) {
                min = node.getDato().getPriority();
                best = node.getDato();
            }
            node = node.getSiguiente();
        }
        return best;
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
            System.out.println("[PEP] Bloqueado: " + process.getProcessName());
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
            System.out.println("[PEP] !! Fallo de Deadline: " + process.getProcessName());
            Interfaces.InterfazHome.logEvento("[Deadline] Fallo de Deadline en Proceso " + process.getProcessName() + " [" + process.getProcessID() + "]");
        }
        System.out.println("[PEP] Terminado: " + process.getProcessName()
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
            System.out.println("[PEP] Desbloqueado → listo: " + process.getProcessName());
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
        Lista<Proceso> lista = new Lista<>();
        lista.agregarAlFinal(new Proceso("P1", 10, 3));
        lista.agregarAlFinal(new Proceso("P2", 2, 1)); // Mayor prioridad

        int tiempoActual = 0;
        int completados = 0;
        int n = lista.size();

        while (completados < n) {
            Proceso masPrioritario = null;
            int mayorPrioridad = Integer.MAX_VALUE;

            Nodo<Proceso> nodoActual = lista.getCabeza();
            while (nodoActual != null) {
                Proceso p = nodoActual.getDato();
                if (p.tiempoRestante > 0 && p.prioridad < mayorPrioridad) {
                    mayorPrioridad = p.prioridad;
                    masPrioritario = p;
                }
                nodoActual = nodoActual.getSiguiente();
            }

            if (masPrioritario != null) {
                System.out.println("[T=" + tiempoActual + "] " + masPrioritario.nombre + " corre.");
                masPrioritario.tiempoRestante--;
                tiempoActual++;
                if (masPrioritario.tiempoRestante == 0) completados++;
            }
        }
    }
    
    
    //getter y seter
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