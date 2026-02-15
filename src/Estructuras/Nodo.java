/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Estructuras;

/**
 *
 * @author Abraham Castillo
 */
public class Nodo<T> {
    //Atributos 
    private T dato;
    private Nodo<T> siguiente;
    
    /**
     * Constructor que crea un nodo con el valor dado.
     * Inicializa el nodo con el dato y establece la referencia al siguiente nodo como null.
     *
     * @param dato El valor que se almacena en el nodo
     */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }
    
    
    //Getter and setter

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
    
}
