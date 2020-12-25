package practica2;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;

import static practica2.Types.*;

 /**
  * Clase encargada de leer los sensores y moverse por el mundo
  * Extendida de la clase IntegratedAgent
  *
  * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
  * @version Practica 2 (4.0)
  */
public class MyWorldExplorer extends IntegratedAgent {

    enum Estado {
        LOGIN,
        SENSORS,
        PLAN,
        RECHARGE,
        ACTION,
        LOGOUT,
    }

    // Estado interno del agente
    private Estado estado = Estado.LOGIN;
    private LinkedList<ACTIONS> plan = new LinkedList<>();
    private Boolean recargando = false;

    // Estado del dron en el mundo
    private int position[] = new int[3];
    private int orientation;
    private int energy = 1000;

    // Información sobre el mundo
    private int target_position[] = new int[3];
    private int map[][];
    private int width;
    private int height;
    private int maximaAltura;
    private Boolean target_xy_known = false;
    private Boolean target_z_known = false;

    // Paso de mensajes
    private String key;
    private ACLMessage out = new ACLMessage();
    private ACLMessage in;

    String receiver;

    /**
    * Método encargado de iniciar la comunicación con el servidor.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */
    @Override
    public void setup() {
        super.setup();
        doCheckinPlatform();
        doCheckinLARVA();
        receiver = this.whoLarvaAgent();
        out.setSender(getAID());
        out.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        _exitRequested = false;
    }

    /**
    * Método que va ejecutando diferentes metodos dependiendo del estado en el que este el dron
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */
    @Override
    public void plainExecute() {
        switch (estado) {
            case LOGIN:
                login();
                break;
            case SENSORS:
                readSensors();
                break;
            case PLAN:
                makePlan();
                break;
            case RECHARGE:
                makePlanRecharge();
                break;
            case ACTION:
                doAction();
                break;
            case LOGOUT:
                logout();
                break;
        }
    }

    /**
    * Método encargado de desconectar del servicio de Larva
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */
    @Override
    public void takeDown() {
        this.doCheckoutLARVA();
        this.doCheckoutPlatform();
        super.takeDown();
    }

    /**
    * Método encargado loguear al agente en el mapa que se seleccione e inicializa los sensore elegidos.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso. 
    * @version Practica 2 (2.0)
    */
    private void login() {
        // Mandar mensaje de login con sensores
        JsonArray sensors = new JsonArray();
        sensors.add("gps").add("compass").add("distance").add("angular").add("visual");

        JsonObject msg = new JsonObject();
        msg.add("command", "login");
        msg.add("world", "Link@Playground1");
        msg.add("attach", sensors);
        sendReply(msg);

        // Parsear respuesta
        String answer = receiveAnswer();
        Info("MyWorldManager dice: " + answer);
        JsonObject respuesta = Json.parse(answer).asObject();
        key = respuesta.getString("key", "");
        width = respuesta.get("width").asInt();
        height = respuesta.get("height").asInt();
        maximaAltura = respuesta.get("maxflight").asInt();

        // Inicializar mapa
        map = new int[height][width];
        for (int i = 0; i < height; i++) {
            Arrays.fill(map[i], -1);
        }

        // Cambiar estado
        if (respuesta.get("result").asString().equals("ok")) {
            estado = Estado.SENSORS;
        } else {
            _exitRequested = true;
        }
    }
    
    
    /**
    * Método encargado de realizar el plan y actualizar el estado del dron.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (2.0)
    */
    private void makePlan() {

        if (!target_z_known) {
            int restoMaxAlt = maximaAltura % 5;
            
            if (restoMaxAlt == 0) {
                restoMaxAlt = 5;
            }
            
            target_position[Z] = maximaAltura - restoMaxAlt;
        }
        
        plan.clear();

        Node initial = new Node(new State(position, orientation), 0, distancia(position, target_position));
        plan = findPath(initial, target_position);

        // Cambiar estado
        if (plan == null) {
            Info("No es posible llegar al objetivo");
            estado = Estado.LOGOUT;
        } else {
            estado = Estado.ACTION;
        }
    }

    /**
    * Actualiza el mapa del dron con las alturas leidas del visual.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (2.0)
    * @param  visual[][] una matirz de enteros que contiene la información de la altura alrededor del dron.
    */
    private void copyVisualToMap(int visual[][]) {
        int corner_i = position[Y] - 3;
        int corner_j = position[X] - 3;
        int map_i, map_j;

        for (int i = 0; i < 7; i++) {
            map_i = corner_i + i;
            
            for (int j = 0; j < 7; j++) {
                map_j = corner_j + j;
                
                if (map_j < width && map_i < height && map_j >= 0 && map_i >= 0) {
                    map[map_i][map_j] = visual[i][j];
                }
            }
        }
    }

    /**
    * Método encargado de leer los sensores que se seleccionan para el dron.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (3.0)
    */
    private void readSensors() {
        energy -= 5;

        JsonObject msg = new JsonObject();
        msg.add("command", "read");
        msg.add("key", key);
        sendReply(msg);

        String answer = receiveAnswer();
        Info("Sensores: " + Json.parse(answer).asObject().toString());
        JsonObject respuesta = Json.parse(answer).asObject();

        double distancia = 0.0;
        double direccionObjetivo = 0;
        int visual[][] = new int[7][7];
        JsonArray perceptions = respuesta.get("details").asObject().get("perceptions").asArray();
        for (JsonValue perception : perceptions) {
            JsonArray data;
            switch (perception.asObject().get("sensor").asString()) {
                case "angular":
                    direccionObjetivo = perception.asObject().get("data").asArray().get(0).asDouble();
                    break;
                case "compass":
                    orientation = (int) Math.round(perception.asObject().get("data").asArray().get(0).asDouble());
                    break;
                case "distance":
                    distancia = perception.asObject().get("data").asArray().get(0).asDouble();
                    break;
                case "gps":
                    data = perception.asObject().get("data").asArray().get(0).asArray();
                    position[X] = data.get(0).asInt();
                    position[Y] = data.get(1).asInt();
                    position[Z] = data.get(2).asInt();
                    break;
                case "visual":
                    for (int i = 0; i < 7; i++) {
                        data = perception.asObject().get("data").asArray().get(i).asArray();
                        for (int j = 0; j < 7; j++) {
                            visual[i][j] = data.get(j).asInt();
                        }
                    }
                    break;
            }
        }

        copyVisualToMap(visual);

        // Calcular posicion del objetivo (target_position) con direccionObjetivo y distancia
        if (!target_xy_known) {
            target_position[X] = position[X] + (int) Math.round(Math.sin(gradToRad(direccionObjetivo)) * distancia);
            target_position[Y] = position[Y] - (int) Math.round(Math.cos(gradToRad(direccionObjetivo)) * distancia);
            target_xy_known = true;
            Info("Objetivo: " + target_position[X] + " " + target_position[Y]);
        }

        if (!target_z_known && map[target_position[Y]][target_position[X]] >= 0) {
            target_position[Z] = map[target_position[Y]][target_position[X]];
            target_z_known = true;
            Info("Objetivo: " + target_position[X] + " " + target_position[Y] + " " + target_position[Z]);
        }

        // Estado
        String result = respuesta.get("result").asString();
        if (result.equals("ok")) {
            if (Arrays.equals(position, target_position)) {
                estado = Estado.LOGOUT;
            } else {
                estado = Estado.PLAN;
            }
        } else if (result.equals("error")) {
            estado = Estado.LOGOUT;
        }
    }
    
    /**
    * Método encargado de enviar el mensaje al agente para que realice la acción generadas en el plan.
    * Tambien se encarga de comprobar si debe aterrizar o si debe volver a leer los sensores actualizando el estado del dron.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (4.0)
    */
    private void doAction() {
        ACTIONS action = plan.peekFirst();
        int next_energy = energy;
        
        if (action != null) {
            switch (action) {
            case recharge:
                recargando = false;
                next_energy = 1000;
                break;
            case moveD:
            case moveUP:
            case touchD:
                next_energy -= 5;
                break;
            default:
                next_energy -= 1;
                break;
            }
        }

        
        if (Arrays.equals(position, target_position)) {
            estado = Estado.LOGOUT;
        }
        else if (plan.isEmpty()) {
            estado = Estado.PLAN;
        } else {
            State actual_st = new State(position, orientation);
            State next_st = actual_st.simulateAction(action, map, maximaAltura, target_position);
            int altura = map[next_st.getY()][next_st.getX()];

            if (!recargando && next_energy < 50 && actual_st.getZ() - map[actual_st.getY()][actual_st.getX()] == 0) {
                plan.addFirst(ACTIONS.recharge);
            }
            else if (!recargando && next_st == null || altura == -1) {
                if (position[Z] - map[position[Y]][position[X]] + 20 >= energy - 5) {
                    estado = Estado.RECHARGE;
                    recargando = true;
                } else {
                    estado = Estado.SENSORS;
                }
            } else if (!recargando && (next_st.getZ() - altura) + 20 >= next_energy) {
                estado = Estado.RECHARGE;
                recargando = true;
            } else {
                plan.removeFirst();
                estado = Estado.ACTION;
                
                // Ejecutar acción
                JsonObject msg = new JsonObject();
                msg.add("command", "execute");
                msg.add("key", key);
                msg.add("action", action.toString());
                sendReply(msg);

                String answer = receiveAnswer();
                Info("Acción: " + action.toString() + Json.parse(answer).asObject().toString());
                JsonObject respuesta = Json.parse(answer).asObject();
                
                // Actualizar estado del dron
                position = next_st.getPosition().clone();
                orientation = next_st.getOrientation();
                energy = next_energy;

                Info("Posición Dron: " + position[X] + " " + position[Y] + " " + position[Z]);
                Info("Altura mapa: " + map[position[Y]][position[X]]);
                Info("Energía: " + energy);
                
                if (respuesta.get("result").asString().equals("error")) {
                    estado = Estado.LOGOUT;
                }
            }
        }
    }

    /**
    * Método encargado de desloguearse .
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */
    private void logout() {
        JsonObject msg = new JsonObject();
        msg.add("command", "logout");
        msg.add("key", key);
        sendReply(msg);

        _exitRequested = true;
    }

    /**
    * Método encargado de encontrar el camino optimo desde la posicion del dron hasta la posicion destino previamente calculada.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    * @param initial Nodo en el que se encuentra el dron.
    * @param target[] vector que contiene la posicion X, Y y Z estimadas del objetivo.
    * @return LinkedList<ACTIONS> que contiene una lista de las acciones ha realizar por el dron para llegar al objetivo.
    */
    private LinkedList<ACTIONS> findPath(Node initial, int target[]) {
        ArrayList<ACTIONS> available_actions = new ArrayList<>(Arrays.asList(ACTIONS.moveF, ACTIONS.rotateL, ACTIONS.rotateR, ACTIONS.moveUP, ACTIONS.moveD, ACTIONS.touchD));
        TreeMap<Node, Node> open = new TreeMap<>();
        HashSet<Node> closed = new HashSet<>();
        Node current;

        open.put(initial, initial);

        while (!open.isEmpty()) {
            current = open.pollFirstEntry().getKey();
            closed.add(current);
            // Si el nodo actual es solución lo devolvemos
            if (Arrays.equals(target, current.getSt().getPosition())) {
                return current.getPath();
            }
            // Generamos hijos
            for (ACTIONS action : available_actions) {
                State new_state = current.getSt().simulateAction(action, map, maximaAltura, target_position);

                if (new_state != null) {
                    int cost;
                    if (action == ACTIONS.moveD || action == ACTIONS.moveUP || action == ACTIONS.touchD) {
                        cost = current.getG() + 5;
                    } else {
                        cost = current.getG() + 1;
                    }

                    Node children = new Node(new_state, cost, cost + distancia(new_state.getPosition(), target), current, action);

                    if (!closed.contains(children)) {
                        open.put(children, children);
                    }
                }
            }
        }
        return null;
    }

    /**
    * Método que sirve para calcular la heuristica del A*
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (2.0)
    * @param pos_inicial[] posicion inicial para realizar el calculo necesario.
    * @param pos_final[] posición donde queremos llegar.
    * @return Devuelve la mayor distancia calculada entre la distancia de X y la distancia de Y al objetivo mas la distancia Z.
    */
    public int distancia(int pos_inicial[], int pos_final[]) {
        int distancia_x = Math.abs(pos_final[X] - pos_inicial[X]);
        int distancia_y = Math.abs(pos_final[Y] - pos_inicial[Y]);
        int distancia_z = Math.abs(pos_final[Z] - pos_inicial[Z]);
        int distancia_xy = (distancia_x > distancia_y) ? distancia_x : distancia_y;

        return distancia_xy + distancia_z;
    }
    
    /**
    * Método encargado de hacer que el dron aterrice y recarque.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (3.0)
    */  
    public void makePlanRecharge() {
        plan.clear();

        int costeAterrizaje = position[Z] - map[position[Y]][position[X]];

        while (costeAterrizaje > 0) {
            if (costeAterrizaje >= 5) {
                plan.add(ACTIONS.moveD);
                costeAterrizaje -= 5;
            } else {
                plan.add(ACTIONS.touchD);
                costeAterrizaje -= 1;
            }
        }

        plan.add(ACTIONS.recharge);

        estado = Estado.ACTION;
    }

    /**
    * Método encargado de enviar la respuesta del agente al servidor.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */ 
    private void sendReply(JsonObject msg) {
        if (in != null) {
            out = in.createReply();
        }
        out.setContent(msg.toString());
        this.sendServer(out);
    }

    /**
    * Método encargado de recivir la respuesta del servidor.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */ 
    private String receiveAnswer() {
        in = this.blockingReceive();
        return in.getContent();
    }
}
