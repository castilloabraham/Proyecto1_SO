/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Process.PCB;
import Process.Mediumtermscheduler;
import Process.ProcessState;
import clock.SystemClock;
import clock.ClockListener;
import Estructuras.Nodo;

/**
 *
 * @author Freaya Blanca
 */
public class SchedulerManager {

    public enum Policy {
        FCFS, ROUND_ROBIN, SRT, PEP, EDF
    }

    private ClockListener activeScheduler;
    private Policy activePolicy;
    private final SystemClock clock;
    private int quantum;
    private final Mediumtermscheduler mts;

    // ===================== Constructor =====================

    public SchedulerManager(SystemClock clock) {
        this(clock, 5);
    }

    public SchedulerManager(SystemClock clock, int maxProcessesInMemory) {
        this.clock   = clock;
        this.quantum = 3;

        // Crear MTS y darle referencia a este SchedulerManager
        this.mts = new Mediumtermscheduler(maxProcessesInMemory);
        this.mts.setSchedulerManager(this);

        // Registrar MTS en el reloj (prioridad 5, antes que los schedulers)
        clock.addListener(mts);

        setPolicy(Policy.FCFS);
    }

    // ===================== Cambio de política =====================

    public void setPolicy(Policy policy) {
        if (activeScheduler != null) {
            clock.removeListener(activeScheduler);
        }
        switch (policy) {
            case FCFS:        activeScheduler = new FCFS();              break;
            case ROUND_ROBIN: activeScheduler = new RoundRobin(quantum); break;
            case SRT:         activeScheduler = new SRT();               break;
            case PEP:         activeScheduler = new PEP();               break;
            case EDF:         activeScheduler = new EDF();               break;
            default:          activeScheduler = new FCFS();
        }
        activePolicy = policy;
        clock.addListener(activeScheduler);
        System.out.println("[SchedulerManager] Política: " + policy);
    }

    // ===================== Admisión =====================

    /**
     * Punto de entrada único para admitir procesos.
     * Si la memoria está llena, intenta suspender al menos urgente UNA sola vez.
     * Si no hay candidato, el nuevo proceso va directo a suspendido.
     */
    public void admitProcess(PCB process) {
        if (process == null) return;

        if (mts.getProcessesInMemory() >= mts.getMaxProcessesInMemory()) {
            PCB running = getRunningProcess();
            PCB toSuspend = findLeastUrgentInReady();

            if (toSuspend != null && toSuspend != running) {
                removeFromActiveScheduler(toSuspend);
                mts.suspendProcess(toSuspend, clock.getCurrentCycle());
                // LOG: proceso movido a suspendido
                Interfaces.InterfazHome.logEvento("[MTS] Proceso " + toSuspend.getProcessName() +
                    " movido a Listo-Suspendido (memoria llena)");
                admitToActiveScheduler(process);
                mts.notifyProcessAdmitted(clock.getCurrentCycle());
            } else {
                // Sin candidato: nuevo proceso va a READY_SUSPENDED
                process.changeState(ProcessState.READY);
                process.changeState(ProcessState.READY_SUSPENDED);
                mts.getReadySuspendedList().agregarAlFinal(process);
                Interfaces.InterfazHome.logEvento("[MTS] Proceso " + process.getProcessName() +
                    " enviado directo a Listo-Suspendido (sin candidato)");
            }
        } else {
            admitToActiveScheduler(process);
            mts.notifyProcessAdmitted(clock.getCurrentCycle());
        }
    }

    /**
     * Busca el proceso menos urgente (mayor deadline) en la cola de listos.
     * Cada scheduler expone su lista para que podamos consultarla.
     */
    private PCB findLeastUrgentInReady() {
        PCB worst = null;
        int maxDeadline = Integer.MIN_VALUE;

        Estructuras.Nodo<PCB> head = getReadyHead();
        if (head == null) return null;

        PCB running = getRunningProcess(); // No suspender el que está en CPU

        for (Estructuras.Nodo<PCB> node = head; node != null; node = node.getSiguiente()) {
            PCB p = node.getDato();
            if (p == running) continue; // Saltar el proceso en CPU
            if (p.getRemainingDeadline() > maxDeadline) {
                maxDeadline = p.getRemainingDeadline();
                worst = p;
            }
        }
        return worst;
    }

    private Estructuras.Nodo<PCB> getReadyHead() {
        switch (activePolicy) {
            case FCFS:        return ((FCFS)       activeScheduler).getReadyQueue().getFrente();
            case ROUND_ROBIN: return ((RoundRobin) activeScheduler).getReadyQueue().getFrente();
            case SRT:         return ((SRT)        activeScheduler).getReadyList().getCabeza();
            case PEP:         return ((PEP)        activeScheduler).getReadyList().getCabeza();
            case EDF:         return ((EDF)        activeScheduler).getReadyList().getCabeza();
            default:          return null;
        }
    }

    private Iterable<PCB> getReadyIterable() { return null; } // No se usa, ver getReadyHead

    /**
     * Elimina un proceso de la cola del scheduler activo
     * (para poder suspenderlo).
     */
    private void removeFromActiveScheduler(PCB process) {
        switch (activePolicy) {
            case SRT: ((SRT) activeScheduler).getReadyList().eliminar(process); break;
            case PEP: ((PEP) activeScheduler).getReadyList().eliminar(process); break;
            case EDF: ((EDF) activeScheduler).getReadyList().eliminar(process); break;
            default: break; // ← FCFS y RR no tienen eliminar, el swap-out no funciona para ellos
        }
    }

    public void admitToActiveScheduler(PCB process) {
        switch (activePolicy) {
            case FCFS:        ((FCFS)       activeScheduler).admitProcess(process); break;
            case ROUND_ROBIN: ((RoundRobin) activeScheduler).admitProcess(process); break;
            case SRT:         ((SRT)        activeScheduler).admitProcess(process); break;
            case PEP:         ((PEP)        activeScheduler).admitProcess(process); break;
            case EDF:         ((EDF)        activeScheduler).admitProcess(process); break;
        }
    }

    // ===================== Notificaciones =====================

    public void notifyProcessFinished() {
        mts.notifyProcessRemoved(clock.getCurrentCycle());
    }

    public void onProcessUnblocked(PCB process) {
        switch (activePolicy) {
            case FCFS:        ((FCFS)       activeScheduler).onProcessUnblocked(process); break;
            case ROUND_ROBIN: ((RoundRobin) activeScheduler).onProcessUnblocked(process); break;
            case SRT:         ((SRT)        activeScheduler).onProcessUnblocked(process); break;
            case PEP:         ((PEP)        activeScheduler).onProcessUnblocked(process); break;
            case EDF:         ((EDF)        activeScheduler).onProcessUnblocked(process); break;
        }
    }

    // ===================== Getters para UI =====================

    public PCB getRunningProcess() {
        switch (activePolicy) {
            case FCFS:        return ((FCFS)       activeScheduler).getRunningProcess();
            case ROUND_ROBIN: return ((RoundRobin) activeScheduler).getRunningProcess();
            case SRT:         return ((SRT)        activeScheduler).getRunningProcess();
            case PEP:         return ((PEP)        activeScheduler).getRunningProcess();
            case EDF:         return ((EDF)        activeScheduler).getRunningProcess();
            default:          return null;
        }
    }

    public int getReadyQueueSize() {
        switch (activePolicy) {
            case FCFS:        return ((FCFS)       activeScheduler).getReadyQueueSize();
            case ROUND_ROBIN: return ((RoundRobin) activeScheduler).getReadyQueueSize();
            case SRT:         return ((SRT)        activeScheduler).getReadyQueueSize();
            case PEP:         return ((PEP)        activeScheduler).getReadyQueueSize();
            case EDF:         return ((EDF)        activeScheduler).getReadyQueueSize();
            default:          return 0;
        }
    }

    public double getMissionSuccessRate() {
        switch (activePolicy) {
            case FCFS:        return ((FCFS)       activeScheduler).getMissionSuccessRate();
            case ROUND_ROBIN: return ((RoundRobin) activeScheduler).getMissionSuccessRate();
            case SRT:         return ((SRT)        activeScheduler).getMissionSuccessRate();
            case PEP:         return ((PEP)        activeScheduler).getMissionSuccessRate();
            case EDF:         return ((EDF)        activeScheduler).getMissionSuccessRate();
            default:          return 100.0;
        }
    }

    public Mediumtermscheduler getMTS()       { return mts; }
    public Policy getActivePolicy()           { return activePolicy; }
    public ClockListener getActiveScheduler() { return activeScheduler; }
    public int getQuantum()                   { return quantum; }

    public void setQuantum(int q) {
        this.quantum = Math.max(1, q);
        if (activePolicy == Policy.ROUND_ROBIN) setPolicy(Policy.ROUND_ROBIN);
    }

    public void reset() {
        if (activeScheduler != null) {
            switch (activePolicy) {
                case FCFS:        ((FCFS)       activeScheduler).reset(); break;
                case ROUND_ROBIN: ((RoundRobin) activeScheduler).reset(); break;
                case SRT:         ((SRT)        activeScheduler).reset(); break;
                case PEP:         ((PEP)        activeScheduler).reset(); break;
                case EDF:         ((EDF)        activeScheduler).reset(); break;
            }
        }
        mts.reset();
    }
}