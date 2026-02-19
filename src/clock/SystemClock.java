/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Freya Blanca
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
    private ClockListener[] listeners; //FALTA CREAR LA CLASE CLOCKLISTENER
    private int listenerCount;
    private final Object listenersLock; //Lock para sincronizar acceso a la lista de listeners
    
    // Estadisticas

    private long startTimeMs; //Tiempo real cuando inició el reloj
    private long totalTicksExecuted; //Total de ticks ejecutados desde el inicio
    private static SystemClock instance; //Instancia única del reloj (Singleton)
    
    
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
    
}
