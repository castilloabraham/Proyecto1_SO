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
public class EDF {
    public void ejecutar() {
        Lista<Proceso> lista = new Lista<>();
        lista.agregarAlFinal(new Proceso("P1", 3, 7)); // Debe terminar en T=7
        lista.agregarAlFinal(new Proceso("P2", 2, 4)); // Debe terminar en T=4

        int tiempoActual = 0;
        int completados = 0;

        while (completados < lista.size()) {
            Proceso urgente = null;
            int deadlineCercano = Integer.MAX_VALUE;

            Nodo<Proceso> aux = lista.getCabeza();
            while (aux != null) {
                Proceso p = aux.getDato();
                if (p.tiempoRestante > 0 && p.prioridad < deadlineCercano) {
                    deadlineCercano = p.prioridad;
                    urgente = p;
                }
                aux = aux.getSiguiente();
            }

            if (urgente != null) {
                System.out.println("[T=" + tiempoActual + "] EDF: " + urgente.nombre);
                urgente.tiempoRestante--;
                tiempoActual++;
                if (urgente.tiempoRestante == 0) {
                    completados++;
                    String status = (tiempoActual <= urgente.prioridad) ? "A TIEMPO" : "TARDE";
                    System.out.println("   >> Finalizado " + status);
                }
            }
        }
    }
}
