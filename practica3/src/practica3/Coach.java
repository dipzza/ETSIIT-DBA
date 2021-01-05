package practica3;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import static ACLMessageTools.ACLMessageTools.getJsonContentACLM;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import Map2D.Map2DGrayscale;
import YellowPages.YellowPages;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**e
 *
 * @author inditex
 */
public class Coach extends AgentInterface {
    LinkedList<String> myCoins = new LinkedList<String>();
    LinkedList<String> myDistSensors = new LinkedList<String>();
    LinkedList<String> myAngSensors = new LinkedList<String>();
    LinkedList<String> myChargeSensors = new LinkedList<String>();
    int numSensBuyed = 4;
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
                in = queryYellowPages(_identitymanager); // As seen on slides
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
                this.sendConvID("SeñorVisor");
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
                            cheapestDist.get(price).add(shop + " " + referencia);
                        }
                        else if (tipoSensor.equals(angSensor)) {
                            int price = sensor.asObject().get("price").asInt();
                            if (!cheapestAng.containsKey(price))
                                cheapestAng.put(price, new ArrayList<>());
                            cheapestAng.get(price).add(shop + " " + referencia);
                        }
                        else if (tipoSensor.equals("CHARGE")) {
                            int price = sensor.asObject().get("price").asInt();
                            if (!cheapestCharge.containsKey(price))
                                cheapestCharge.put(price, new ArrayList<>());
                            cheapestCharge.get(price).add(shop + " " + referencia);
                        }
                    }
                    
                    Info("Products of shop " + shop + " OK");
                }

                myDistSensors = buySensors(cheapestDist, numSensBuyed);
                myAngSensors = buySensors(cheapestAng, numSensBuyed);
                myChargeSensors = buySensors(cheapestCharge, 50);

                Info("Sensor bought");

                sendSensors("SeñorBusca1", new JsonArray().add(myDistSensors.pollFirst()).add(myAngSensors.pollFirst()).add(myChargeSensors.pollFirst()));
                sendSensors("SeñorBusca2", new JsonArray().add(myDistSensors.pollFirst()).add(myAngSensors.pollFirst()).add(myChargeSensors.pollFirst()));
                sendSensors("SeñorBusca3", new JsonArray().add(myDistSensors.pollFirst()).add(myAngSensors.pollFirst()).add(myChargeSensors.pollFirst()));
                sendSensors("SeñorBusca4", new JsonArray().add(myDistSensors.pollFirst()).add(myAngSensors.pollFirst()).add(myChargeSensors.pollFirst()));

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

    // Devuelve una matriz con el siguiente formato: [Shop, Reference, Price], ...
    private ArrayList<ArrayList<Object>> getProductFromMap(TreeMap<Integer, ArrayList<String>> cheapest_products, int quantity) {
        ArrayList<ArrayList<Object>> products = new ArrayList<>();
        int i = 0;

        while(i < quantity && !cheapest_products.isEmpty()) {
            for (String refshop : cheapest_products.firstEntry().getValue()) {
                ArrayList<Object> product = new ArrayList<>();

                product.add(refshop.split(" ")[0]);
                product.add(refshop.split(" ")[1]);
                product.add(cheapest_products.firstEntry().getKey());

                products.add(product);

                i++;
                if (i == quantity)
                    break;
            }

            cheapest_products.pollFirstEntry();
        }

        return products;
    }

    private LinkedList<String> buySensors(TreeMap<Integer, ArrayList<String>> cheapest_products, int quantity) {
        LinkedList<String> boughtSensors = new LinkedList<>();

        for (ArrayList<Object> product : getProductFromMap(cheapest_products, quantity))
        {
            int price = (int) product.get(2);
            if (myCoins.size() < price)
                break;

            ArrayList<String> coins = new ArrayList<>();

            for (int i = 0; i < price; i++)
                coins.add(myCoins.pollFirst());

            sendBuyProduct((String) product.get(0), (String) product.get(1), coins);
            in = blockingReceive();
            myError = in.getPerformative() != ACLMessage.INFORM;
            if (myError) {
                Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                        + " Buy operation failed due to " + getDetailsLARVA(in));
                myStatus = "CHECKOUT-SESSION";
                break;
            }

            boughtSensors.add(Json.parse(in.getContent()).asObject().get("reference").asString());
        }

        return boughtSensors;
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

    protected void sendBuyProduct(String shop, String reference, ArrayList<String> coins) {
        JsonObject content = new JsonObject();
        JsonArray payment = new JsonArray();

        
        content.add("operation", "buy");
        content.add("reference", reference);
        for (String coin : coins) {
            payment.add(coin);
        }
        content.add("payment", payment);

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(shop, AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.REQUEST);
        out.setConversationId(myConvID);
        send(out);
    }

    private void sendSensors(String receiver, JsonArray sensors) {
        JsonObject content = new JsonObject();
        content.add("sensors", sensors);
        content.add("width", myMap.getWidth());
        content.add("height", myMap.getWidth());

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        out.setContent(content.toString());
        out.setProtocol("REGULAR");
        out.setPerformative(ACLMessage.INFORM);
        out.setConversationId(myConvID);
        send(out);
    }
}