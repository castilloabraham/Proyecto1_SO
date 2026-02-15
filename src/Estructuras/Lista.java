/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Estructuras;

/**
 *
 * @author adcd_
 */
public class Lista <T> {
    private Nodo<T> cabeza;
    private int size;
    
    /**
     * Constructor por defecto.
     * Inicializa una lista vacía.
     */
    public Lista() {
        this.cabeza = null;
        this.size = 0;
    }
    
    /**
     * Agrega un nuevo nodo con el valor especificado al inicio de la lista.
     * @param dato el valor a almacenar en el nuevo nodo
     */
    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cabeza);
        cabeza = nuevo;
        size++;
    }
    
    /**
     * Agrega un nuevo nodo con el valor especificado al final de la lista.
     * @param dato el valor a almacenar en el nuevo nodo
     */
    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            Nodo<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevo);
        }
        size++;
    }
    
    /**
    * Obtiene el elemento en la posición especificada de la lista.
    * 
    * @param indice la posición del elemento a obtener (basada en 0)
    * @return el elemento en la posición indicada, o null si el índice está fuera de rango
    */
    public T obtener(int indice) {
        if (indice < 0 || indice >= size) return null;
        
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }
    
    /**
    * Elimina y retorna el elemento en la posición especificada.
    * 
    * @param indice la posición del elemento a eliminar (basada en 0)
    * @return el elemento eliminado, o null si el índice está fuera de rango
    */
    public T eliminar(int indice) {
        if (indice < 0 || indice >= size) return null;
        
        Nodo<T> actual = cabeza;
        Nodo<T> anterior = null;
        
        if (indice == 0) {
            cabeza = cabeza.getSiguiente();
        } else {
            for (int i = 0; i < indice; i++) {
                anterior = actual;
                actual = actual.getSiguiente();
            }
            anterior.setSiguiente(actual.getSiguiente());
        }
        size--;
        return actual.getDato();
    }
    
    /**
    * Elimina la primera ocurrencia del elemento especificado.
    * 
    * @param dato el elemento a eliminar
    * @return true si el elemento fue encontrado y eliminado, false en caso contrario
    */
    public boolean eliminar(T dato) {
        Nodo<T> actual = cabeza;
        Nodo<T> anterior = null;
        
        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                if (anterior == null) {
                    cabeza = actual.getSiguiente();
                } else {
                    anterior.setSiguiente(actual.getSiguiente());
                }
                size--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }
    
    /**
    * Verifica si la lista contiene el elemento especificado.
    * 
    * @param dato el elemento a buscar
    * @return true si el elemento existe en la lista, false en caso contrario
    */
    public boolean contiene(T dato) {
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }
    
    /**
     * Retorna el número de elementos en la lista.
     * 
     * @return cantidad actual de elementos
     */
    public int size() {
        return size;
    }
    
    /**
     * Verifica si la lista está vacía.
     * @return true si la lista está vacía, false en caso contrario
     */
    public boolean estaVacia() {
        return cabeza == null;
    }
    
    
    // Método para iterar
    public Nodo<T> getCabeza() {
        return cabeza;
    }

}
