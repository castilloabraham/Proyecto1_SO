/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Estructuras.Cola;

/**
 * @author Miguel
 */
public class RoundRobin {

    public void ejecutar(int quantum) {
        // Usamos tu clase Cola personalizada
        Cola<Proceso> colaListos = new Cola<>();

        // Agregamos procesos de prueba
        colaListos.encolar(new Proceso("P1", 8, 0));
        colaListos.encolar(new Proceso("P2", 4, 0));
        colaListos.encolar(new Proceso("P3", 10, 0));

        int tiempoActual = 0;

        System.out.println("=== Ejecución Round Robin (Quantum: " + quantum + ") ===");

        while (!colaListos.estaVacia()) {
            // Sacamos el proceso que toca
            Proceso p = colaListos.desencolar();
            
            // El tiempo que ejecutará es el mínimo entre lo que le queda y el quantum
            int tiempoEjecucion = (p.tiempoRestante > quantum) ? quantum : p.tiempoRestante;
            
            System.out.println("[T = " + tiempoActual + "] Ejecutando " + p.nombre + " por " + tiempoEjecucion + " unidades.");
            
            p.tiempoRestante -= tiempoEjecucion;
            tiempoActual += tiempoEjecucion;

            if (p.tiempoRestante > 0) {
                // Si aún tiene ráfaga, vuelve al final de la cola (encolar)
                colaListos.encolar(p);
                System.out.println("   -> " + p.nombre + " vuelve a la cola (Restante: " + p.tiempoRestante + ")");
            } else {
                // El proceso terminó
                p.tiempoFinal = tiempoActual;
                p.tiempoEspera = p.tiempoFinal - p.tiempoRafaga;
                System.out.println("   >> " + p.nombre + " FINALIZADO. (Espera: " + p.tiempoEspera + ")");
            }
        }
        
        System.out.println("\nSimulación completada en T = " + tiempoActual);
    }

    public static void main(String[] args) {
        RoundRobin rr = new RoundRobin();
        rr.ejecutar(3); // Probamos con un quantum de 3
    }
}