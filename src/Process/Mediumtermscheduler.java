/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

import Estructuras.Lista;
import Estructuras.Nodo;
import clock.ClockListener;
import clock.ResourceManager;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Freya Blanca
 */
public class Mediumtermscheduler implements ClockListener {

    private final Lista<PCB> readySuspendedList;
    private final Lista<PCB> blockedSuspendedList;

    private int maxProcessesInMemory;
    private static final int DEFAULT_MAX = 5;
    private final AtomicInteger processesInMemory = new AtomicInteger(0);

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

    public void setSchedulerManager(Object sm) {
        this.schedulerManager = sm;
    }

    // ===================== API pública =====================

    public void notifyProcessAdmitted(int cycle) {
        processesInMemory.incrementAndGet();
        System.out.println("[MTS] Procesos en memoria: " + processesInMemory.get()
                + "/" + maxProcessesInMemory);
    }

    public void notifyProcessRemoved(int cycle) {
        processesInMemory.updateAndGet(v -> Math.max(0, v - 1));
        if (processesInMemory.get() < maxProcessesInMemory && hasSuspendedProcesses()) {
            swapIn(cycle);
        }
    }

    public void suspendProcess(PCB process, int cycle) {
        if (process.getCurrentState() == ProcessState.RUNNING) {
            System.out.println("[MTS] No se suspende proceso en CPU: " + process.getProcessName());
            return;
        }
        if (process.getCurrentState() == ProcessState.READY_SUSPENDED) return;
        if (process.getCurrentState() == ProcessState.BLOCKED_SUSPENDED) return;

        ProcessState state = process.getCurrentState();

        if (state == ProcessState.NEW) {
            process.changeState(ProcessState.READY);
            state = ProcessState.READY;
        }

        if (state == ProcessState.BLOCKED) {
            process.changeState(ProcessState.BLOCKED_SUSPENDED);
            blockedSuspendedList.agregarAlFinal(process);
            processesInMemory.updateAndGet(v -> Math.max(0, v - 1));
            swapOutCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap OUT (BLOQUEADO→SUSPENDIDO): " + process.getProcessName());
        } else if (state == ProcessState.READY) {
            process.changeState(ProcessState.READY_SUSPENDED);
            readySuspendedList.agregarAlFinal(process);
            processesInMemory.updateAndGet(v -> Math.max(0, v - 1));
            swapOutCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap OUT (LISTO→SUSPENDIDO): " + process.getProcessName());
        } else {
            System.out.println("[MTS] No se puede suspender proceso en estado: " + state + " | " + process.getProcessName());
        }
    }

    // ===================== ClockListener =====================

    @Override
    public void onClockTick(int currentCycle) {
        processBlockedSuspendedIO(currentCycle);
        if (processesInMemory.get() < maxProcessesInMemory && hasSuspendedProcesses()) {
            swapIn(currentCycle);
        }
    }

    @Override
    public String getListenerName() { return "MediumTermScheduler"; }

    @Override
    public int getPriority() { return 5; }

    // ===================== Swap In =====================

    private void swapIn(int cycle) {
        PCB candidate = findMostUrgent(readySuspendedList);
        if (candidate != null) {
            rm.acquireReadySuspended();
            readySuspendedList.eliminar(candidate);
            rm.releaseReadySuspended();
            candidate.changeState(ProcessState.READY);
            processesInMemory.incrementAndGet();
            swapInCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap IN (→ listo): "
                    + candidate.getProcessName()
                    + " | Deadline: " + candidate.getRemainingDeadline());
            if (schedulerManager instanceof Politicas.SchedulerManager) {
                ((Politicas.SchedulerManager) schedulerManager).admitToActiveScheduler(candidate);
            }
            return;
        }

        candidate = findMostUrgent(blockedSuspendedList);
        if (candidate != null) {
            rm.acquireBlockedSuspended();
            blockedSuspendedList.eliminar(candidate);
            rm.releaseBlockedSuspended();
            candidate.changeState(ProcessState.BLOCKED);
            processesInMemory.incrementAndGet();
            swapInCount++;
            System.out.println("[MTS][T=" + cycle + "] Swap IN (→ bloqueado): "
                    + candidate.getProcessName()
                    + " | Deadline: " + candidate.getRemainingDeadline());
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
    public int getProcessesInMemory()            { return processesInMemory.get(); }

    public void setMaxProcessesInMemory(int max) {
        this.maxProcessesInMemory = Math.max(1, max);
    }

    public void reset() {
        while (!readySuspendedList.estaVacia())   readySuspendedList.eliminar(0);
        while (!blockedSuspendedList.estaVacia()) blockedSuspendedList.eliminar(0);
        processesInMemory.set(0);
        swapOutCount = 0;
        swapInCount  = 0;
        System.out.println("[MTS] Reiniciado.");
    }
}