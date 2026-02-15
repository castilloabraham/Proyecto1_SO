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
public class FCFS {

    public void ejecutar() {
        // Usamos una cola para representar el orden de llegada
        Queue<Proceso> colaListos = new LinkedList<>();

        // Agregamos procesos de ejemplo (puedes cambiarlos o pedirlos por consola)
        colaListos.add(new Proceso("P1", 12));
        colaListos.add(new Proceso("P2", 3));
        colaListos.add(new Proceso("P3", 6));

        int tiempoActual = 0;
        double sumaEspera = 0;
        int cantidadProcesos = colaListos.size();

        System.out.println("=== Ejecución FCFS (No Preentivo) ===");

        while (!colaListos.isEmpty()) {
            Proceso p = colaListos.poll();

            // En FCFS, el tiempo de espera es el tiempo que ha transcurrido hasta ahora
            p.tiempoEspera = tiempoActual;
            
            System.out.println("[T = " + tiempoActual + "] Iniciando " + p.nombre + "...");
            
            // El proceso se ejecuta hasta que termina (Rafaga completa)
            tiempoActual += p.tiempoRafaga;
            p.tiempoFinal = tiempoActual;
            
            sumaEspera += p.tiempoEspera;

            System.out.println("   >> " + p.nombre + " FINALIZADO. (Tiempo de Espera: " + p.tiempoEspera + ")");
        }

        System.out.println("\n--- Estadísticas Finales ---");
        System.out.println("Tiempo total de ejecución: " + tiempoActual);
        System.out.println("Tiempo medio de espera: " + (sumaEspera / cantidadProcesos));
    }

    public static void main(String[] args) {
        FCFS planificador = new FCFS();
        planificador.ejecutar();
    }
}