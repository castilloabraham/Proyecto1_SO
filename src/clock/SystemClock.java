/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Comparator;


/**
 *
 * @author Freya Blanca
 * 
 * Nota: Todas las medidas de tiempo estan en ms
 */
public class SystemClock extends Thread {
    // Configuracion de velocidad
    
    public static final int MIN_CYCLE_DURATION_MS = 10; //Duración min permitida de un ciclo
    public static final int MAX_CYCLE_DURATION_MS = 5000; //Duración max permitida de un ciclo
    public static final int DEFAULT_CYCLE_DURATION_MS = 1000; //Duracion por defecto de un ciclo
    
    /**
     * Velocidades predefinidas
     */
    public enum Speed {
        VERY_SLOW(2000, "Muy lento (2s/ciclo)"),
        SLOW(1500, "Lento (1.5s/ciclo)"),
        NORMAL(1000, "Normal (1s/ciclo)"),
        FAST(500, "Rapido (0.5s/ciclo)"),
        VERY_FAST(100, "Muy rspido (0.1s/ciclo)"),
        TURBO(50, "Turbo (0.05s/ciclo)");
        
        private final int durationMs;
        private final String description;
        
        Speed(int durationMs, String description) {
            this.durationMs = durationMs;
            this.description = description;
        }
        
        public int getDuration() {
            return durationMs;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Estados del reloj
    
    private final AtomicInteger currentCycle; //Ciclo actual del reloj global (thread-safe)
    private final AtomicBoolean running; //Indica si el reloj está en ejecución
    private final AtomicBoolean shouldStop; //Indica si el hilo debe detenerse completamente
    private final AtomicBoolean paused; //Indica si el reloj esta en pausa
    private volatile int cycleDurationMs; //Duracion actual del ciclo de reloj
    
    // Listeners
    
    /**
     * Lista de componentes suscritos al reloj
     * Sincronizada para evitar ConcurrentModificationException
     */
    private ClockListener[] listeners;
    private int listenerCount;
    private final Object listenersLock; //Lock para sincronizar acceso a la lista de listeners
    
    // Estadisticas

    private long startTimeMs; //Tiempo real cuando inició el reloj
    private long totalTicksExecuted; //Total de ticks ejecutados desde el inicio
    private static SystemClock instance; //Instancia única del reloj (Singleton)
    
    
    private static final int INITIAL_LISTENER_CAPACITY = 10;
    /**
     * Constructor (Singleton pattern).
     */
    private SystemClock() {
        super("SystemClock-Thread");
        this.cycleDurationMs = DEFAULT_CYCLE_DURATION_MS;
        this.currentCycle = new AtomicInteger(0);
        this.running = new AtomicBoolean(false);
        this.shouldStop = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.listeners = new ClockListener[INITIAL_LISTENER_CAPACITY];
        this.listenersLock = new Object();
        this.totalTicksExecuted = 0;
        
        // Configurar como daemon para que no impida el cierre del programa
        setDaemon(true);
    }
    
    
    // Configuración del reloj y gestión de listeners
    
    /**
     * Establece la duración del ciclo de reloj y valida que este dentro de los limites
     * Puede cambiarse en tiempo real mientras el reloj corre.
     * 
     * @param durationMs Duración en milisegundos
     * @return true si el cambio fue exitoso
     */
    public synchronized boolean setCycleDuration(int durationMs) {
        if (durationMs < MIN_CYCLE_DURATION_MS || durationMs > MAX_CYCLE_DURATION_MS) {
            System.err.println("[SystemClock] Duración inválida: " + durationMs + "ms (rango: " + MIN_CYCLE_DURATION_MS + "-" + MAX_CYCLE_DURATION_MS + "ms)");
            return false;
        }
        this.cycleDurationMs = durationMs;
        System.out.println("[SystemClock] Duración de ciclo cambiada a: " + durationMs + "ms");
        return true;
    }
    
    
    
    //Ciclo principal del reloj
    
    /** 
     * Ejecuta indefinidamente hasta que se llame a stopClock():
     * 1. Espera la duración del ciclo
     * 2. Incrementa el contador
     * 3. Notifica a todos los listeners
     */
    public void run() {
        startTimeMs = System.currentTimeMillis();
        System.out.println("[SystemClock] Reloj iniciado - Ciclo: " + currentCycle.get());
        
        while (!shouldStop.get()) {
            try {
                // Si está pausado, esperar
                while (paused.get() && !shouldStop.get()) {
                    Thread.sleep(100); // Check cada 100ms si se reanudó
                }
                
                // Si se debe detener, salir
                if (shouldStop.get()) {
                    break;
                }
                
                // Esperar la duración del ciclo
                Thread.sleep(cycleDurationMs);
                
                // Incrementar ciclo
                int cycle = currentCycle.incrementAndGet();
                totalTicksExecuted++;
                
                // Notificar a todos los listeners
                notifyListeners(cycle);
                
            } catch (InterruptedException e) {
                System.err.println("[SystemClock] Hilo interrumpido: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("[SystemClock] Error en ciclo de reloj: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[SystemClock] Reloj detenido - Ciclos ejecutados: " + totalTicksExecuted);
    }
    
    /**
     * Notifica a todos los listeners del nuevo ciclo, estos se ejecutan en orden de prioridad.
     * 
     * @param cycle Ciclo actual
     */
    private void notifyListeners(int cycle) {
        // Crear una copia del array actual para evitar modificaciones durante la iteración
        ClockListener[] currentListeners;
        int currentCount;

        synchronized (listenersLock) {
            // Hacer una copia de los listeners actuales
            currentListeners = new ClockListener[listenerCount];
            System.arraycopy(listeners, 0, currentListeners, 0, listenerCount);
            currentCount = listenerCount;
        }

        // Notificar a cada listener (ya están ordenados por prioridad)
        for (int i = 0; i < currentCount; i++) {
            ClockListener listener = currentListeners[i];
            if (listener != null) {
                try {
                    listener.onClockTick(cycle);
                } catch (Exception e) {
                    System.err.println("[SystemClock] Error en listener " + listener.getListenerName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    
    
    // Control del reloj
    
    /**
     * Inicia el reloj contando ciclos
     */
    public void startClock() {
        if (running.compareAndSet(false, true)) {
            shouldStop.set(false);
            paused.set(false);
            if (!isAlive()) {
                start(); // Iniciar el hilo
            }
            System.out.println("SystemClock: Reloj iniciado");
        }
    }
    
    /**
     * Pausa el reloj - ATENCION: no destruye el hilo
     */
    public void pauseClock() {
        if (paused.compareAndSet(false, true)) {
            int cycle = currentCycle.get();
            
            // Notificar a listeners
            synchronized (listenersLock) {
                for (ClockListener listener : listeners) {
                    try {
                        listener.onClockPaused(cycle);
                    } catch (Exception e) {
                        System.err.println("SystemClock: Error notificando pausa: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("SystemClock: Reloj pausado en ciclo: " + cycle);
        }
    }
    
    /**
     * Reanuda el reloj después de una pausa.
     */
    public void resumeClock() {
        if (paused.compareAndSet(true, false)) {
            int cycle = currentCycle.get();
            
            // Notificar a listeners
            synchronized (listenersLock) {
                for (ClockListener listener : listeners) {
                    try {
                        listener.onClockResumed(cycle);
                    } catch (Exception e) {
                        System.err.println("SystemClock: Error notificando reanudación: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("SystemClock: Reloj reanudado desde ciclo: " + cycle);
        }
    }
    
    /**
     * Detiene el reloj completamente (termina el hilo).
     * Para reiniciar, se debe crear una nueva instancia.
     */
    public void stopClock() {
        shouldStop.set(true);
        running.set(false);
        System.out.println("SystemClock: Deteniendo reloj...");
        
        // Interrumpir el hilo para salir de sleep()
        if (isAlive()) {
            interrupt();
        }
    }
    
    /**
     * Reinicia el reloj (vuelve el ciclo a 0). Esto resetea todo el sistema.
     */
    public void resetClock() {
        boolean wasRunning = running.get();
        
        // Pausar temporalmente
        pauseClock();
        
        // Resetear contador
        currentCycle.set(0);
        totalTicksExecuted = 0;
        
        // Notificar reset a listeners
        synchronized (listenersLock) {
            for (ClockListener listener : listeners) {
                try {
                    listener.onClockReset();
                } catch (Exception e) {
                    System.err.println("SystemClock: Error notificando reset: " + e.getMessage());
                }
            }
        }
        
        System.out.println("SystemClock: Reloj reiniciado");
        
        // Reanudar si estaba corriendo
        if (wasRunning) {
            resumeClock();
        }
    }
    
    
    // Gestion Listeners
    
    /**
     * Registra un componente para recibir notificaciones del reloj.
     * 
     * @param listener Componente a registrar
     * @return true si se registró exitosamente
     */
    public boolean addListener(ClockListener listener) {
        if (listener == null) {
            return false;
        }
        
        synchronized (listenersLock) {
            // Verificar si ya existe
            for (int i = 0; i < listenerCount; i++) {
                if (listeners[i] != null && listeners[i].equals(listener)) {
                    return false;
                }
            }
            
            // Verificar si necesitamos expandir el array
            if (listenerCount >= listeners.length) {
                ClockListener[] newListeners = new ClockListener[listeners.length * 2];
                System.arraycopy(listeners, 0, newListeners, 0, listeners.length);
                listeners = newListeners;
            }
            
            // Agregar el listener
            listeners[listenerCount] = listener;
            listenerCount++;
            System.out.println("[SystemClock] Listener registrado: " + listener.getListenerName());
            return true;
        }
    }
    
    /**
     * Desregistra un componente
     * 
     * @param listener Componente a desregistrar
     * @return true si se removió exitosamente
     */
    public boolean removeListener(ClockListener listener) {
        synchronized (listenersLock) {
            for (int i = 0; i < listenerCount; i++) {
                if (listeners[i] != null && listeners[i].equals(listener)) {
                    // Desplazar elementos
                    for (int j = i; j < listenerCount - 1; j++) {
                        listeners[j] = listeners[j + 1];
                    }
                    listeners[listenerCount - 1] = null;
                    listenerCount--;
                    System.out.println("[SystemClock] Listener removido: " + listener.getListenerName());
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Obtiene la cantidad de listeners registrados.
     * 
     * @return Número de listeners
     */
    public int getListenerCount() {
        synchronized (listenersLock) {
            return listenerCount;
        }
    }
    
    /**
     * Limpia todos los listeners.
     */
    public void clearListeners() {
        synchronized (listenersLock) {
            for (int i = 0; i < listenerCount; i++) {
                listeners[i] = null;
            }
            listenerCount = 0;
            System.out.println("[SystemClock] Todos los listeners removidos");
        }
    }
    
    // ==================== GETTERS Y ESTADO ====================
    
    /**
     * Obtiene el ciclo actual del reloj
     * 
     * @return Ciclo actual (thread-safe)
     */
    public int getCurrentCycle() {
        return currentCycle.get();
    }
    
    /**
     * Verifica si el reloj esta corriendo
     * 
     * @return true si está en ejecución
     */
    public boolean isRunning() {
        return running.get() && !paused.get();
    }
    
    /**
     * Verifica si el reloj esta pausado
     * 
     * @return true si está pausado
     */
    public boolean isPaused() {
        return paused.get();
    }
    
    /*
     * Obtiene el total de ticks ejecutados
     * 
     * @return Total de ticks
     */
    public long getTotalTicks() {
        return totalTicksExecuted;
    }
    
    /**
     * Calcula el tiempo real transcurrido desde el inicio
     * 
     * @return Tiempo en milisegundos
     */
    public long getElapsedTimeMs() {
        if (startTimeMs == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTimeMs;
    }
    
    /**
     * Calcula la tasa de ticks por segundo (TPS)
     * 
     * @return Ticks por segundo
     */
    public double getTicksPerSecond() {
        long elapsedMs = getElapsedTimeMs();
        if (elapsedMs == 0) {
            return 0;
        }
        return (totalTicksExecuted * 1000.0) / elapsedMs;
    }
    
    // Info
    
    /**
     * Obtiene informacion completa del estado del reloj
     * 
     * @return String con información detallada
     */
    public String getStatusInfo() {
        return String.format(
            "SystemClock Status:\n" +
            "  Ciclo actual: %d\n" +
            "  Estado: %s\n" +
            "  Duración ciclo: %dms\n" +
            "  Listeners: %d\n" +
            "  Total ticks: %d\n" +
            "  Tiempo transcurrido: %.2fs\n" +
            "  TPS: %.2f",
            currentCycle.get(),
            isRunning() ? "CORRIENDO" : (isPaused() ? "PAUSADO" : "DETENIDO"),
            cycleDurationMs,
            getListenerCount(),
            totalTicksExecuted,
            getElapsedTimeMs() / 1000.0,
            getTicksPerSecond()
        );
    }
    
    public String toString() {
        return String.format("SystemClock[cycle=%d, running=%b, duration=%dms, listeners=%d]",
                           currentCycle.get(), isRunning(), cycleDurationMs, getListenerCount());
    }

    public static SystemClock getInstance() {
        return instance;
    }


    
    
}
