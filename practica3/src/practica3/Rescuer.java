package practica3;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import static ACLMessageTools.ACLMessageTools.getJsonContentACLM;
import Geometry.Point;
import static practica3.Types.*;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import YellowPages.YellowPages;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import Map2D.Map2DGrayscale;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Arrays;

/**
 * Clase que implementa los rescuers encargados de rescatar los alemanes.
 * Extendida de la clase AgentInterfa 
 *
 * @author Javier, Jose Miguel, Alvaro y Bryan.
 * @version Practica 3 (1.0)
 */
 
public class Rescuer extends AgentInterface {
    // Estado interno del agente
    private LinkedList<ACTIONS> plan = new LinkedList<>();
    private Boolean recargando = false;
    private boolean target_german = true;

    // Estado del dron en el mundo
    private int position[] = new int[3];
    private int orientation = 90;
    private int energy = 10;

    // Información sobre el mundo
    private int target_position[] = new int[3];
    private int local_map[][];
    private int width;
    private int height;
    private double _distance;

    /**
    * Método que va ejecutando diferentes metodos dependiendo del estado en el que este el dron
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */

    @Override
    protected void plainExecute() {
        switch (myStatus.toUpperCase()) {
            case "CHECKIN-LARVA":
                Info("Checkin in LARVA with " + _identitymanager);
                in = sendCheckinLARVA(_identitymanager); // As seen in slides
                myError = (in.getPerformative() != ACLMessage.INFORM);
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())

                            + " Checkin failed due to " + getDetailsLARVA(in));
                    myStatus = "EXIT";
                    break;
                }
                myStatus = "PROCESS-MAP";
                Info("\tCheckin ok");
                break;

            case "PROCESS-MAP":
                saveMap();
            break;

            case "SUBSCRIBE-SESSION":
                Info("Retrieve who is my WM");
                // First update Yellow Pages
                in = queryYellowPages(_identitymanager); // As seen oon slides
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Query YellowPages failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                myYP = new YellowPages();
                myYP.updateYellowPages(in);
                // It might be the case that YP are right but we dont find an appropriate service for us, then leave
                if (myYP.queryProvidersofService(myService).isEmpty()) {
                    Info("\t" + "There is no agent providing the service " + myService);
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                // Choose one of the available service providers, i.e., the first one
                myWorldManager = myYP.queryProvidersofService(myService).iterator().next();

                // Suscribe SESSION
                in = sendSubscribeSession("RESCUER");
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Suscribe SESSION failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }

                sendMsg("ElPepe", ACLMessage.INFORM, "REGULAR", in.getContent());

                myStatus = "LOGIN";
                break;
            
            case "LOGIN":
                doLogin();
                break;
            
            case "PLAN":
                makePlan();
                break;

            case "ACTION":
                doAction();
                break;

            case "RECHARGE":
                makePlanRecharge();
                break;
            
            case "SENSORS":
                readSensors();
                break;
                
            case "CHECKOUT-LARVA":
                Info("Exit LARVA");
                in = sendCheckoutLARVA(_identitymanager);
                myStatus = "EXIT";
                sendMsg("ElPepe", ACLMessage.INFORM, "REGULAR", "FinalSession");
                break;
            
            case "EXIT":
                Info("The agent dies");
                _exitRequested = true;
                break;
        }
    }

    /**
    * Metodo que dibide el mapa en 4 cuadrantes y los guarda localmente para cada uno de los rescuers
    * 
    * @author Javier, Jose Miguel, Alvaro y Bryan.
    * @version Practica 3 (1.0)
    */

    private void saveMap() {
        // Wait for CONV-ID and map from COACH
        in = blockingReceive();

        if (in.getPerformative() != ACLMessage.PROPAGATE) {
            Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                    + " Expected CONV-ID failed due to " + getDetailsLARVA(in));
            myStatus = "CHECKOUT-LARVA";
            return;
        }
        this.sessionConvID = in.getConversationId();

        System("Save map of world " + myWorld);
        // Examines the content of the message from server
        JsonObject jscontent = getJsonContentACLM(in);
            if (jscontent.names().contains("map")) {
                JsonObject jsonMapFile = jscontent.get("map").asObject();
                String mapfilename = jsonMapFile.getString("filename", "nonamefound");
                Info("Found map " + mapfilename);
                myMap = new Map2DGrayscale();
                if (myMap.fromJson(jsonMapFile)) {
                    Info("Map " + mapfilename + "( " + myMap.getWidth() + "cols x" + myMap.getHeight()
                            + "rows ) saved on disk (project's root folder) and ready in memory");
                }
            }
        
            sendMsg("ElPepe", ACLMessage.CONFIRM, "REGULAR", "");

        // Calcular submapa local y posición relativa en este
        switch(getLocalName()) {

            // if(myMap.getWidth()%2==0) width = myMap.getWidth()/2;
            // else width = myMap.getWidth() - myMap.getWidth()/2;
            // if(myMap.getHeight()%2==0) height = myMap.getHeight()/2;
            // else height = myMap.getHeight() - myMap.getHeight()/2;
            // local_map = new int[width][height];
            
            case "SeñorBusca1":
                width = myMap.getWidth()/2;
                height = myMap.getHeight()/2;
                local_map = new int[width][height];

                for (int i = 0; i < myMap.getWidth()/2; i++){
                    for (int j = 0; j < myMap.getHeight()/2; j++) {
                        local_map[i][j] = myMap.getLevel(i, j);
                    }
                }
            break;

            case "SeñorBusca2": 
                width = myMap.getWidth() - myMap.getWidth()/2;
                height = myMap.getHeight()/2;
                local_map = new int[width][height];
                
                for (int i = myMap.getWidth()/2; i < myMap.getWidth(); i++){
                    for (int j = 0; j < myMap.getHeight()/2; j++) {
                        local_map[i-(myMap.getWidth()/2)][j] = myMap.getLevel(i, j);
                    }
                }
            break;

            case "SeñorBusca3":
                width = myMap.getWidth()/2;
                height = myMap.getHeight() - myMap.getHeight()/2;
                local_map = new int [width][height];
                
                for (int i = 0; i < myMap.getWidth()/2; i++){
                    for (int j = myMap.getHeight()/2; j < myMap.getHeight(); j++) {
                        local_map[i][j-(myMap.getHeight()/2)] = myMap.getLevel(i, j);
                    }
                }
                
            break;

            case "SeñorBusca4":
                width = myMap.getWidth() - myMap.getWidth()/2;
                height = myMap.getHeight() - myMap.getHeight()/2;
                local_map = new int[width][height];
                
                for (int i = myMap.getWidth()/2; i < myMap.getWidth(); i++){
                    for (int j = myMap.getHeight()/2; j < myMap.getHeight(); j++) {
                        local_map[i-(myMap.getWidth()/2)][j-(myMap.getHeight()/2)] = myMap.getLevel(i, j);
                    }
                }
            break;
        }
        
        position[X] = width/2 - 1;
        position[Y] = height/2 - 1;
        position[Z] = local_map[position[X]][position[Y]];

        myStatus = "SUBSCRIBE-SESSION";
    }

    /**
    * Método encargado de recibir sensores y recarga del coach.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */

    private void doLogin() {
        
        in = blockingReceive();
        if (in.getProtocol().equals("REGULAR")) {
            myError = in.getPerformative() != ACLMessage.INFORM;
            if (myError) {
                Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                        + "Getting sensors failed due to " + getDetailsLARVA(in));
                myStatus = "CHECKOUT-LARVA";
                return;
            }
            JsonObject answer = Json.parse(in.getContent()).asObject();
            JsonArray sensors = answer.get("sensors").asArray();
            

            Info("Sensors and World Size received. Login to world");
            int x = 0, y = 0;

            switch(getLocalName()) {
                case "SeñorBusca1":
                    x = myMap.getWidth()/4 - 1; y = myMap.getHeight()/4 - 1;
                break;
                case "SeñorBusca2":
                    x = (3*myMap.getWidth())/4 - 1; y = myMap.getHeight()/4 - 1;
                break;
                case "SeñorBusca3":
                    x = myMap.getWidth()/4 - 1; y = (3*myMap.getHeight())/4 - 1;
                break;
                case "SeñorBusca4":
                    x = (3*myMap.getWidth())/4 - 1; y = (3*myMap.getHeight())/4 - 1;
                break;
            }

            in = sendLogin(sensors, x, y);

            myStatus = "SENSORS";

        } else if (in.getProtocol().equals("BROADCAST")) {
            System.out.println("no ta hecho");
        } else {
            Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                    + " LogIn failure due to: " + getDetailsLARVA(in));
            myStatus = "CHECKOUT-LARVA";
            return;
        }
    }


    /**
    * Método que lee los sensores que se han comprado y calcula la posición del aleman que se ha encontrado, si se ha encontrado
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */

    private void readSensors() {
        if (myMap.getWidth() > 200 || myMap.getHeight() > 200) {
            energy -= 8;
        } else {
            energy -= 2;
        }

        in = sendRead();

        Info("Sensores: " + Json.parse(in.getContent()).asObject().toString());
        JsonObject respuesta = Json.parse(in.getContent()).asObject();

        double distancia = 0.0;
        double direccionObjetivo = 0;
        JsonArray perceptions = respuesta.get("details").asObject().get("perceptions").asArray();
        for (JsonValue perception : perceptions) {
            switch (perception.asObject().get("sensor").asString()) {
                case "angular":
                    direccionObjetivo = perception.asObject().get("data").asArray().get(0).asDouble();
                    break;
                case "distance":
                    distancia = perception.asObject().get("data").asArray().get(0).asDouble();
                    break;
            }
        }
        // Calcular posicion del objetivo (target_position) con direccionObjetivo y distancia
        target_position[X] = position[X] + (int) Math.round(Math.sin(gradToRad(direccionObjetivo)) * distancia);
        target_position[Y] = position[Y] - (int) Math.round(Math.cos(gradToRad(direccionObjetivo)) * distancia);

        boolean germanOut = (target_position[X] >= width || target_position[Y] >= height || target_position[X] < 0 || target_position[Y] < 0);
        if (!(germanOut))
            target_position[Z] = local_map[target_position[X]][target_position[Y]];
        
        Info("Objetivo: " + target_position[X] + " " + target_position[Y] + " " + target_position[Z]);
        
        // Estado
        String result = respuesta.get("result").asString();
        if (result.equals("ok")) {
            if (germanOut) {
                if (position[X] == width/2 - 1 && position[Y] == height/2 - 1) {
                    if (distancia < 0) {
                        sendOperation("touchD");
                        myStatus = "CHECKOUT-LARVA";
                    } else {
                        in = informWaiting();
                        if (in.getPerformative() == ACLMessage.REQUEST) {
                            myStatus = "SENSORS";
                        } else if (in.getPerformative() == ACLMessage.CANCEL) {
                            sendOperation("touchD");
                            myStatus = "CHECKOUT-LARVA";
                        }
                        else {
                            Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                                + "Waiting failed due to " + getDetailsLARVA(in));
                            myStatus = "CHECKOUT-LARVA";
                            return;
                        }
                    }
                }
                else {
                    target_position[X] = width/2 - 1;
                    target_position[Y] = height/2 - 1;
                    target_position[Z] = local_map[target_position[X]][target_position[Y]];
                    target_german = false;
                    myStatus = "PLAN";
                }
            } else {
                myStatus = "PLAN";
            }
        } else if (result.equals("error")) {
            myStatus = "CHECKOUT-LARVA";
        }
    }

    /**
    * Método encargado de realizar el plan y actualizar el estado del dron.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */

    private void makePlan() {
        
        plan.clear();

        Node initial = new Node(new State(position, orientation), 0, distancia(position, target_position));
        plan = findPath(initial, target_position);

        // Cambiar estado
        myStatus = "ACTION";
    }

    /**
    * Método encargado de realizar la primera accion en la lista del plan
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */
    
    private void doAction() {
        ACTIONS action = null;
        
        if (plan != null)
            action = plan.peekFirst();

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
                    next_energy -= 20;
                    break;
                default:
                    next_energy -= 4;
                    break;
            }
        }

        if (Arrays.equals(position, target_position)) {
            if (target_german) {
                in = sendOperation("rescue");
                if (in.getPerformative() != ACLMessage.INFORM) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                                    + "Rescue failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-SESION";
                    return;
                }
                informRescue();
            } else {
                target_german = true;
            }
            myStatus = "SENSORS";
        }
        else if (plan.isEmpty()) {
            myStatus = "PLAN";
        } else {
            State actual_st = new State(position, orientation);
            State next_st = actual_st.simulateAction(action, local_map, 256);
            int altura = local_map[next_st.getX()][next_st.getY()];

            if (!recargando && next_energy < 50 && actual_st.getZ() - local_map[actual_st.getX()][actual_st.getY()] == 0) {
                plan.addFirst(ACTIONS.recharge);
            }
            else if (!recargando && next_st == null || altura == -1) {
                if (position[Z] - local_map[position[X]][position[Y]] + 20 >= energy - 5) {
                    myStatus = "RECHARGE";
                    recargando = true;
                } else {
                    myStatus = "SENSORS";
                }
            } else if (!recargando && (next_st.getZ() - altura) + 20 >= next_energy) {
                myStatus = "RECHARGE";
                recargando = true;
            } else {
                plan.removeFirst();
                myStatus = "ACTION";
                
                // Ejecutar acción
                if (action == ACTIONS.recharge) {
                    in = requestCharge();
                    if (in.getPerformative() == ACLMessage.FAILURE) {
                        myStatus = "CHECKOUT-LARVA";
                        return;
                    } else {
                        in = requestRecharge(in.getContent());
                        if (in.getPerformative() != ACLMessage.INFORM) {
                            Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                                    + "Recharge failed due to " + getDetailsLARVA(in));
                            myStatus = "CHECKOUT-LARVA";
                            return;
                        }
                    }
                } else {
                    in = sendOperation(action.toString());
                    if (in.getPerformative() != ACLMessage.INFORM) {
                            Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                                    + "Action failed due to " + getDetailsLARVA(in));
                            myStatus = "CHECKOUT-LARVA";
                            return;
                    }
                    Info("Acción: " + action.toString() + Json.parse(in.getContent()).asObject().toString());
                    
                    
                    // Actualizar estado del dron
                    position = next_st.getPosition().clone();
                    orientation = next_st.getOrientation();
                }
                energy = next_energy;

                JsonObject respuesta = Json.parse(in.getContent()).asObject();

                Info("Posición Dron.: " + position[X] + " " + position[Y] + " " + position[Z]);
                Info("Energía: " + energy);
                
                if (respuesta.get("result").asString().equals("error")) {
                    myStatus = "CHECKOUT-SESION";
                }
            }
        }
    }
    
    /**
    * Método que sirve para calcular la heuristica del A*.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
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
    * Método encargado de encontrar el camino optimo desde la posicion del dron hasta la posicion destino previamente calculada.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
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
                State new_state = current.getSt().simulateAction(action, local_map, 256);

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
    * Método encargado de hacer que el dron aterrice y recarque.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */  
    public void makePlanRecharge() {
        plan.clear();

        int costeAterrizaje = position[Z] - local_map[position[X]][position[Y]];

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

        myStatus = "ACTION";
    }

    /**
    * Método encargado de mandar al WM los sensores que se han comprado y la posicion del dron
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param sensors Sensores que se han comprado.
    * @param x Posición horizontal en la que se encuentra el dron.
    * @param y Posición vertical en la que se encuentra el dron.
    * @return Respuesta del WM
    */ 

    private ACLMessage sendLogin(JsonArray sensors, int x, int y) {
        JsonObject content = new JsonObject();
        content.add("operation", "login");
        content.add("attach", sensors);
        content.add("posx", x);
        content.add("posy", y);

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.REQUEST);
        out.setConversationId(sessionConvID);
    
        send(out);
        return blockingReceive();
    }

    /**
    * Método encargado de mandar al WM la lectura de un mensaje.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @return Respùesta de WM.
    */ 

    private ACLMessage sendRead() {
        JsonObject content = new JsonObject().add("operation", "read");

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.QUERY_REF);
        out.setConversationId(sessionConvID);
        send(out);
        return blockingReceive();
    }

    /**
    * Método encargado de mandar al WM la operacion que se va a realizar
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @return Respuesta del WM
    */

    private ACLMessage sendOperation(String operation) {
        JsonObject content = new JsonObject().add("operation", operation);

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.REQUEST);
        out.setConversationId(sessionConvID);
        send(out);
        return blockingReceive();
    }

    /**
    * Método encargado de mandar al WM un mensaje cuando un dron quiere recargar.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @return Respuesta del WM
    */
    
    private ACLMessage requestRecharge(String charge) {
        JsonObject content = new JsonObject();
        content.add("operation", "recharge");
        content.add("recharge", charge);

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.REQUEST);
        out.setConversationId(sessionConvID);
        send(out);
        return blockingReceive();
    }

    /**
    * Método encargado de mandar al Coach para que le mande una recarga
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @return respuesta del coach.
    */

    private ACLMessage requestCharge() {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID("ElPepe", AID.ISLOCALNAME));
        out.setContent("RECARGA");
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.REQUEST);
        out.setConversationId(sessionConvID);
        send(out);
        return blockingReceive();
    }

    /**
    * Método encargado de informar que un dron esta esperando a que rescaten a un aleman que han encontrado
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @return Respuesta del coach.
    */

    private ACLMessage informWaiting() {
        JsonObject content = new JsonObject();
        content.add("state", "waiting");
        content.add("x", target_position[X]);
        content.add("y", target_position[Y]);

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID("ElPepe", AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.INFORM);
        out.setConversationId(sessionConvID);
        send(out);
        return blockingReceive();
    }
    
    private void informRescue() {
        JsonObject content = new JsonObject();
        content.add("state", "rescue");
        content.add("x", target_position[X]);
        content.add("y", target_position[Y]);

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID("ElPepe", AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.INFORM);
        out.setConversationId(sessionConvID);
        send(out);
    }
}