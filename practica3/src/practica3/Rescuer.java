package practica3;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import YellowPages.YellowPages;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

public class Rescuer extends AgentInterface {
    int worldWidth, worldHeight;
    ACLMessage replyToWM;

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
                myStatus = "SUBSCRIBE-SESSION";
                Info("\tCheckin ok");
                break;
            case "SUBSCRIBE-SESSION":
                Info("R-etrieve who is my WM");
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

                // Wait for CONV-ID and map dimensions from COACH
                in = blockingReceive();
                myError = in.getPerformative() != ACLMessage.QUERY_IF;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Expected CONV-ID failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                this.sessionConvID = in.getConversationId();

                // Suscribe SESSION
                in = sendSubscribeSession("RESCUER");
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Suscribe SESSION failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                replyToWM = in.createReply();
                
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
                    worldWidth = answer.get("width").asInt();
                    worldHeight = answer.get("height").asInt();
                    

                    Info("Sensors and World Size received. Login to world");
                    int x = 0, y = 0;

                    switch(getLocalName()) {
                        case "SeñorBusca1": x = 0; y = 0; break;
                        case "SeñorBusca2": x = worldWidth; y = 0; break;
                        case "SeñorBusca3": x = 0; y = worldHeight; break;
                        case "SeñorBusca4": x = worldWidth; y = worldHeight; break;
                    }

                    in = sendLogin(sensors, x, y);

                    myStatus = "CHECKOUT-SESSION";

                } else if (in.getProtocol().equals("BROADCAST")) {
                    System.out.println("no ta hecho");
                } else {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " LogIn failure due to: " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-SESSION";
                    break;
                }
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
                sendMsg("SeñorEscucha", ACLMessage.INFORM, "REGULAR", "FinalSession");
                break;
            
            case "EXIT":
                Info("The agent dies");
                _exitRequested = true;
                break;
        }
    }

    // 106 (REQUEST
    // :sender  ( agent-identifier :name Hound#1@CAJAR  :addresses (sequence http://numenor:7778/acc ))
    // :receiver  (set ( agent-identifier :name WorldManager1@CAJAR ) )
    // :content  "{\"operation\":\"login\",\"attach\":[\"ALIVE#BRKTXKTFTIIU\",\"ONTARGET#BRPEFGOBBYLV\",\"GPS#BRTNNCIVJNOX\",\"COMPASS#CHQFDTRXFYYH\",\"DISTANCE#BTADRXXNMTLE\",\"ANGULAR#BSUTKCCSFEID\",\"ALTIMETER#BSMATKNCOZCB\",\"VISUAL#BSDHDSYLZSUZ\",\"LIDAR#BSHQLOSHHIZA\",\"THERMAL#BSQKCGHXXOFC\",\"ENERGY#BRYXUYDQRDRY\"],\"posx\":0,\"posy\":0}" 
    // :reply-with  Hound#1nxgy7NMM  :in-reply-to  Hound#1nxgy7NMM  :protocol  REGULAR
    // :conversation-id  SESSION#dmtai )
    private ACLMessage sendLogin(JsonArray sensors, int x, int y) {
        JsonObject content = new JsonObject();
        content.add("operation", "login");
        content.add("attach", sensors);
        content.add("posx", x);
        content.add("posy", y);

        out = replyToWM;
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