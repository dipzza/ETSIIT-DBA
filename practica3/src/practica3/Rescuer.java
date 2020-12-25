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

                // Wait for CONV-ID from COACH
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
                
                sendMsg("ElPepe", ACLMessage.INFORM, "REGULAR", in.getContent());

                myStatus = "LOGIN";
                break;
            
            case "LOGIN":
                in = blockingReceive();
                if (in.getProtocol().equals("REGULAR")) {
                    System.out.println("habría q pillar los sensores y logearse");
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
}