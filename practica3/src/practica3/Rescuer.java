package practica3;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import static ACLMessageTools.ACLMessageTools.getJsonContentACLM;
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


public class Rescuer extends AgentInterface {
    // Estado interno del agente
    private LinkedList<ACTIONS> plan = new LinkedList<>();
    private Boolean recargando = false;

    // Estado del dron en el mundo
    private int position[] = new int[3];
    private int orientation;
    private int energy = 10;

    // Información sobre el mundo
    private int target_position[] = new int[3];
    private int local_map[][];
    private int width;
    private int height;

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
                // Wait for CONV-ID and map from COACH
                in = blockingReceive();
                myError = in.getPerformative() != ACLMessage.PROPAGATE;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Expected CONV-ID failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
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
                        Info("Sampling three random points for cross-check:");
                        int px, py;
                        for (int ntimes = 0; ntimes < 3; ntimes++) {
                            px = (int) (Math.random() * myMap.getWidth());
                            py = (int) (Math.random() * myMap.getHeight());
                            Info("\tX: " + px + ", Y:" + py + " = " + myMap.getLevel(px, py));
                        }
                    }
                }
                
                int a = myMap.getWidth();

                // Calcular submapa local y posición relativa en este
                switch(getLocalName()) {
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
                        
                        for (int i = 0; i < myMap.getWidth(); i++){
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
                
                position[X] = width/2;
                position[Y] = height/2;

                myStatus = "SUBSCRIBE-SESSION";
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
                in = blockingReceive();
                if (in.getProtocol().equals("REGULAR")) {
                    myError = in.getPerformative() != ACLMessage.INFORM;
                    if (myError) {
                        Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                                + "Getting sensors failed due to " + getDetailsLARVA(in));
                        myStatus = "CHECKOUT-SESSION";
                        break;
                    }
                    JsonObject answer = Json.parse(in.getContent()).asObject();
                    JsonArray sensors = answer.get("sensors").asArray();
                    

                    Info("Sensors and World Size received. Login to world");
                    int x = 0, y = 0;

                    switch(getLocalName()) {
                        /*case "SeñorBusca1": x = 0; y = 0; break;
                        case "SeñorBusca2": x = myMap.getWidth(); y = 0; break;
                        case "SeñorBusca3": x = 0; y = myMap.getHeight(); break;
                        case "SeñorBusca4": x = myMap.getWidth(); y = myMap.getHeight(); break;*/
                        case "SeñorBusca1":
                            x = myMap.getWidth()/4; y = myMap.getHeight()/4;
                        break;
                        case "SeñorBusca2":
                            x = (3*myMap.getWidth())/4; y = myMap.getHeight()/4;
                        break;
                        case "SeñorBusca3":
                            x = myMap.getWidth()/4; y = (3*myMap.getHeight())/4;
                        break;
                        case "SeñorBusca4":
                            x = (3*myMap.getWidth())/4; y = (3*myMap.getHeight())/4;
                        break;
                    }

                    in = sendLogin(sensors, x, y);

                    myStatus = "SENSORS";

                } else if (in.getProtocol().equals("BROADCAST")) {
                    System.out.println("no ta hecho");
                } else {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " LogIn failure due to: " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-SESSION";
                    break;
                }
                break;
                
            
            case "SENSORS":
                readSensors();
                break;
                
            case "CHECKOUT-SESSION":
                Info("Exit SESSION");
                in = sendCANCELWM(sessionConvID);
                myStatus = "CHECKOUT-LARVA";
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

            case "PLAN":
                makePlan();
            break;

            case "doAction":
                doAction();
            break;
            
            
        }
    } 

    private void makePlan() {
        
        plan.clear();

        Node initial = new Node(new State(position, orientation), 0, distancia(position, target_position));
        plan = findPath(initial, target_position);

        // Cambiar estado
        if (plan == null) {
            Info("No es posible llegar al objetivo");
            myStatus = "EXIT";
        } else {
            myStatus = "ACTION";
        }
    }

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
        JsonArray perceptions = respuesta.get("result").asObject().get("perceptions").asArray();
        for (JsonValue perception : perceptions) {
            JsonArray data;
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
        target_position[Z] = local_map[target_position[X]][target_position[Y]];
        
        Info("Objetivo: " + target_position[X] + " " + target_position[Y] + " " + target_position[Z]);
        if (target_position[X] >= width || target_position[Y] >= height) {
            if (position[X] == width/2 && position[Y] == height/2) {
                in = this.blockingReceive();
            } 
            else {
                target_position[X] = width/2;
                target_position[Y] = height/2;
                myStatus = "PLAN";
                makePlan();
            }
        }
        
        // Estado
        String result = respuesta.get("result").asString();
        if (result.equals("ok")) {
            if (Arrays.equals(position, target_position)) {
                rescueGerman();
            } else {
                myStatus = "PLAN";
            }
        } else if (result.equals("error")) {
            myStatus = "LOGOUT";
        }
    }

    private void rescueGerman() {
        sendOperation("rescue");
    }

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
                State new_state = current.getSt().simulateAction(action, local_map, myMap.getMaxHeight(), target_position);

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

    public int distancia(int pos_inicial[], int pos_final[]) {
        int distancia_x = Math.abs(pos_final[X] - pos_inicial[X]);
        int distancia_y = Math.abs(pos_final[Y] - pos_inicial[Y]);
        int distancia_z = Math.abs(pos_final[Z] - pos_inicial[Z]);
        int distancia_xy = (distancia_x > distancia_y) ? distancia_x : distancia_y;

        return distancia_xy + distancia_z;
    }
    
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
            myStatus = "LOGOUT";
        }
        else if (plan.isEmpty()) {
            myStatus = "PLAN";
        } else {
            State actual_st = new State(position, orientation);
            State next_st = actual_st.simulateAction(action, local_map, myMap.getMaxHeight(), target_position);
            int altura = local_map[next_st.getY()][next_st.getX()];

            if (!recargando && next_energy < 50 && actual_st.getZ() - local_map[actual_st.getY()][actual_st.getX()] == 0) {
                plan.addFirst(ACTIONS.recharge);
            }
            else if (!recargando && next_st == null || altura == -1) {
                if (position[Z] - local_map[position[Y]][position[X]] + 20 >= energy - 5) {
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
                in = sendOperation(action.toString());
                Info("Acción: " + action.toString() + Json.parse(in.getContent()).asObject().toString());
                JsonObject respuesta = Json.parse(in.getContent()).asObject();
                
                // Actualizar estado del dron
                position = next_st.getPosition().clone();
                orientation = next_st.getOrientation();
                energy = next_energy;

                Info("Posición Dron: " + position[X] + " " + position[Y] + " " + position[Z]);
                Info("Altura mapa: " + local_map[position[Y]][position[X]]);
                Info("Energía: " + energy);
                
                if (respuesta.get("result").asString().equals("error")) {
                    myStatus = "LOGOUT";
                }
            }
        }
    }

    /**
    * Método encargado de hacer que el dron aterrice y recarque.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (3.0)
    */  
    public void makePlanRecharge() {
        plan.clear();

        int costeAterrizaje = position[Z] - local_map[position[Y]][position[X]];

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
}