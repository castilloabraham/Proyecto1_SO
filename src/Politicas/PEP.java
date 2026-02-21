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
public class PEP {
    public void ejecutar() {
        Lista<Proceso> lista = new Lista<>();
        lista.agregarAlFinal(new Proceso("P1", 10, 3));
        lista.agregarAlFinal(new Proceso("P2", 2, 1)); // Mayor prioridad

        int tiempoActual = 0;
        int completados = 0;
        int n = lista.size();

        while (completados < n) {
            Proceso masPrioritario = null;
            int mayorPrioridad = Integer.MAX_VALUE;

            Nodo<Proceso> nodoActual = lista.getCabeza();
            while (nodoActual != null) {
                Proceso p = nodoActual.getDato();
                if (p.tiempoRestante > 0 && p.prioridad < mayorPrioridad) {
                    mayorPrioridad = p.prioridad;
                    masPrioritario = p;
                }
                nodoActual = nodoActual.getSiguiente();
            }

            if (masPrioritario != null) {
                System.out.println("[T=" + tiempoActual + "] " + masPrioritario.nombre + " corre.");
                masPrioritario.tiempoRestante--;
                tiempoActual++;
                if (masPrioritario.tiempoRestante == 0) completados++;
            }
        }
    }
}