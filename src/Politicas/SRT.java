/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Politicas;

import Estructuras.Lista;
import Estructuras.Nodo;

/**
 * @author Miguel
 */
public class SRT {
    public void ejecutar() {
        Lista<Proceso> procesos = new Lista<>();
        procesos.agregarAlFinal(new Proceso("P1", 8, 0));
        procesos.agregarAlFinal(new Proceso("P2", 4, 0));
        procesos.agregarAlFinal(new Proceso("P3", 2, 0));

        int tiempoActual = 0;
        int completados = 0;
        int n = procesos.size();

        System.out.println("=== SRT (Estructuras Propias) ===");

        while (completados < n) {
            Proceso masCorto = null;
            int minRestante = Integer.MAX_VALUE;

            // Recorrido manual usando tu método getCabeza()
            Nodo<Proceso> actual = procesos.getCabeza();
            while (actual != null) {
                Proceso p = actual.getDato();
                if (p.tiempoRestante > 0 && p.tiempoRestante < minRestante) {
                    minRestante = p.tiempoRestante;
                    masCorto = p;
                }
                actual = actual.getSiguiente();
            }

            if (masCorto != null) {
                System.out.println("[T=" + tiempoActual + "] Ejecutando " + masCorto.nombre);
                masCorto.tiempoRestante--;
                tiempoActual++;

                if (masCorto.tiempoRestante == 0) {
                    completados++;
                    System.out.println("   >> " + masCorto.nombre + " terminado.");
                }
            } else {
                tiempoActual++;
            }
        }
    }
}