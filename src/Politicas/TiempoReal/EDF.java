/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas.TiempoReal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Miguel Montilla
 */
public class EDF {

    public void ejecutar() {
        List<Proceso> lista = new ArrayList<>();
        
        // Agregamos procesos: (Nombre, Ráfaga, Prioridad/Deadline)
        // P2 tiene prioridad 5, es decir, debe terminar en T=5.
        lista.add(new Proceso("P1", 4, 10));
        lista.add(new Proceso("P2", 2, 5));
        lista.add(new Proceso("P3", 5, 15));

        int tiempoActual = 0;
        int completados = 0;
        int n = lista.size();

        System.out.println("=== Ejecución EDF (Usando Atributo Prioridad como Deadline) ===");

        while (completados < n) {
            Proceso masUrgente = null;
            int menorValorPrioridad = Integer.MAX_VALUE;

            // Buscamos el proceso con la prioridad (deadline) más baja
            for (Proceso p : lista) {
                if (p.tiempoRestante > 0 && p.prioridad < menorValorPrioridad) {
                    menorValorPrioridad = p.prioridad;
                    masUrgente = p;
                }
            }

            if (masUrgente == null) {
                tiempoActual++;
                continue;
            }

            System.out.println("[T = " + tiempoActual + "] " + masUrgente.nombre + 
                               " ejecutando (Plazo: " + masUrgente.prioridad + ")");
            
            masUrgente.tiempoRestante--;
            tiempoActual++;

            if (masUrgente.tiempoRestante == 0) {
                completados++;
                // Verificamos si cumplió con su "prioridad" (plazo)
                if (tiempoActual <= masUrgente.prioridad) {
                    System.out.println("   >> " + masUrgente.nombre + " TERMINADO A TIEMPO.");
                } else {
                    System.out.println("   >> " + masUrgente.nombre + " TERMINADO CON RETRASO.");
                }
            }
        }
        System.out.println("\nSimulación finalizada.");
    }

    public static void main(String[] args) {
        new EDF().ejecutar();
    }
}