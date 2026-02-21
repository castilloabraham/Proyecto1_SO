/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clock;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Freya Blanca
 */
public class ResourceManager {
     // Semaforos
    
    private final Semaphore readyQueueMutex; //Semaforo para cuidar cola de procesos listos
    private final Semaphore blockedQueueMutex; //Para proteger procesos bloqueados
    private final Semaphore readySuspendedMutex; //Para proteger procesos listos-suspendidos
    private final Semaphore blockedSuspendedMutex; //Para proteger procesos bloqueados-suspendidos
    private final Semaphore terminatedQueueMutex; // Para proteger la lista de procesos terminados
    private final Semaphore newQueueMutex; // Para proteger la cola de procesos nuevos, los que estan esperando a ser admitidos
    private final Semaphore cpuMutex; // Semáforo para el CPU permite solo 1 proceso puede ejecutar a la vez
    private final Semaphore ioDevicesSemaphore; // Semáforo para operaciones de E/S
    
    // Locks
    
    private final ReentrantLock contextSwitchLock; //Para proteger el cambio de contexto. Garantiza que solo un hilo pueda realizar context switch a la vez.
    private final ReentrantLock metricsLock; //Para proteger las métricas del sistema.
    private final ReentrantLock schedulerLock; //Para planificador, evita que se ejecute el planificador mientras ya está en ejecución (re-entrancia).
    
    // Configuracion general
    
    private final int ioDevicesCount; //Num de dispositivos de E/S
    private static final long ACQUIRE_TIMEOUT_MS = 5000; //Timeout para operaciones de adquisicion
    private static ResourceManager instance; //Instancia única
    
    // Estdisitca
    
    private long contentionCount; //Contador de bloqueos por contención
    private long timeoutCount; //Contador de timeouts
    
    
    /**
     * Constructor - Singleton
     * 
     * @param ioDevicesCount Número de dispositivos de E/S simultáneos
     */
    private ResourceManager(int ioDevicesCount) {
        this.ioDevicesCount = ioDevicesCount;
        
        // Inicializar semáforos mutex (1 permiso = exclusión mutua)
        this.readyQueueMutex = new Semaphore(1, true); // true = fair (FIFO)
        this.blockedQueueMutex = new Semaphore(1, true);
        this.readySuspendedMutex = new Semaphore(1, true);
        this.blockedSuspendedMutex = new Semaphore(1, true);
        this.terminatedQueueMutex = new Semaphore(1, true);
        this.newQueueMutex = new Semaphore(1, true);
        this.cpuMutex = new Semaphore(1, true);
        
        // Semáforo para E/S (permite múltiples accesos)
        this.ioDevicesSemaphore = new Semaphore(ioDevicesCount, true);
        
        // Inicializar locks
        this.contextSwitchLock = new ReentrantLock(true);
        this.metricsLock = new ReentrantLock(true);
        this.schedulerLock = new ReentrantLock(true);
        
        // Estadísticas
        this.contentionCount = 0;
        this.timeoutCount = 0;
        
        System.out.println("[ResourceManager] Inicializado con " + ioDevicesCount + " dispositivos I/O");
    }
    
    
    
    /**
     * Obtiene la instancia única del ResourceManager
     * 
     * @return Instancia del ResourceManager
     */
    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager(4); // 4 dispositivos I/O por defecto
        }
        return instance;
    }
    
    /**
     * Obtiene la instancia con configuración personalizada
     * 
     * @param ioDevicesCount Número de dispositivos I/O
     * @return Instancia del ResourceManager
     */
    public static synchronized ResourceManager getInstance(int ioDevicesCount) {
        if (instance == null) {
            instance = new ResourceManager(ioDevicesCount);
        }
        return instance;
    }
    
    
    
    // Metodos de acceso a colas
    
    /**
     * Adquiere el mutex de la cola de listos
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireReadyQueue() {
        return acquireSemaphore(readyQueueMutex, "ReadyQueue");
    }
    
    /**
     * Libera el mutex de la cola de listos
     */
    public void releaseReadyQueue() {
        readyQueueMutex.release();
    }
    
    /**
     * Adquiere el mutex de la cola de bloqueados
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireBlockedQueue() {
        return acquireSemaphore(blockedQueueMutex, "BlockedQueue");
    }
    
    /**
     * Libera el mutex de la cola de bloqueados
     */
    public void releaseBlockedQueue() {
        blockedQueueMutex.release();
    }
    
    /**
     * Adquiere el mutex de la cola de listos suspendidos
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireReadySuspended() {
        return acquireSemaphore(readySuspendedMutex, "ReadySuspended");
    }
    
    /**
     * Libera el mutex de la cola de listos suspendidos
     */
    public void releaseReadySuspended() {
        readySuspendedMutex.release();
    }
    
    /**
     * Adquiere el mutex de la cola de bloqueados suspendidos
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireBlockedSuspended() {
        return acquireSemaphore(blockedSuspendedMutex, "BlockedSuspended");
    }
    
    /**
     * Libera el mutex de la cola de bloqueados suspendidos
     */
    public void releaseBlockedSuspended() {
        blockedSuspendedMutex.release();
    }
    
    /**
     * Adquiere el mutex de la cola de terminados
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireTerminatedQueue() {
        return acquireSemaphore(terminatedQueueMutex, "TerminatedQueue");
    }
    
    /**
     * Libera el mutex de la cola de terminados
     */
    public void releaseTerminatedQueue() {
        terminatedQueueMutex.release();
    }
    
    /**
     * Adquiere el mutex de la cola de nuevos
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireNewQueue() {
        return acquireSemaphore(newQueueMutex, "NewQueue");
    }
    
    /**
     * Libera el mutex de la cola de nuevos
     */
    public void releaseNewQueue() {
        newQueueMutex.release();
    }
    
    // Acceso al cpu
    
    /**
     * Adquiere el CPU para un proceso
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireCPU() {
        return acquireSemaphore(cpuMutex, "CPU");
    }
    
    /**
     * Libera el CPU
     */
    public void releaseCPU() {
        cpuMutex.release();
    }
    
    /**
     * Verifica si el CPU está disponible
     * 
     * @return true si está disponible
     */
    public boolean isCPUAvailable() {
        return cpuMutex.availablePermits() > 0;
    }
    
    // Acceso a dispositivo E/S
    
    /**
     * Adquiere un dispositivo de E/S
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireIODevice() {
        return acquireSemaphore(ioDevicesSemaphore, "IODevice");
    }
    
    /**
     * Libera un dispositivo de E/S
     */
    public void releaseIODevice() {
        ioDevicesSemaphore.release();
    }
    
    /**
     * Obtiene el número de dispositivos I/O disponibles
     * 
     * @return Dispositivos libres
     */
    public int getAvailableIODevices() {
        return ioDevicesSemaphore.availablePermits();
    }
    
    // locks de control
    
    /**
     * Adquiere el lock de cambio de contexto
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireContextSwitchLock() {
        try {
            boolean acquired = contextSwitchLock.tryLock(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                timeoutCount++;
                System.err.println("[ResourceManager] Timeout adquiriendo ContextSwitchLock");
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Libera el lock de cambio de contexto
     */
    public void releaseContextSwitchLock() {
        if (contextSwitchLock.isHeldByCurrentThread()) {
            contextSwitchLock.unlock();
        }
    }
    
    /**
     * Adquiere el lock del planificador
     *
     * @return true si se adquirió exitosamente
     */
    public boolean acquireSchedulerLock() {
        try {
            boolean acquired = schedulerLock.tryLock(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                timeoutCount++;
                System.err.println("[ResourceManager] Timeout adquiriendo SchedulerLock");
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Libera el lock del planificador
     */
    public void releaseSchedulerLock() {
        if (schedulerLock.isHeldByCurrentThread()) {
            schedulerLock.unlock();
        }
    }
    
    /**
     * Adquiere el lock de metricas
     * 
     * @return true si se adquirió exitosamente
     */
    public boolean acquireMetricsLock() {
        try {
            boolean acquired = metricsLock.tryLock(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                timeoutCount++;
                System.err.println("[ResourceManager] Timeout adquiriendo MetricsLock");
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Libera el lock de metricas
     */
    public void releaseMetricsLock() {
        if (metricsLock.isHeldByCurrentThread()) {
            metricsLock.unlock();
        }
    }
    
    // Metodo auxiliar
    
    /**
     * Metodo auxiliar para adquirir un semáforo con timeout
     * 
     * @param semaphore Semáforo a adquirir
     * @param resourceName Nombre del recurso (para logging)
     * @return true si se adquirió exitosamente
     */
    private boolean acquireSemaphore(Semaphore semaphore, String resourceName) {
        try {
            boolean acquired = semaphore.tryAcquire(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!acquired) {
                timeoutCount++;
                System.err.println("[ResourceManager] Timeout adquiriendo " + resourceName);
            } else if (semaphore.hasQueuedThreads()) {
                contentionCount++;
            }
            
            return acquired;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ResourceManager] Interrumpido adquiriendo " + resourceName);
            return false;
        }
    }
    
    // Metodos de unidad
    
    /**
     * Libera todos los recursos (para emergencias o reset).
     */
    public void releaseAll() {
        readyQueueMutex.release();
        blockedQueueMutex.release();
        readySuspendedMutex.release();
        blockedSuspendedMutex.release();
        terminatedQueueMutex.release();
        newQueueMutex.release();
        cpuMutex.release();
        
        // Liberar locks si están held
        if (contextSwitchLock.isHeldByCurrentThread()) {
            contextSwitchLock.unlock();
        }
        if (schedulerLock.isHeldByCurrentThread()) {
            schedulerLock.unlock();
        }
        if (metricsLock.isHeldByCurrentThread()) {
            metricsLock.unlock();
        }
        
        System.out.println("[ResourceManager] Todos los recursos liberados");
    }
    
    /**
     * Reinicia las estadisticas
     */
    public void resetStatistics() {
        contentionCount = 0;
        timeoutCount = 0;
    }
    
    /**
     * Obtiene el número de bloqueos por contención
     * 
     * @return Contador de contenciones
     */
    public long getContentionCount() {
        return contentionCount;
    }
    
    /**
     * Obtiene el número de timeouts
     * 
     * @return Contador de timeouts
     */
    public long getTimeoutCount() {
        return timeoutCount;
    }
    
    /**
     * Obtiene información del estado de los recursos
     * 
     * @return String con información detallada
     */
    public String getResourceStatus() {
        return String.format(
            "ResourceManager Status:\n" +
            "  CPU disponible: %s\n" +
            "  Dispositivos I/O disponibles: %d/%d\n" +
            "  Contenciones: %d\n" +
            "  Timeouts: %d\n" +
            "  ContextSwitch lock held: %s\n" +
            "  Scheduler lock held: %s",
            isCPUAvailable() ? "si" : "no",
            getAvailableIODevices(), ioDevicesCount,
            contentionCount,
            timeoutCount,
            contextSwitchLock.isLocked(),
            schedulerLock.isLocked()
        );
    }
    
    public String toString() {
        return String.format("ResourceManager[IO_Devices=%d, CPU_Available=%s]",
                           ioDevicesCount, isCPUAvailable());
    }
    
    
}
