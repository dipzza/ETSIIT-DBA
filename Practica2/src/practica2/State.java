package practica2;

import static practica2.Types.*;
import static practica2.Types.ACTIONS.*;
import java.util.Arrays;

/**
 * Clase encargada de simular acciones que nos sirve para la creacion del plan del A*.
 * Implementa la interfaz Comparable para poder comparar estados.
 * 
 * @author Javier, Jose Miguel, Alvaro y Bryan.
 * @version Practica 2 (4.0)
 */
public class State implements Comparable<State> {
    private final int[] position;
    private final int orientation;

    /**
    * Constructor de la clase State
    *
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    * @param position[] vector de int que contiene la posicion X, Y y Z del estado.
    * @param orientation int que contiene la orientación del estado.
    */
    public State(int[] position, int orientation) {
        this.position = position.clone();
        this.orientation = orientation;
    }

    public static final int NORTH = 0;
    public static final int NORTHEAST = 45;
    public static final int EAST = 90;
    public static final int SOUTHEAST = 135;
    public static final int SOUTH = 180;
    public static final int SOUTHWEST = -135;
    public static final int WEST = -90;
    public static final int NORTHWEST = -45;

    /**
     * Simula las consequencias de realizar una acción
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (4.0)
     * @param action Acción ha realizar.
     * @param map[][] matriz que contiene las alturas del mapa leidas y -1 las posiciones que aun no son leidas por los sensores.
     * @param maximaAltura Altura maxima del mundo.
     * @param target_position[] vector que contiene la posicion estimada del objetivo.
     * @return El estado resultante de realizar la acción, si no se puede realizar devuelve null.
     * @throws ArrayIndexOutOfBoundsException que indica si se ha accedido a una posición invalida del mapa.
     */
    public State simulateAction(ACTIONS action, int[][] map, int maximaAltura, int target_position[])
    {
        int[] new_position = position.clone();
        int new_orientation = orientation;

        switch (action) {
            case moveF:
                switch(orientation) {
                    case NORTH:
                        new_position[Y] -= 1;
                    break;
                    case NORTHEAST:
                        new_position[X] += 1;
                        new_position[Y] -= 1;
                    break;
                    case EAST:
                        new_position[X] += 1;
                    break;
                    case SOUTHEAST:
                        new_position[X] += 1;
                        new_position[Y] += 1;
                    break;
                    case SOUTH:
                        new_position[Y] += 1;
                    break;
                    case SOUTHWEST:
                        new_position[X] -= 1;
                        new_position[Y] += 1;
                    break;
                    case WEST:
                        new_position[X] -= 1;
                    break;
                    case NORTHWEST:
                        new_position[X] -= 1;
                        new_position[Y] -= 1;
                    break;
                }
                break;
            case rotateL:
                if (orientation == SOUTHWEST)
                    new_orientation = SOUTH;
                else
                    new_orientation = orientation - 45;
                break;
            case rotateR:
                if (orientation == SOUTH)
                    new_orientation = SOUTHWEST;
                else
                    new_orientation = orientation + 45;
                break;
            case moveUP:
                new_position[Z] += 5;
                break;
            case moveD:
                new_position[Z] -= 5;
                break;
            case touchD:
                new_position[Z] -= 1;
                break;
            case recharge:
                return new State(new_position, new_orientation);
            default:
                break;
        }
        
        try {
            int height = map[new_position[Y]][new_position[X]];
            
            if (action == touchD && new_position[Z] - height >= 5) {
                return null;
            }
            if (new_position[Z] >= height && new_position[Z] < maximaAltura && height >= -1)
                return new State(new_position, new_orientation);
            else
                return null;
        } catch(ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Metodo que transforma un array a hash
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return El hash resultante de transformar la posicion en codigo hash
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Arrays.hashCode(this.position);
        hash = 97 * hash + this.orientation;
        return hash;
    }
  
    /**
     * Metodo que iguala el estado con el objeto (parametro)
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @param obj Objeto a comparar.
     * @return True si el estado es igual al objeto, false si no son iguales
     */ 

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final State other = (State) obj;
        if (this.orientation != other.orientation) {
            return false;
        }
        if (!Arrays.equals(this.position, other.position)) {
            return false;
        }
        return true;
    }
    
    
    /**
     * Metodo que devuelve la posición del estado
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve el vector posición del estado.
     */ 
    public int[] getPosition() {
        return position;
    }

    /**
     * Metodo que devuelve la orientación del estado.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve la orientación del estado.
     */ 
    public int getOrientation() {
        return orientation;
    }
    
    /**
     * Metodo que devuelve la posición X del estado
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve la posición X del estado.
     */ 
    public int getX() {
        return position[X];
    }
    
        /**
     * Metodo que devuelve la posición Y del estado
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve la posición Y del estado.
     */ 
    public int getY() {
        return position[Y];
    }
    
    /**
     * Metodo que devuelve la posición Z del estado
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @return Devuelve la posición Z del estado.
     */ 
    public int getZ() {
        return position[Z];
    }

    /**
     * Metodo que compara si el estado actual es igual a otro estado pasado por parametro mirando si sus argumentos son iguales.
     * 
     * @author Javier, Jose Miguel, Alvaro y Bryan.
     * @version Practica 2 (1.0)
     * @param state State a comparar
     * @return True si los argumentos son iguales, false si son diferentes.
     */ 
    @Override
    public int compareTo(State state) {
        int compare_x = Integer.compare(position[X], state.position[X]);

        if (compare_x == 0)
        {
            int compare_y = Integer.compare(position[Y], state.position[Y]);

            if (compare_y == 0)
                return Integer.compare(orientation, state.orientation);
            else
                return compare_y;
        }
        else
            return compare_x;
    }
}