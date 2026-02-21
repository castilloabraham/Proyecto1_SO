/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

import Estructuras.Lista;
import Estructuras.Nodo;
import clock.ClockListener;
import clock.ResourceManager;
import Process.PCB;


/**
 *
 * @author Freya Blanca
 */
public class Mediumtermscheduler implements ClockListener {

    private final Lista<PCB> readySuspendedList;
    private final Lista<PCB> blockedSuspendedList;

    private int maxProcessesInMemory;
    private static final int DEFAULT_MAX = 5;
    private int processesInMemory;

    // Referencia directa al SchedulerManager para swap in
    // Se usa Object para evitar dependencia circular en la compilación.
    // Se llama via reflexión o se castea — ver setSchedulerManager().
    private Object schedulerManager;

    private final ResourceManager rm;

    private int swapOutCount;
    private int swapInCount;

    // ===================== Constructor =====================

    public Mediumtermscheduler(int maxInMemory) {
        this.maxProcessesInMemory = Math.max(1, maxInMemory);
        this.readySuspendedList   = new Lista<>();
        this.blockedSuspendedList = new Lista<>();
        this.rm                   = ResourceManager.getInstance();
    }

    public Mediumtermscheduler() {
        this(DEFAULT_MAX);
    }

    /**
     * Inyecta el SchedulerManager después de construirlo.
     * Se llama desde el propio SchedulerManager en su constructor.
     */
    public void setSchedulerManager(Object sm) {
        this.schedulerManager = sm;
    }

    // ===================== API pública =====================

    /**
     * El SchedulerManager llama esto cada vez que admite un proceso a memoria.
     */
    public void notifyProcessAdmitted(int cycle) {
        processesInMemory++;
        System.out.println("[MTS] Procesos en memoria: " + processesInMemory
                + "/" + maxProcessesInMemory);
    }

    /**
     * El SchedulerManager llama esto cuando un proceso termina o se elimina.
     */
    public void notifyProcessRemoved(int cycle) {
        processesInMemory = Math.max(0, processesInMemory - 1);
        if (processesInMemory < maxProcessesInMemory && hasSuspendedProcesses()) {
            swapIn(cycle);
        }
    }

    /**
     * Suspende un proceso específico (llamado desde SchedulerManager
     * cuando la memoria está llena y ya eligió al candidato).
     */
    public void suspendProcess(PCB process, int cycle) {
        ProcessState state = process.getCurrentState();
        if (state == ProcessState.BLOCKED) {
            process.changeState(ProcessState.BLOCKED_SUSPENDED);
            blockedSuspendedList.agregarAlFinal(process);
            processesInMemory = Math.max(0, processesInMemory - 1);
            swapOutCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap OUT (bloqueado): "
                    + process.getProcessName()
                    + " | Deadline: " + process.getRemainingDeadline());
        } else if (state == ProcessState.READY) {
            process.changeState(ProcessState.READY_SUSPENDED);
            readySuspendedList.agregarAlFinal(process);
            processesInMemory = Math.max(0, processesInMemory - 1);
            swapOutCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap OUT (listo): "
                    + process.getProcessName()
                    + " | Deadline: " + process.getRemainingDeadline());
        }
    }

    // ===================== ClockListener =====================

    @Override
    public void onClockTick(int currentCycle) {
        processBlockedSuspendedIO(currentCycle);
        if (processesInMemory < maxProcessesInMemory && hasSuspendedProcesses()) {
            swapIn(currentCycle);
        }
    }

    @Override
    public String getListenerName() { return "MediumTermScheduler"; }

    @Override
    public int getPriority() { return 5; }

    // ===================== Swap In =====================

    private void swapIn(int cycle) {
        // Primero READY_SUSPENDED → va directo a READY
        PCB candidate = findMostUrgent(readySuspendedList);
        if (candidate != null) {
            rm.acquireReadySuspended();
            readySuspendedList.eliminar(candidate);
            rm.releaseReadySuspended();

            candidate.changeState(ProcessState.READY);
            processesInMemory++;
            swapInCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap IN (→ listo): "
                    + candidate.getProcessName()
                    + " | Deadline: " + candidate.getRemainingDeadline());

            // Avisar al SchedulerManager directamente
            if (schedulerManager instanceof Politicas.SchedulerManager) {
                ((Politicas.SchedulerManager) schedulerManager).admitProcess(candidate);
            }
            return;
        }

        // BLOCKED_SUSPENDED → vuelve a BLOCKED
        candidate = findMostUrgent(blockedSuspendedList);
        if (candidate != null) {
            rm.acquireBlockedSuspended();
            blockedSuspendedList.eliminar(candidate);
            rm.releaseBlockedSuspended();

            candidate.changeState(ProcessState.BLOCKED);
            processesInMemory++;
            swapInCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap IN (→ bloqueado): "
                    + candidate.getProcessName()
                    + " | Deadline: " + candidate.getRemainingDeadline());

            // El scheduler lo manejará cuando termine su E/S
            if (schedulerManager instanceof Politicas.SchedulerManager) {
                ((Politicas.SchedulerManager) schedulerManager).onProcessUnblocked(candidate);
            }
        }
    }

    // ===================== E/S de BLOCKED_SUSPENDED =====================

    private void processBlockedSuspendedIO(int cycle) {
        if (blockedSuspendedList.estaVacia()) return;

        Lista<PCB> toPromote = new Lista<>();
        Nodo<PCB> node = blockedSuspendedList.getCabeza();
        while (node != null) {
            if (node.getDato().processIOCycle()) {
                toPromote.agregarAlFinal(node.getDato());
            }
            node = node.getSiguiente();
        }

        Nodo<PCB> p = toPromote.getCabeza();
        while (p != null) {
            PCB proc = p.getDato();
            blockedSuspendedList.eliminar(proc);
            proc.changeState(ProcessState.READY_SUSPENDED);
            readySuspendedList.agregarAlFinal(proc);
            System.out.println("[MTS][T=" + cycle + "] BLOCKED_SUSPENDED → READY_SUSPENDED: "
                    + proc.getProcessName());
            p = p.getSiguiente();
        }
    }

    // ===================== Búsqueda =====================

    private PCB findMostUrgent(Lista<PCB> list) {
        PCB best = null;
        int min = Integer.MAX_VALUE;
        Nodo<PCB> node = list.getCabeza();
        while (node != null) {
            if (node.getDato().getRemainingDeadline() < min) {
                min  = node.getDato().getRemainingDeadline();
                best = node.getDato();
            }
            node = node.getSiguiente();
        }
        return best;
    }

    private boolean hasSuspendedProcesses() {
        return !readySuspendedList.estaVacia() || !blockedSuspendedList.estaVacia();
    }

    // ===================== Getters para UI =====================

    public Lista<PCB> getReadySuspendedList()    { return readySuspendedList; }
    public Lista<PCB> getBlockedSuspendedList()  { return blockedSuspendedList; }
    public int getReadySuspendedCount()          { return readySuspendedList.size(); }
    public int getBlockedSuspendedCount()        { return blockedSuspendedList.size(); }
    public int getSwapOutCount()                 { return swapOutCount; }
    public int getSwapInCount()                  { return swapInCount; }
    public int getMaxProcessesInMemory()         { return maxProcessesInMemory; }
    public int getProcessesInMemory()            { return processesInMemory; }

    public void setMaxProcessesInMemory(int max) {
        this.maxProcessesInMemory = Math.max(1, max);
    }

    public void reset() {
        while (!readySuspendedList.estaVacia())   readySuspendedList.eliminar(0);
        while (!blockedSuspendedList.estaVacia()) blockedSuspendedList.eliminar(0);
        processesInMemory = 0;
        swapOutCount      = 0;
        swapInCount       = 0;
        System.out.println("[MTS] Reiniciado.");
    }
}
