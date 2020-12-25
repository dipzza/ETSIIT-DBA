package practica2;

 /**
  * Clase para tener las variables definidas y guardar las acciones que puede realizar el dron.
  * Esta clase nos permite un uso de las variables mas ordenado a la hora de llamarlas en el resto de métodos de las otras clases.
  *
  * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
  * @version Practica 2 (1.0)
  */
public class Types {
    private Types(){}
    
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    
    public static final int NORTH = 0;
    public static final int NORTHEAST = 45;
    public static final int EAST = 90;
    public static final int SOUTHEAST = 135;
    public static final int SOUTH = 180;
    public static final int SOUTHWEST = -135;
    public static final int WEST = -90;
    public static final int NORTHWEST = -45;
    
    
    public static enum ACTIONS {
        moveF,
        rotateL,
        rotateR,
        moveUP,
        moveD,
        touchD,
        recharge
    }

     /**
    * Método encargado de pasar de grados a radianes.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    * @param g un double que contiene los grados a convertir.
    * @return los grados ya pasados a radianes.
    */
    public static double gradToRad(double g) {
        return (g * Math.PI / 180.0);
    }
}