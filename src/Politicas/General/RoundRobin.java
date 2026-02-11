/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas.General;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Miguel Montilla
 */
public class RoundRobin {

    public void simular(int quantum) {
        // Creamos la cola de listos (Ready Queue)
        Queue<Proceso> colaListos = new LinkedList<>();

        // Agregamos algunos procesos de prueba
        colaListos.add(new Proceso("P1", 8));
        colaListos.add(new Proceso("P2", 4));
        colaListos.add(new Proceso("P3", 9));

        int tiempoGlobal = 0;

        System.out.println("=== Ejecución Round Robin (Quantum: " + quantum + ") ===");
        
        

        while (!colaListos.isEmpty()) {
            Proceso p = colaListos.poll(); // Sacamos el primero de la cola

            // El proceso corre por el quantum o por lo que le quede (lo que sea menor)
            int tiempoAEjecutar = Math.min(p.tiempoRestante, quantum);
            
            System.out.println("[T = " + tiempoGlobal + "] Ejecutando " + p.nombre + " por " + tiempoAEjecutar + " unidades.");
            
            p.tiempoRestante -= tiempoAEjecutar;
            tiempoGlobal += tiempoAEjecutar;

            if (p.tiempoRestante > 0) {
                // Si aún tiene ráfaga, vuelve al final de la cola
                colaListos.add(p);
            } else {
                // El proceso ha terminado
                p.tiempoFinal = tiempoGlobal;
                int turnaround = p.tiempoFinal; // Asumiendo llegada en 0
                p.tiempoEspera = turnaround - p.tiempoRafaga;
                
                System.out.println("   >> " + p.nombre + " FINALIZADO. (Espera: " + p.tiempoEspera + ")");
            }
        }
        
        System.out.println("\nSimulación completada en T = " + tiempoGlobal);
    }

    public static void main(String[] args) {
        RoundRobin rr = new RoundRobin();
        // Probamos con un quantum de 3
        rr.simular(3);
    }
}