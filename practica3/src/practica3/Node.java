package practica3;

import java.util.LinkedList;
import static practica3.Types.*;
import java.util.Objects;


/**
 * Clase nodo necesaria para crear el arbol en el A*
 * Implementa la interfaz Comparable para poder comparar nodos.
 * 
 * @author Javier, Jose Miguel, Alvaro y Bryan.
 * @version Práctica 2 (1.0)
 */

public class Node implements Comparable<Node>{
    private State st;
    private int g;
    private int f;

    private Node parent;
    private ACTIONS lastAction;

    
    /**
    * Constructor de la clase Node teniendo en cuenta un padre y una acción realizada.
    *
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    * @param st State Estado actual del nodo.
    * @param g  coste real para llegar al nodo.
    * @param f  suma del coste real mas la estimación de llegar al objetivo.
    * @param parent Nodo padre
    * @param lastAction Acción realizada para llegar a ese nodo.
    */
    public Node(State st, int g, int f, Node parent, ACTIONS lastAction) {
        this.st = st;
        this.g = g;
        this.f = f;
        this.parent = parent;
        this.lastAction = lastAction;
    }

    /**
    * Constructor de la clase Node para el nodo inicial del A*.
    *
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    * @param st State Estado actual del nodo.
    * @param g  coste real para llegar al nodo.
    * @param f  suma del coste real mas la estimación de llegar al objetivo.
    */
    public Node(State st, int g, int f) {
        this.st = st;
        this.g = g;
        this.f = f;

        this.parent = null;
        this.lastAction = null;
    }

    /**
     * Método que devuelve la lista de acciones realizadas para llegar nodo actual.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return LinkedList lista de acciones realizadas para llegar al nodo actual.
     */
    public LinkedList<ACTIONS> getPath()
    {
        Node current = this;
        LinkedList<ACTIONS> actions = new LinkedList<>();

        while (current.parent != null)
        {
            actions.addFirst(current.lastAction);
            current = current.parent;
        }

        return actions;
    }

    /**
     * Metodo que iguala el nodo con el objeto (parametro)
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @param o Objeto a comparar.
     * @return True si el nodo es igual al objeto, false si no son iguales
     */ 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return st.equals(node.st);
    }

    /**
     * Metodo que genera el hash dado el estado del nodo.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return El hash generado por el estado.
     */
    @Override
    public int hashCode() {
        return Objects.hash(st);
    }

    
    /**
     * Metodo que compara si el nodo actual es igual a otro estado pasado por parametro mirando si sus argumentos son iguales.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @param o Node a comparar
     * @return True si los argumentos son iguales, false si son diferentes.
     */ 
    public int compareTo(Node o)
    {
        int compare_cost = Integer.compare(f, o.f);

        if (compare_cost == 0)
        {
            return st.compareTo(o.getSt());
        }
        else
            return compare_cost;
    }

    /**
     * Metodo que devuelve el estado del nodo.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve el estado del nodo.
     */ 
    public State getSt() {
        return st;
    }

    /**
     * Metodo que devuelve el coste real del nodo.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve el coste real del nodo.
     */ 
    public int getG() {
        return g;
    }

    /**
     * Metodo que devuelve la suma de el coste real mas la estimación de llegar al objetivo.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve la suma de el coste real mas la estimación de llegar al objetivo.
     */ 
    public double getF() {
        return f;
    }
}
