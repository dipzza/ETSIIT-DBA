package Practica1;

import IntegratedAgent.IntegratedAgent;
import LarvaAgent.LarvaAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.LinkedList;

public class MyWorldExplorer extends IntegratedAgent {
    
    enum Estado {
        LOGIN,
        SENSORS,
        ACTION,
        LOGOUT,
    }
    
    private Estado estado = Estado.LOGIN;
    
    // Para almacenar sensores
    private int x, y, z;
    private int energy = 0;
    private int [][] visual = new int[7][7];
    
    private String key;
    private ACLMessage out = new ACLMessage();
    private ACLMessage in;

    String receiver;

    @Override
    public void setup() {
        super.setup();
        doCheckinPlatform();
        doCheckinLARVA();
        receiver = this.whoLarvaAgent();
        _exitRequested = false;
    }

    @Override
    public void plainExecute() {
        
        switch(estado){
            case LOGIN:
                login();
                break;
            case SENSORS:
                readSensors();
                break;
            case ACTION:
                doAction();
                break;
            case LOGOUT:
                logout();
                break;
        }
    }

    @Override
    public void takeDown() {
        this.doCheckoutLARVA();
        this.doCheckoutPlatform();
        super.takeDown();
    }
    
    private void login(){
        JsonObject  msg;
        msg = new JsonObject();
        msg.add("command","login");
        msg.add("world","BasePlayground");
        
        // Añadir sensores
        JsonArray attach=new JsonArray();
        attach.add("gps");
        attach.add("distance");
        attach.add("visual");
        attach.add("thermal");
        msg.add("attach",attach);
        
        // Mandar mensaje
        out.setSender(getAID());
        out.addReceiver(new AID(receiver,AID.ISLOCALNAME));
        out.setContent(msg.toString());
        this.sendServer(out);
        
        // Parsear respuesta
        in = this.blockingReceive();
        String answer = in.getContent();
        Info("MyWorldManager dice: " + answer);
        JsonObject respuesta = Json.parse(answer).asObject();
        key = respuesta.getString("key", "");
        
        if (respuesta.get("result").asString().equals("ok")) {
            estado = Estado.SENSORS;
        }
        else
            _exitRequested = true;
        
    }
    
    private void readSensors(){
        JsonObject msg = new JsonObject();
        msg.add("command","read");
        msg.add("key", key);
        
        out = in.createReply();
        out.setContent(msg.toString());
        this.sendServer(out);
        
        in = this.blockingReceive();
        String answer = in.getContent();
        Info("MyWorldManager dice: " + answer);
        
        JsonObject respuesta = Json.parse(answer).asObject();
        JsonArray perceptions = respuesta.get("details").asObject().get("perceptions").asArray();
        
        for (JsonValue perception : perceptions) {
            switch(perception.asObject().get("sensor").asString()){
                case "gps":
                    JsonArray data = perception.asObject().get("data").asArray().get(0).asArray();
                    x = data.get(0).asInt();
                    y = data.get(1).asInt();
                    z = data.get(2).asInt();
                    break;
                case "visual":
                    break;
                case "distance":
                    break;
            }
        }

        if (respuesta.get("result").asString().equals("ok")) {
            estado = Estado.ACTION;
        }
        else
            _exitRequested = true;
    }
    
    private void doAction(){
        JsonObject msg = new JsonObject();
        msg.add("command","execute");
        msg.add("key", key);
        msg.add("action", "rotateL");
        
        out = in.createReply();
        out.setContent(msg.toString());
        this.sendServer(out);
        
        in = this.blockingReceive();
        String answer = in.getContent();
        Info("MyWorldManager dice: " + answer);
        
        JsonObject respuesta = Json.parse(answer).asObject();
        
        if (respuesta.get("result").asString().equals("ok")) {
            estado = Estado.LOGOUT;
        }
        else
            _exitRequested = true;
    }
    
    private void logout(){
        JsonObject msg = new JsonObject();
        msg.add("command","logout");
        msg.add("key", key);
        
        out = in.createReply();
        out.setContent(msg.toString());
        this.sendServer(out);
        
        _exitRequested = true;
    }
    
    /**
     * A* Algorithm to find optimal path from two points in the map.
     * @param initial Initial Node to start from.
     * @param target Wanted final location.
     * @return 	List of ACTIONS to reach target.
     */
    private LinkedList<ACTIONS> findPath(Node initial, Vector2d target, Boolean simulate_monster)
    {
        ArrayList<ACTIONS> available_actions = new ArrayList<>(Arrays.asList(ACTIONS.ACTION_UP, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT));
        TreeMap<Node, Node> open = new TreeMap<>();
        HashSet<Node> closed = new HashSet<>();
        Node current;

        open.put(initial, initial);

        while (!open.isEmpty()) {
            current = open.pollFirstEntry().getKey();
            closed.add(current);
            // Si el nodo actual es solución lo devolvemos
            if (current.getSt().getPosition().equals(target)) {
                return current.getPath();
            }
            // Generamos hijos
            for (ACTIONS action : available_actions) {
                State new_state = current.getSt().simulateAction(action, walls, simulate_monster);

                if (new_state != null) {
                    int cost = current.getG() + 1;

                    Node children = new Node(new_state, cost, cost + manhattanDist(new_state.getPosition(), target), current, action);

                    if (!closed.contains(children)) {
                        open.put(children, children);
                    }
                }
            }
        }
        return null;
    }
}
