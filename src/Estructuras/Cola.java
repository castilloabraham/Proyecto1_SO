/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Estructuras;

/**
 *
 * @author Abraham Castillo
 */
public class Cola<T> {
    
    //Atributos
    private Nodo<T> frente;
    private Nodo<T> fondo;
    private int size;
    
    /**
     * Constructor: Inicializa una cola vacía.
     */
    public Cola() {
        this.frente = null;
        this.fondo = null;
        this.size = 0;
    }
    
    /**
     * Inserta un elemento al final de la cola.
     * @param dato Valor a insertar.
     */
    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (estaVacia()) {
            frente = nuevo;
            fondo = nuevo;
        } else {
            fondo.setSiguiente(nuevo);
            fondo = nuevo;
        }
        size++;
    }
    
    /**
     * Elimina el primer elemento de la cola.
     * Retorna null si está vacía.
     */
    public T desencolar() {
        if (estaVacia()) return null;
        
        T dato = frente.getDato();
        frente = frente.getSiguiente();
        
        if (frente == null) {
            fondo = null;
        }
        size--;
        return dato;
    }
    
    /**
     * Retorna el elemento que está en el frente de la cola sin eliminarlo.
     * @return Valor del primer elemento.
     */
    public T verFrente() {
        if (estaVacia()) return null;
        return frente.getDato();
    }
    
    /**
     * Verifica si la cola está vacía.
     * @return true si está vacía, false en caso contrario.
     */
    public boolean estaVacia() {
        return frente == null;
    }
    /**
     * Retorna el número de elementos en la cola.
     * @return Tamaño de la cola.
     */
    public int size() {
        return size;
    }
    
    /**
     * Elimina todos los elementos de la cola.
     */
    public void vaciar() {
        frente = null;
        fondo = null;
        size = 0;
    }

    public Nodo<T> getFrente() {
        return frente;
    }
    
    
}
