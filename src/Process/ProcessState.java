/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

/**
 *
 * @author adcd_
 */
public enum ProcessState {
    
    NEW("Nuevo", "El proceso ha sido creado pero aún no está en memoria"),
    READY("Listo", "Proceso en memoria, esperando asignación de CPU"),
    RUNNING("Ejecución", "Proceso ejecutándose actualmente en el CPU"),
    BLOCKED("Bloqueado", "Proceso esperando operación de E/S"),
    TERMINATED("Terminado", "Proceso finalizado"),
    READY_SUSPENDED("Listo-Suspendido", "Proceso listo pero en disco (swap)"),
    BLOCKED_SUSPENDED("Bloqueado-Suspendido", "Proceso bloqueado y en disco (swap)");
    
    // Atributos del enum
    private final String displayName;
    private final String description;
    
    
    /**
     * Constructor del enum.
     * 
     * @param displayName Nombre para mostrar en la interfaz
     * @param description Descripción detallada del estado
     */
    ProcessState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    
    //Getters
    
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
    
    
    
    //Metodos
    
    
    /**
     * Verifica si el proceso está en un estado suspendido (en disco).
     * 
     * @return true si está en READY_SUSPENDED o BLOCKED_SUSPENDED
     */
    public boolean isSuspended() {
        return this == READY_SUSPENDED || this == BLOCKED_SUSPENDED;
    }
    
    /**
     * Verifica si el proceso está en memoria principal.
     * 
     * @return true si está en READY, RUNNING o BLOCKED
     */
    public boolean isInMainMemory() {
        return this == READY || this == RUNNING || this == BLOCKED;
    }
    
    /**
     * Verifica si el proceso está activo (no terminado ni nuevo).
     * 
     * @return true si el proceso está en cualquier estado excepto NEW y TERMINATED
     */
    public boolean isActive() {
        return this != NEW && this != TERMINATED;
    }
    
   
    public String toString() {
        return displayName;
    }
    
}
