/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Estructuras.Cola;

/**
 * @author Miguel
 */
public class FCFS {

    public void ejecutar() {
        // Usamos tu clase Cola personalizada
        Cola<Proceso> colaListos = new Cola<>();

        // Agregamos procesos de ejemplo usando tu método encolar
        colaListos.encolar(new Proceso("P1", 10, 0));
        colaListos.encolar(new Proceso("P2", 3, 0));
        colaListos.encolar(new Proceso("P3", 5, 0));

        int tiempoActual = 0;
        float sumaEspera = 0;
        int totalProcesos = colaListos.size();

        System.out.println("=== Ejecución FCFS (Estructuras de Abraham Castillo) ===");

        while (!colaListos.estaVacia()) {
            // Extraemos el proceso del frente de la cola
            Proceso p = colaListos.desencolar();
            
            p.tiempoEspera = tiempoActual;
            System.out.println("[T = " + tiempoActual + "] Iniciando " + p.nombre + " (Ráfaga: " + p.tiempoRafaga + ")");
            
            // En FCFS el proceso corre hasta terminar
            tiempoActual += p.tiempoRafaga;
            p.tiempoFinal = tiempoActual;
            sumaEspera += p.tiempoEspera;

            System.out.println("   >> " + p.nombre + " FINALIZADO en T = " + tiempoActual);
        }

        System.out.println("\nPromedio de tiempo de espera: " + (sumaEspera / totalProcesos));
    }

    public static void main(String[] args) {
        new FCFS().ejecutar();
    }
}