/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

/**
 *
 * @author Abraham Castillo
 */
public enum ProcessType {
    
    PERIODIC("Periódica", "Tarea que se ejecuta a intervalos regulares"),
    APERIODIC("Aperiódica", "Tarea disparada por eventos externos");
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor del enum.
     * 
     * @param displayName Nombre para mostrar en la interfaz
     * @param description Descripción del tipo de proceso
     */
    ProcessType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    
    //Getter
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    
    /**
     * Verifica si el proceso es periódico.
     * 
     * @return true si es PERIODIC
     */
    public boolean isPeriodic() {
        return this == PERIODIC;
    }
    
    /**
     * Verifica si el proceso es aperiódico.
     * 
     * @return true si es APERIODIC
     */
    public boolean isAperiodic() {
        return this == APERIODIC;
    }
    
    
    public String toString() {
        return displayName;
    }
}
