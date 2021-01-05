package practica3;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import static ACLMessageTools.ACLMessageTools.getJsonContentACLM;
import IntegratedAgent.IntegratedAgent;
import Map2D.Map2DGrayscale;
import YellowPages.YellowPages;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jdk.nashorn.api.tree.Tree;

import java.util.ArrayList;

import java.util.TreeMap;

/**e
 *
 * @author inditex
 */
public class Coach extends AgentInterface {
    ArrayList<String> myCoins = new ArrayList<>();
    int coinsLeft = 4;

    public void plainExecute() {
        plainWithErrors();
    }

    public void plainWithErrors() {
        // Basic iteration
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
                myStatus = "SUBSCRIBE-WM";
                Info("\tCheckin ok");
                break;
            case "SUBSCRIBE-WM":
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

                // Now it is time to start the game and turn on the lights within a given world
                in = sendSubscribeWM(myWorld);
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info(ACLMessage.getPerformative(in.getPerformative())
                            + " Could not open a session with "
                            + myWorldManager + " due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                // Keep the Conversation ID and spread it amongs the team members
                myConvID = in.getConversationId();
                Info("ConvID: " + myConvID.toString());
                this.sendConvID("SeñorEscucha");
                this.sendConvID("SeñorBusca1");
                this.sendConvID("SeñorBusca2");
                this.sendConvID("SeñorBusca3");
                this.sendConvID("SeñorBusca4");

                
                // Move on to get the map
                myStatus = "PROCESS-MAP";
                break;

            case "PROCESS-MAP":
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
                        myStatus = "GETCOINS";
                    } else {
                        Info("\t" + "There was an error processing and saving the image ");
                        myStatus = "CANCEL-WM";
                        break;
                    }
                } else {
                    Info("\t" + "There is no map found in the message");
                    myStatus = "CANCEL-WM";
                    break;
                }
            
            case "GETCOINS":
                in = blockingReceive();
                if (in.getProtocol().equals("REGULAR")) {
                    JsonArray coins = Json.parse(in.getContent()).asObject().get("coins").asArray();
                    for (JsonValue coin : coins) {
                        myCoins.add(coin.asString());
                    }
                    coinsLeft--;

                if (coinsLeft == 0) 
                    myStatus = "MARKETPLACE";

                } else if (in.getProtocol().equals("BROADCAST")) {
                    System.out.println("no ta hecho");
                } else {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Get coins failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-SESSION";
                    break;
                }
                break;
            
            case "MARKETPLACE":
                // First update Yellow Pages
                in = queryYellowPages(_identitymanager); // As seen oon slides
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Query YellowPages failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-SESSION";
                    break;
                }
                myYP = new YellowPages();
                myYP.updateYellowPages(in);
                // It might be the case that YP are right but we dont find an appropriate service for us, then leave
                if (myYP.queryProvidersofService("shop").isEmpty()) {
                    Info("\t" + "There is no agent providing the service " + myService);
                    myStatus = "CHECKOUT-SESSION";
                    break;
                }

                myYP.prettyPrint();

                TreeMap<Integer, ArrayList<String>> cheapestDist = new TreeMap<>();
                TreeMap<Integer, ArrayList<String>> cheapestAng = new TreeMap<>();
                TreeMap<Integer, ArrayList<String>> cheapestCharge = new TreeMap<>();
                String distSensor, angSensor;
                
                if (myMap.getWidth() > 200 || myMap.getHeight() > 200) {
                    distSensor = "DISTANCEHQ";
                    angSensor = "ANGULARHQ";
                } else {
                    distSensor = "DISTANCE";
                    angSensor = "ANGULAR";
                }

                // Obtener precio sensores
                // yp es el id de la tienda
                for(String shop : myYP.queryProvidersofService("shop@" + myConvID)){
                    //tenemos mandar un mensaje a la tienda para que nos diga el precio
                    sendShopQuery(shop);
                    in = blockingReceive();
                    
                    JsonArray products = Json.parse(in.getContent()).asObject().get("products").asArray();
                    for (JsonValue sensor : products) {
                        String referencia = sensor.asObject().get("reference").asString();
                        String tipoSensor = referencia.split("#")[0];
                        if (tipoSensor.equals(distSensor)) {
                            int price = sensor.asObject().get("price").asInt();
                            if (!cheapestDist.containsKey(price))
                                cheapestDist.put(price, new ArrayList<>());
                            cheapestDist.get(price).add(referencia + " " + shop);
                        }
                        else if (tipoSensor.equals(angSensor)) {
                            int price = sensor.asObject().get("price").asInt();
                            if (!cheapestAng.containsKey(price))
                                cheapestAng.put(price, new ArrayList<>());
                            cheapestAng.get(price).add(referencia + " " + shop);
                        }
                        else if (tipoSensor.equals("CHARGE")) {
                            int price = sensor.asObject().get("price").asInt();
                            if (!cheapestCharge.containsKey(price))
                                cheapestCharge.put(price, new ArrayList<>());
                            cheapestCharge.get(price).add(referencia + " " + shop);
                        }
                    }
                    
                    Info("Products of shop " + shop + " OK");
                }

                myStatus = "WAITTOFINISH";

                break;
            
            case "WAITTOFINISH":
                in = blockingReceive();
                if (in.getPerformative() == ACLMessage.INFORM) {
                    if (in.getContent().equals("FinalSession")){
                        myStatus = "CANCEL-WM";
                    }
                }
                break;
            
            case "CANCEL-WM":
                Info("Closing the game");
                in = sendCANCELWM(myConvID);
                myStatus = "CHECKOUT-LARVA";
                break;
            case "CHECKOUT-LARVA":
                Info("Exit LARVA");
                in = sendCheckoutLARVA(_identitymanager);
                myStatus = "EXIT";
                break;
            case "EXIT":
                Info("The agent dies");
                _exitRequested = true;
                break;
        }
    }
    
    public void plainNoErrors() {
        // Basic iteration
        switch (myStatus.toUpperCase()) {
            case "CHECKIN-LARVA":
                Info("Checkin in LARVA with " + _identitymanager);
                in = sendCheckinLARVA(_identitymanager); // As seen in slides
                myStatus = "SUBSCRIBE-WM";
                break;
            case "SUBSCRIBE-WM":
                Info("Retrieve who is my WM");
                // First update Yellow Pages
                in = queryYellowPages(_identitymanager); // As seen oon slides
                myYP = new YellowPages();
                myYP.updateYellowPages(in);
                // Choose one of the available service providers, i.e., the first one
                myWorldManager = myYP.queryProvidersofService(myService).iterator().next();

                // Now it is time to start the game and turn on the lights within a given world
                in = sendSubscribeWM(myWorld);
                // Keep the Conversation ID and spread it amongs the team members
                myConvID = in.getConversationId();
                // Move on to get the map
                myStatus = "CANCEL-WM";
                break;

            case "CANCEL-WM":
                Info("Closing the game");
                in = sendCANCELWM(myConvID);
                myStatus = "CHECKOUT-LARVA";
                break;
            case "CHECKOUT-LARVA":
                Info("Exit LARVA");
                in = sendCheckoutLARVA(_identitymanager);
                myStatus = "EXIT";
                break;
            case "EXIT":
                Info("The agent dies");
                _exitRequested = true;
                break;
        }
    }

    protected void sendConvID (String im) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setContent("");
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.QUERY_IF);
        out.setConversationId(myConvID);
        send(out);
    }

    protected void sendShopQuery(String shop) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(shop, AID.ISLOCALNAME));
        out.setContent("{}");
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.QUERY_REF);
        out.setConversationId(myConvID);
        send(out);
    }
}