/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Process.PCB;
import clock.SystemClock;
import clock.ClockListener;

/**
 *
 * @author Freaya Blanca
 */
public class SchedulerManager {
    public enum Policy {
        FCFS, ROUND_ROBIN, SRT, PEP, EDF
    }

    // scheduler activo es un ClockListener con metodos comunes
    private ClockListener activeScheduler;
    private Policy activePolicy;
    private final SystemClock clock;
    private int quantum;

    public SchedulerManager(SystemClock clock) {
        this.clock   = clock;
        this.quantum = 3;
        setPolicy(Policy.FCFS); // Política por defecto
    }

    // ===================== Cambio de política =====================

    /**
     * Cambia la política de planificación.
     * Desregistra la anterior del reloj y registra la nueva.
     */
    public void setPolicy(Policy policy) {
        if (activeScheduler != null) {
            clock.removeListener(activeScheduler);
        }

        switch (policy) {
            case FCFS:        activeScheduler = new FCFS();             break;
            case ROUND_ROBIN: activeScheduler = new RoundRobin(quantum); break;
            case SRT:         activeScheduler = new SRT();              break;
            case PEP:         activeScheduler = new PEP();              break;
            case EDF:         activeScheduler = new EDF();              break;
            default:          activeScheduler = new FCFS();
        }

        activePolicy = policy;
        clock.addListener(activeScheduler);
        System.out.println("[SchedulerManager] Política activa: " + policy);
    }

    // ===================== Admisión de procesos =====================

    /**
     * Admite un proceso en el scheduler activo.
     */
    public void admitProcess(PCB process) {
        if (activeScheduler == null) return;
        switch (activePolicy) {
            case FCFS:        ((FCFS)        activeScheduler).admitProcess(process); break;
            case ROUND_ROBIN: ((RoundRobin)  activeScheduler).admitProcess(process); break;
            case SRT:         ((SRT)         activeScheduler).admitProcess(process); break;
            case PEP:         ((PEP)         activeScheduler).admitProcess(process); break;
            case EDF:         ((EDF)         activeScheduler).admitProcess(process); break;
        }
    }

    /**
     * Notifica que un proceso terminó su E/S.
     */
    public void onProcessUnblocked(PCB process) {
        if (activeScheduler == null) return;
        switch (activePolicy) {
            case FCFS:        ((FCFS)        activeScheduler).onProcessUnblocked(process); break;
            case ROUND_ROBIN: ((RoundRobin)  activeScheduler).onProcessUnblocked(process); break;
            case SRT:         ((SRT)         activeScheduler).onProcessUnblocked(process); break;
            case PEP:         ((PEP)         activeScheduler).onProcessUnblocked(process); break;
            case EDF:         ((EDF)         activeScheduler).onProcessUnblocked(process); break;
        }
    }

    // ===================== Getters para la UI =====================

    public PCB getRunningProcess() {
        if (activeScheduler == null) return null;
        switch (activePolicy) {
            case FCFS:        return ((FCFS)        activeScheduler).getRunningProcess();
            case ROUND_ROBIN: return ((RoundRobin)  activeScheduler).getRunningProcess();
            case SRT:         return ((SRT)         activeScheduler).getRunningProcess();
            case PEP:         return ((PEP)         activeScheduler).getRunningProcess();
            case EDF:         return ((EDF)         activeScheduler).getRunningProcess();
            default:          return null;
        }
    }

    public int getReadyQueueSize() {
        if (activeScheduler == null) return 0;
        switch (activePolicy) {
            case FCFS:        return ((FCFS)        activeScheduler).getReadyQueueSize();
            case ROUND_ROBIN: return ((RoundRobin)  activeScheduler).getReadyQueueSize();
            case SRT:         return ((SRT)         activeScheduler).getReadyQueueSize();
            case PEP:         return ((PEP)         activeScheduler).getReadyQueueSize();
            case EDF:         return ((EDF)         activeScheduler).getReadyQueueSize();
            default:          return 0;
        }
    }

    public double getMissionSuccessRate() {
        if (activeScheduler == null) return 100.0;
        switch (activePolicy) {
            case FCFS:        return ((FCFS)        activeScheduler).getMissionSuccessRate();
            case ROUND_ROBIN: return ((RoundRobin)  activeScheduler).getMissionSuccessRate();
            case SRT:         return ((SRT)         activeScheduler).getMissionSuccessRate();
            case PEP:         return ((PEP)         activeScheduler).getMissionSuccessRate();
            case EDF:         return ((EDF)         activeScheduler).getMissionSuccessRate();
            default:          return 100.0;
        }
    }

    public Policy getActivePolicy()          { return activePolicy; }
    public ClockListener getActiveScheduler(){ return activeScheduler; }
    public int getQuantum()                  { return quantum; }

    public void setQuantum(int q) {
        this.quantum = Math.max(1, q);
        if (activePolicy == Policy.ROUND_ROBIN) setPolicy(Policy.ROUND_ROBIN);
    }

    public void reset() {
        if (activeScheduler == null) return;
        switch (activePolicy) {
            case FCFS:        ((FCFS)        activeScheduler).reset(); break;
            case ROUND_ROBIN: ((RoundRobin)  activeScheduler).reset(); break;
            case SRT:         ((SRT)         activeScheduler).reset(); break;
            case PEP:         ((PEP)         activeScheduler).reset(); break;
            case EDF:         ((EDF)         activeScheduler).reset(); break;
        }
    }
}
