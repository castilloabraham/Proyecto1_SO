/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Process;

/**
 *
 * @author Abraham Castillo y Freya Blanca
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
    
    private int priority; //Prioridad del proceso 1 es la maxima
    private int deadline; //Tiempo límite absoluto para completar el proceso
    private int remainingDeadline; //Tiempo restante hasta el deadline
    private int period; //Periodo del proceso
    private int arrivalTime; //Ciclo en que el proceso entró al sistema
    private int cpuTimeUsed; //Tiempo total de CPU usado por el proceso
    private int remainingQuantum; //Quantum restante para algoritmos como Round Robin
    private int waitingTime; //Tiempo de espera acumulado en colas
    private int turnaroundTime; //Tiempo total desde llegada hasta terminación
    
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
    
    /**
     * Constructor principal del PCB.
     * 
     * @param processName Nombre descriptivo del proceso
     * @param totalInstructions Número total de instrucciones
     * @param instructionType Tipo de instrucciones (CPU o IO)
     * @param priority Prioridad (1 = alta)
     * @param deadline Deadline en ciclos de reloj
     * @param processType Tipo de proceso (PERIODIC o APERIODIC)
     * @param arrivalTime Ciclo de llegada al sistema
     */
    public PCB(String processName, int totalInstructions, InstructionType instructionType, int priority, int deadline, ProcessType processType, int arrivalTime) {
        
        // Generar ID único
        this.processID = ProcessIDGenerator.getInstance().generateID();
        
        // Información básica
        this.processName = processName;
        this.totalInstructions = totalInstructions;
        this.instructionType = instructionType;
        this.priority = priority;
        this.deadline = deadline;
        this.remainingDeadline = deadline;
        this.processType = processType;
        this.arrivalTime = arrivalTime;
        
        // Estado inicial
        this.currentState = ProcessState.NEW;
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        
        // Inicialización de contadores
        this.cpuTimeUsed = 0;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.remainingQuantum = 0;
        this.currentIOCycles = 0;
        
        // Flags iniciales
        this.isBlockedForIO = false;
        this.missedDeadline = false;
        this.isSuspended = false;
        this.completionTime = -1;
        
        // Valores por defecto para E/S
        this.cyclesUntilIOException = 0;
        this.cyclesForIOCompletion = 0;
        this.period = 0; // 0 indica no periódico
    }
    
    /**
     * Constructor simplificado para procesos de CPU puro.
     */
    public PCB(String processName, int totalInstructions, int priority, int deadline, int arrivalTime) {
        this(processName, totalInstructions, InstructionType.CPU, priority, deadline, ProcessType.APERIODIC, arrivalTime);
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getRemainingDeadline() {
        return remainingDeadline;
    }

    public void setRemainingDeadline(int remainingDeadline) {
        this.remainingDeadline = remainingDeadline;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getCpuTimeUsed() {
        return cpuTimeUsed;
    }

    public void setCpuTimeUsed(int cpuTimeUsed) {
        this.cpuTimeUsed = cpuTimeUsed;
    }

    public int getRemainingQuantum() {
        return remainingQuantum;
    }

    public void setRemainingQuantum(int remainingQuantum) {
        this.remainingQuantum = remainingQuantum;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }
    
    
    
    
}
