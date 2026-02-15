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
public class PEP {

    public void ejecutar() {
        List<Proceso> lista = new ArrayList<>();
        
        // Agregamos procesos: (Nombre, Ráfaga, Prioridad)
        // Menor número = Mayor prioridad
        lista.add(new Proceso("P1", 10, 3));
        lista.add(new Proceso("P2", 1, 1));
        lista.add(new Proceso("P3", 2, 4));
        lista.add(new Proceso("P4", 5, 2));

        int tiempoActual = 0;
        int completados = 0;
        int n = lista.size();
        
        System.out.println("=== Ejecución Prioridad Estática Preemptiva ===");
        

        while (completados < n) {
            Proceso masPrioritario = null;
            int mayorPrioridad = Integer.MAX_VALUE;

            // Buscamos el proceso listo con la prioridad más alta (número más bajo)
            for (Proceso p : lista) {
                if (p.tiempoRestante > 0 && p.prioridad < mayorPrioridad) {
                    mayorPrioridad = p.prioridad;
                    masPrioritario = p;
                }
            }

            if (masPrioritario == null) {
                tiempoActual++;
                continue;
            }

            // Ejecución por 1 unidad de tiempo (Preemptivo)
            System.out.println("[T = " + tiempoActual + "] " + masPrioritario.nombre + 
                               " ejecutando (Prioridad: " + masPrioritario.prioridad + ")");
            
            masPrioritario.tiempoRestante--;
            tiempoActual++;

            if (masPrioritario.tiempoRestante == 0) {
                completados++;
                System.out.println("   >> " + masPrioritario.nombre + " FINALIZADO en T = " + tiempoActual);
            }
        }
        
        System.out.println("\nSimulación finalizada.");
    }

    public static void main(String[] args) {
        new PEP().ejecutar();
    }
}