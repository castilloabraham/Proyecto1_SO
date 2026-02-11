/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

public class Proceso {
    String nombre;
    int tiempoRafaga;    // Duración total requerida
    int tiempoRestante;  // Lo que falta por ejecutar
    int tiempoEspera;    // Tiempo que ha pasado en la cola
    int tiempoFinal;     // Momento exacto en que termina

    public Proceso(String nombre, int tiempoRafaga) {
        this.nombre = nombre;
        this.tiempoRafaga = tiempoRafaga;
        this.tiempoRestante = tiempoRafaga;
        this.tiempoEspera = 0;
        this.tiempoFinal = 0;
    }
}
