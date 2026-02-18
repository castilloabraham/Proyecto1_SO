/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

/**
 *
 * @author Abraham Castillo
 */
public class PCB {
    // ==================== IDENTIFICACIÓN ====================
    private final String processID; // Id del proceso
    private String processName; //Nombre
    private ProcessType processType; //tipo de proceso, periodico o no
    
    // ==================== ESTADO Y CONTROL ====================
    private ProcessState currentState; //Estado del proceso
    private int programCounter; //PC - Program Counter: Indica la próxima instrucción a ejecutar
    private int memoryAddressRegister;  //MAR - Memory Address Register: Dirección de memoria actual
    
    // ==================== INFORMACIÓN DE INSTRUCCIONES ====================
    private final int totalInstructions; //Numero total de instrucciones del proceso
    private InstructionType instructionType; //Tipo de instruccion: CPU o IO
    private int cyclesUntilIOException; //Ciclos necesarios para generar una excepción de E/S
    private int cyclesForIOCompletion; //Ciclos necesarios para completar la operación de E/S
    private int currentIOCycles; //Contador de ciclos de E/S
    
    // ==================== PLANIFICACIÓN Y TIEMPOS ====================
    
    //Agregar freya
    
    // ==================== FLAGS Y ESTADO ADICIONAL ====================
    
    private boolean isBlockedForIO; //Indica si el proceso está actualmente bloqueado por E/S
    private boolean missedDeadline; //Indica si el proceso perdió su deadline
    private int completionTime; //Ciclo de reloj en que el proceso terminó
    private boolean isSuspended; //Indica si el proceso ha sido suspendido a disco
    
    
    /**
     * Constructor principal del PCB.
     * 
     * @param processName Nombre descriptivo del proceso
     * @param totalInstructions Número total de instrucciones
     * @param instructionType Tipo de instrucciones (CPU o IO)
     * @param processType Tipo de proceso (PERIODIC o APERIODIC)
     */
    public PCB(String processName, int totalInstructions, InstructionType instructionType, ProcessType processType) {
        
        // Generar ID único
        this.processID = ProcessIDGenerator.getInstance().generateID();
        
        // Información básica
        this.processName = processName;
        this.totalInstructions = totalInstructions;
        this.instructionType = instructionType;
        this.processType = processType;
        
        // Estado inicial
        this.currentState = ProcessState.NEW;
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        
        // Inicialización de contadores
        this.currentIOCycles = 0;
        
        // Flags iniciales
        this.isBlockedForIO = false;
        this.missedDeadline = false;
        this.isSuspended = false;
        this.completionTime = -1;
        
        // Valores por defecto para E/S
        this.cyclesUntilIOException = 0;
        this.cyclesForIOCompletion = 0;
    }
    
    
    
    //getter y setter

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public ProcessState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ProcessState currentState) {
        this.currentState = currentState;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public int getMemoryAddressRegister() {
        return memoryAddressRegister;
    }

    public void setMemoryAddressRegister(int memoryAddressRegister) {
        this.memoryAddressRegister = memoryAddressRegister;
    }

    public InstructionType getInstructionType() {
        return instructionType;
    }

    public void setInstructionType(InstructionType instructionType) {
        this.instructionType = instructionType;
    }

    public int getCyclesUntilIOException() {
        return cyclesUntilIOException;
    }

    public void setCyclesUntilIOException(int cyclesUntilIOException) {
        this.cyclesUntilIOException = cyclesUntilIOException;
    }

    public int getCyclesForIOCompletion() {
        return cyclesForIOCompletion;
    }

    public void setCyclesForIOCompletion(int cyclesForIOCompletion) {
        this.cyclesForIOCompletion = cyclesForIOCompletion;
    }

    public int getCurrentIOCycles() {
        return currentIOCycles;
    }

    public void setCurrentIOCycles(int currentIOCycles) {
        this.currentIOCycles = currentIOCycles;
    }

    public boolean isIsBlockedForIO() {
        return isBlockedForIO;
    }

    public void setIsBlockedForIO(boolean isBlockedForIO) {
        this.isBlockedForIO = isBlockedForIO;
    }

    public boolean isMissedDeadline() {
        return missedDeadline;
    }

    public void setMissedDeadline(boolean missedDeadline) {
        this.missedDeadline = missedDeadline;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }

    public boolean isIsSuspended() {
        return isSuspended;
    }

    public void setIsSuspended(boolean isSuspended) {
        this.isSuspended = isSuspended;
    }
    
    
    
    
}
