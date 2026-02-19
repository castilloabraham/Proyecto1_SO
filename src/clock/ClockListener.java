/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clock;

/**
 *
 * @author Freya Blanca
 */
public interface ClockListener {
    
    /**
     * Método invocado por el reloj en cada tick/ciclo.
     * 
     * @param currentCycle Ciclo actual del reloj global
     */
    void onClockTick(int currentCycle);
    
    
    
    /**
     * Metodo invocado cuando el reloj se pausa. Permite a los componentes realizar acciones necesarias
     * cuando la simulación se detiene temporalmente.
     * 
     * Implementación por defecto vacía
     * 
     * @param cycleWhenPaused Ciclo en el que se pausó
     */
    default void onClockPaused(int cycleWhenPaused) {
        // Los componentes pueden sobrescribir si necesitan manejar la pausa
    }
    
    /**
     * Método invocado cuando el reloj se reanuda
     * 
     * @param cycleWhenResumed Ciclo en el que se reanudó
     */
    default void onClockResumed(int cycleWhenResumed) {
        // Implementación por defecto vacía
    }
    
    /**
     * Método invocado cuando el reloj se reinicia completamente
     */
    default void onClockReset() {
        // Implementación por defecto vacía
    }
    
    /**
     * Obtiene el nombre del listener
     * 
     * @return Nombre identificador del componente
     */
    default String getListenerName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Indica la prioridad de ejecución del listener
     * 
     * @return Prioridad (1 = más alta, números mayores = menor prioridad)
     */
    default int getPriority() {
        return 100; // Prioridad por defecto (baja)
    }
}
