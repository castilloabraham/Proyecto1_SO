/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas.General;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Miguel Montilla
 */
public class SRT {

    public void ejecutar() {
        List<Proceso> listaProcesos = new ArrayList<>();
        
        // Agregamos procesos (Nombre, Tiempo Ráfaga)
        listaProcesos.add(new Proceso("P1", 8));
        listaProcesos.add(new Proceso("P2", 4));
        listaProcesos.add(new Proceso("P3", 2));
        listaProcesos.add(new Proceso("P4", 5));

        int tiempoActual = 0;
        int completados = 0;
        int n = listaProcesos.size();
        
        System.out.println("=== Ejecución SRT (Shortest Remaining Time) ===");
        

        while (completados < n) {
            Proceso masCorto = null;
            int minRestante = Integer.MAX_VALUE;

            // Buscamos el proceso con el menor tiempo restante que aún no haya terminado
            for (Proceso p : listaProcesos) {
                if (p.tiempoRestante > 0 && p.tiempoRestante < minRestante) {
                    minRestante = p.tiempoRestante;
                    masCorto = p;
                }
            }

            if (masCorto == null) {
                tiempoActual++;
                continue;
            }

            // Ejecutamos por una unidad de tiempo
            System.out.println("[T = " + tiempoActual + "] Ejecutando " + masCorto.nombre + " (Restante: " + masCorto.tiempoRestante + ")");
            masCorto.tiempoRestante--;
            tiempoActual++;

            // Si el proceso termina
            if (masCorto.tiempoRestante == 0) {
                completados++;
                masCorto.tiempoFinal = tiempoActual;
                masCorto.tiempoEspera = masCorto.tiempoFinal - masCorto.tiempoRafaga;
                System.out.println("   >> " + masCorto.nombre + " FINALIZADO. (Espera: " + masCorto.tiempoEspera + ")");
            }
        }

        System.out.println("\nSimulación SRT finalizada en T = " + tiempoActual);
    }

    public static void main(String[] args) {
        SRT planificador = new SRT();
        planificador.ejecutar();
    }
}