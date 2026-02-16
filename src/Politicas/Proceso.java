/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

/**
 * Representa un Proceso para políticas de Tiempo Real (como EDF)
 * y políticas de Prioridad.
 * * @author Miguel
 */
public class Proceso {
    public String nombre;
    public int tiempoRafaga;    // Duración original de la tarea
    public int tiempoRestante;  // Lo que falta por ejecutar
    public int prioridad;       // En EDF representa el Plazo Límite (Deadline)
    public int tiempoFinal;     // Momento en que termina la ejecución
    public int tiempoEspera;    // Tiempo total que pasó en espera

    /**
     * Constructor para procesos con ráfaga y prioridad/deadline.
     * * @param nombre Nombre del proceso (ej. "P1")
     * @param tiempoRafaga Tiempo total de CPU requerido
     * @param prioridad Valor de importancia o Deadline (menor valor = más urgente)
     */
    public Proceso(String nombre, int tiempoRafaga, int prioridad) {
        this.nombre = nombre;
        this.tiempoRafaga = tiempoRafaga;
        this.tiempoRestante = tiempoRafaga;
        this.prioridad = prioridad;
        this.tiempoFinal = 0;
        this.tiempoEspera = 0;
    }

    // Opcional: Sobrescribir toString para facilitar la impresión en consola
    @Override
    public String toString() {
        return "Proceso{" + "nombre=" + nombre + ", restante=" + tiempoRestante + ", prioridad=" + prioridad + '}';
    }
}