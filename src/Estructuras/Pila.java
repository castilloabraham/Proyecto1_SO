/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Estructuras;

/**
 *
 * @author Abraham Castillo
 */
public class Pila<T> {
    private Nodo<T> tope;
    private int size;
    
     /**
     * Constructor que inicializa una pila vacía.
     * El tope se establece como null y el tamaño en 0.
     */
    public Pila(){
        this.tope = null;
        this.size = 0;
    }
    
    /**
     * Inserta un elemento en el tope de la pila.
     * 
     * @param dato el elemento a insertar
     */
    public void push(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(tope);
        tope = nuevo;
        size++;
    }
    
    /**
     * Elimina y retorna el elemento en el tope de la pila.
     * 
     * @return el elemento eliminado del tope, o null si la pila está vacía
     */
    public T pop() {
        if (estaVacia()) return null;
        
        T dato = tope.getDato();
        tope = tope.getSiguiente();
        size--;
        return dato;
    }
    
    /**
     * Retorna el elemento en el tope de la pila sin eliminarlo.
     * 
     * @return el elemento en el tope, o null si la pila está vacía
     */
    public T verTope() {
        if (estaVacia()) return null;
        return tope.getDato();
    }
    
    /**
     * Verifica si la pila está vacía.
     * 
     * @return true si la pila no contiene elementos, false en caso contrario
     */
    public boolean estaVacia() {
        return tope == null;
    }
    
    /**
     * Retorna el número de elementos en la pila.
     * 
     * @return cantidad actual de elementos
     */
    public int size() {
        return size;
    }
    
    
    /**
     * Elimina todos los elementos de la pila.
     * El tope se establece como null y el tamaño en 0.
     */
    public void vaciar() {
        tope = null;
        size = 0;
    }
}
