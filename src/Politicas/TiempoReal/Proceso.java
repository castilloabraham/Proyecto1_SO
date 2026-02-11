/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas.TiempoReal;

public class Proceso {
    public String nombre;
    public int tiempoRafaga;
    public int tiempoRestante;
    public int prioridad; // Atributo específico para esta política

    public Proceso(String nombre, int tiempoRafaga, int prioridad) {
        this.nombre = nombre;
        this.tiempoRafaga = tiempoRafaga;
        this.tiempoRestante = tiempoRafaga;
        this.prioridad = prioridad;
    }
}