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
public class SchedulerManager implements MediumTermScheduler.SwapInCallback {

    public enum Policy {
        FCFS, ROUND_ROBIN, SRT, PEP, EDF
    }

    private ClockListener activeScheduler;
    private Policy activePolicy;
    private final SystemClock clock;
    private int quantum;

    // Planificador de mediano plazo
    private final MediumTermScheduler mts;

    // ===================== Constructor =====================

    public SchedulerManager(SystemClock clock) {
        this(clock, 5); // 5 procesos en memoria por defecto
    }

    public SchedulerManager(SystemClock clock, int maxProcessesInMemory) {
        this.clock   = clock;
        this.quantum = 3;
        this.mts     = new MediumTermScheduler(this, maxProcessesInMemory);

        // Registrar el MTS en el reloj (prioridad 5, corre antes que los schedulers)
        clock.addListener(mts);

        // Política por defecto
        setPolicy(Policy.FCFS);
    }

    // ===================== Cambio de política =====================

    /**
     * Cambia la política de planificación en tiempo real.
     * Desregistra el scheduler anterior del reloj y registra el nuevo.
     */
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
        System.out.println("[SchedulerManager] Política activa: " + policy);
    }

    // ===================== Admisión de procesos =====================

    /**
     * Punto de entrada único para admitir procesos al sistema.
     * El MTS decide si va directo a memoria o al swap.
     */
    public void admitProcess(PCB process) {
        if (process == null) return;

        // Notificar al MTS que entra un proceso nuevo
        // Si la memoria está llena, el MTS llamará a forceSwapOut
        // sobre el candidato menos urgente
        int currentInMemory = mts.getProcessesInMemory();
        int maxInMemory     = mts.getMaxProcessesInMemory();

        if (currentInMemory >= maxInMemory) {
            // Memoria llena: el proceso nuevo va directo al swap
            process.changeState(ProcessState.READY_SUSPENDED);
            mts.getReadySuspendedList().agregarAlFinal(process);
            System.out.println("[SchedulerManager] Memoria llena → "
                    + process.getProcessName() + " va a READY_SUSPENDED");
        } else {
            // Hay espacio: admitir al scheduler activo
            admitToActiveScheduler(process);
            mts.notifyProcessAdmitted(process, clock.getCurrentCycle());
        }
    }

    /**
     * Delega la admisión al scheduler de corto plazo activo.
     */
    private void admitToActiveScheduler(PCB process) {
        switch (activePolicy) {
            case FCFS:        ((FCFS)       activeScheduler).admitProcess(process); break;
            case ROUND_ROBIN: ((RoundRobin) activeScheduler).admitProcess(process); break;
            case SRT:         ((SRT)        activeScheduler).admitProcess(process); break;
            case PEP:         ((PEP)        activeScheduler).admitProcess(process); break;
            case EDF:         ((EDF)        activeScheduler).admitProcess(process); break;
        }
    }

    // ===================== SwapInCallback =====================

    /**
     * El MTS llama esto cuando hace swap in de un proceso suspendido.
     * Si el proceso quedó en READY, lo readmitimos al scheduler activo.
     * Si quedó en BLOCKED, el scheduler lo manejará cuando termine su E/S.
     */
    @Override
    public void onSwapIn(PCB process) {
        if (process.getCurrentState() == ProcessState.READY) {
            admitToActiveScheduler(process);
        }
        // Si es BLOCKED, el scheduler de corto plazo lo verá en su lista
        // de bloqueados cuando procese la E/S
    }

    // ===================== Notificaciones de ciclo de vida =====================

    /**
     * Llamar cuando un proceso termina (TERMINATED) o es eliminado.
     */
    public void notifyProcessFinished() {
        mts.notifyProcessRemoved(clock.getCurrentCycle());
    }

    /**
     * Llamar cuando un proceso se bloquea por E/S.
     */
    public void notifyProcessBlocked(PCB process) {
        mts.notifyProcessBlocked(process);
    }

    /**
     * Notifica que un proceso terminó su E/S (BLOCKED → READY).
     */
    public void onProcessUnblocked(PCB process) {
        switch (activePolicy) {
            case FCFS:        ((FCFS)       activeScheduler).onProcessUnblocked(process); break;
            case ROUND_ROBIN: ((RoundRobin) activeScheduler).onProcessUnblocked(process); break;
            case SRT:         ((SRT)        activeScheduler).onProcessUnblocked(process); break;
            case PEP:         ((PEP)        activeScheduler).onProcessUnblocked(process); break;
            case EDF:         ((EDF)        activeScheduler).onProcessUnblocked(process); break;
        }
    }

    // ===================== Getters para la UI =====================

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

    public MediumTermScheduler getMTS()          { return mts; }
    public Policy getActivePolicy()              { return activePolicy; }
    public ClockListener getActiveScheduler()    { return activeScheduler; }
    public int getQuantum()                      { return quantum; }

    public void setQuantum(int q) {
        this.quantum = Math.max(1, q);
        if (activePolicy == Policy.ROUND_ROBIN) setPolicy(Policy.ROUND_ROBIN);
    }

    // ===================== Reset =====================

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
        System.out.println("[SchedulerManager] Reiniciado.");
    }
}