/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

/**
 *
 * @author Abraham Castillo
 */
public enum InstructionType {
    CPU("CPU", "Instrucción que requiere procesador"),
    IO("E/S", "Instrucción de Entrada/Salida");
    
    
    private final String displayName;
    private final String description;

    
    /**
     * Constructor del enum.
     * 
     * @param displayName Nombre para la interfaz gráfica
     * @param description Descripción del tipo de instrucción
     */
    InstructionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    //getter
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Verifica si es una instrucción de CPU.
     * 
     * @return true si es CPU
     */
    public boolean isCPU() {
        return this == CPU;
    }
    
    /**
     * Verifica si es una instrucción de E/S.
     * 
     * @return true si es IO
     */
    public boolean isIO() {
        return this == IO;
    }
    
    /**
     * Determina si esta instrucción causa bloqueo del proceso.
     * 
     * @return true si es IO (causa transición a BLOCKED)
     */
    public boolean causesBlocking() {
        return this == IO;
    }
    

    public String toString() {
        return displayName;
    }
    
    
}
