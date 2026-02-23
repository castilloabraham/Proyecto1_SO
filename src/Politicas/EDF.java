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
 * @author Miguel / Freya Blanca
 */
public class EDF implements ClockListener {
    
    
    private final Lista<PCB> readyList;
    private final Lista<PCB> blockedList;
    private final Lista<PCB> terminatedList;
    private PCB runningProcess;
    private final ResourceManager rm;

    private int completedCount;
    private int missedDeadlines;

    public EDF() {
        this.readyList = new Lista<>();
        this.blockedList = new Lista<>();
        this.terminatedList = new Lista<>();
        this.runningProcess = null;
        this.rm = ResourceManager.getInstance();
    }
    
    //Admision 
    
    public void admitProcess(PCB process) {
        if (process == null) return;
        rm.acquireReadyQueue();
        try {
            process.changeState(ProcessState.READY);
            readyList.agregarAlFinal(process);
            System.out.println("[EDF] Admitido: " + process.getProcessName() + " | Deadline: " + process.getRemainingDeadline() + " | Cola: " + readyList.size());
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

        // EDF es dinámico: en cada ciclo puede haber un nuevo más urgente
        if (runningProcess != null) {
            checkPreemption(currentCycle);
        }

        if (runningProcess != null) {
            executeCycle(currentCycle);
        }

        // Actualizar timers antes de verificar fallos
        updateTimers();
        checkDeadlineFailures(currentCycle);
    }
    
    public String getListenerName() { return "EDF"; }
    public int getPriority() { return 10; }
    
    
    //Logica interna
    private void dispatch(int cycle) {
        PCB earliest = findEarliestDeadline();
        if (earliest == null) return;
        rm.acquireReadyQueue();
        readyList.eliminar(earliest);
        rm.releaseReadyQueue();
        runningProcess = earliest;
        runningProcess.changeState(ProcessState.RUNNING);
        System.out.println("[EDF][T=" + cycle + "] CPU → " + runningProcess.getProcessName() + " | Deadline en: " + runningProcess.getRemainingDeadline() + " ciclos");
    }
    
    
    private void checkPreemption(int cycle) {
        PCB earliest = findEarliestDeadline();
        if (earliest == null) return;
        if (earliest.getRemainingDeadline() < runningProcess.getRemainingDeadline()) {
            PCB preempted = runningProcess;
            runningProcess = null;
            System.out.println("[EDF][T=" + cycle + "] Preemption: "
                    + preempted.getProcessName() + "(dl=" + preempted.getRemainingDeadline()
                    + ") → " + earliest.getProcessName()
                    + "(dl=" + earliest.getRemainingDeadline() + ")");
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
    
    
    private PCB findEarliestDeadline() {
        PCB earliest = null;
        int min = Integer.MAX_VALUE;
        Nodo<PCB> node = readyList.getCabeza();
        while (node != null) {
            if (node.getDato().getRemainingDeadline() < min) {
                min = node.getDato().getRemainingDeadline();
                earliest = node.getDato();
            }
            node = node.getSiguiente();
        }
        return earliest;
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
            System.out.println("[EDF] Bloqueado: " + process.getProcessName());
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
            System.out.println("[EDF] !! Fallo de Deadline: " + process.getProcessName());
            Interfaces.InterfazHome.logEvento("[Deadline] Fallo de Deadline en Proceso " + process.getProcessName() + " [" + process.getProcessID() + "]");
        }
        System.out.println("[EDF] Terminado: " + process.getProcessName()
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
            System.out.println("[EDF] Desbloqueado → listo: " + process.getProcessName());
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

    /** Detecta y loguea fallos de deadline en procesos activos. */
    private void checkDeadlineFailures(int cycle) {
        if (runningProcess != null && runningProcess.getRemainingDeadline() <= 0
                && !runningProcess.isCompleted() && !runningProcess.isMissedDeadline()) {
            runningProcess.setMissedDeadline(true);
            System.out.println("[EDF][T=" + cycle + "] !! Fallo de Deadline: "
                    + runningProcess.getProcessName());
        }
        Nodo<PCB> node = readyList.getCabeza();
        while (node != null) {
            PCB p = node.getDato();
            if (p.getRemainingDeadline() <= 0 && !p.isMissedDeadline()) {
                p.setMissedDeadline(true);
                System.out.println("[EDF][T=" + cycle + "] !! Fallo de Deadline (en cola): "
                        + p.getProcessName());
            }
            node = node.getSiguiente();
        }
    }
    
    public void ejecutar() {
        Lista<Proceso> lista = new Lista<>();
        lista.agregarAlFinal(new Proceso("P1", 3, 7)); // Debe terminar en T=7
        lista.agregarAlFinal(new Proceso("P2", 2, 4)); // Debe terminar en T=4

        int tiempoActual = 0;
        int completados = 0;

        while (completados < lista.size()) {
            Proceso urgente = null;
            int deadlineCercano = Integer.MAX_VALUE;

            Nodo<Proceso> aux = lista.getCabeza();
            while (aux != null) {
                Proceso p = aux.getDato();
                if (p.tiempoRestante > 0 && p.prioridad < deadlineCercano) {
                    deadlineCercano = p.prioridad;
                    urgente = p;
                }
                aux = aux.getSiguiente();
            }

            if (urgente != null) {
                System.out.println("[T=" + tiempoActual + "] EDF: " + urgente.nombre);
                urgente.tiempoRestante--;
                tiempoActual++;
                if (urgente.tiempoRestante == 0) {
                    completados++;
                    String status = (tiempoActual <= urgente.prioridad) ? "A TIEMPO" : "TARDE";
                    System.out.println("   >> Finalizado " + status);
                }
            }
        }
    }
    
    
    
    //getter y setter

    public PCB getRunningProcess(){ return runningProcess; }
    public int getReadyQueueSize(){ return readyList.size(); }
    public boolean hasReadyProcesses(){ return !readyList.estaVacia(); }
    public int getCompletedCount(){ return completedCount; }
    public int getMissedDeadlines(){ return missedDeadlines; }
    public Lista<PCB> getTerminatedList(){ return terminatedList; }
    public Lista<PCB> getBlockedList(){ return blockedList; }
    public Lista<PCB> getReadyList(){ return readyList; }

    public double getMissionSuccessRate(){
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
