/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

/**
 *
 * @author Abraham Castillo
 */
public class ProcessIDGenerator {
    
    private static ProcessIDGenerator instance;
    private int nextID;
    private static final String PREFIX = "P_";
    private static final int ID_DIGITS = 4;
    
    /**
     * Constructor para implementar Singleton.
     * El contador inicia en 1.
     */
    private ProcessIDGenerator() {
        this.nextID = 1;
    }
    
    /**
     * Obtiene la instancia única del generador.
     * Si no existe, la crea.
     * 
     * @return La única instancia de ProcessIDGenerator
     */
    public static ProcessIDGenerator getInstance() {
        if (instance == null) {
            instance = new ProcessIDGenerator();
        }
        return instance;
    }
    
    /**
     * Genera el siguiente ID único para un proceso.
     * 
     * El método es sincronizado para garantizar thread-safety,
     * ya que múltiples hilos podrían crear procesos simultáneamente.
     * 
     * @return ID único en formato "P_XXXX"
     */
    public synchronized String generateID() {
        String id = PREFIX + String.format("%0" + ID_DIGITS + "d", nextID);
        nextID++;
        return id;
    }
    
    /**
     * Obtiene el próximo número de ID sin incrementar el contador.
     * Útil para preview o debugging.
     * 
     * @return El próximo número que será asignado
     */
    public synchronized int peekNextID() {
        return nextID;
    }
    
    /**
     * Resetea el generador al estado inicial.
     * 
     * Solo para reiniciar la simulación completa!!!!!!!!!!!!!!!!!1
     */
    public synchronized void reset() {
        nextID = 1;
    }
    
    /**
     * Obtiene el total de IDs generados hasta el momento.
     * 
     * @return Cantidad de procesos que han recibido ID
     */
    public synchronized int getTotalGenerated() {
        return nextID - 1;
    }
    
    /**
     * Genera múltiples IDs de una sola vez.
     * 
     * Para la función Generar 20 Procesos Aleatorios.
     * 
     * @param count Cantidad de IDs a generar
     * @return Array con los IDs generados
     */
    public synchronized String[] generateBatch(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = generateID();
        }
        return ids;
    }
    
    /**
     * Verifica si un ID tiene el formato correcto.
     * 
     * @param id El ID a validar
     * @return true si el formato es válido (P_XXXX)
     */
    public static boolean isValidFormat(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        // Verificar patrón: P_ seguido de exactamente ID_DIGITS dígitos
        String pattern = "^" + PREFIX + "\\d{" + ID_DIGITS + "}$";
        return id.matches(pattern);
    }
    
    /**
     * Extrae solo el numero del ID.
     * 
     * @param id ID completo (ej: "P_0042")
     * @return Número extraído (ej: 42), o -1 si el formato es inválido
     */
    public static int extractNumber(String id) {
        if (!isValidFormat(id)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(id.substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
        
    public String toString() {
        return "ProcessIDGenerator{nextID=" + nextID + 
               ", totalGenerated=" + getTotalGenerated() + "}";
    }
}
