package practica3;


import IntegratedAgent.IntegratedAgent;
import Map2D.Map2DGrayscale;
import YellowPages.YellowPages;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**e
 *
 * @author inditex
 */

public class AgentInterface extends IntegratedAgent{

    protected YellowPages myYP;
    protected String myStatus, myService, myWorldManager, myWorld, myConvID, sessionConvID;
    protected boolean myError;
    protected ACLMessage in, out;
    protected Map2DGrayscale myMap;

    @Override
    public void setup()   {
        // Hardcoded the only known agent: Sphinx
        _identitymanager = "Sphinx";
        super.setup();

        Info("Booting");

        // Description of my group
        myService = "Analytics group Inditex";

        // The world I am going to open
        myWorld = "Playground1";

        // First state of the agent
        myStatus = "CHECKIN-LARVA";

        // To detect possible errors
        myError = false;

        _exitRequested = false;
    }
    
    @Override
    public void takeDown() {
        Info("Taking down");
        super.takeDown();
    }

    protected void sendReply(int perf, String protocol, String cont) {
        out = in.createReply();
        out.setPerformative(perf);
        out.setContent(cont);
        out.setProtocol(protocol);

        if (perf == ACLMessage.SUBSCRIBE) {
            out.setEncoding(_myCardID.getCardID());
        }

        this.send(out);
    }

    protected void sendMsg(String receiver, int perf, String protocol, String cont) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(receiver, AID.ISLOCALNAME));

        out.setPerformative(perf);
        out.setContent(cont);
        out.setProtocol(protocol);

        if (perf == ACLMessage.SUBSCRIBE) {
            out.setEncoding(_myCardID.getCardID());
        }

        this.send(out);
    }

    protected ACLMessage sendCheckinLARVA(String im) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setContent("");
        out.setProtocol("ANALYTICS");
        out.setEncoding(_myCardID.getCardID());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        send(out);
        return blockingReceive();
    }

    protected ACLMessage sendCheckoutLARVA(String im) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setContent("");
        out.setProtocol("ANALYTICS");
        out.setPerformative(ACLMessage.CANCEL);
        send(out);
        return blockingReceive();
    }

    protected ACLMessage sendSubscribeWM(String problem) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setProtocol("ANALYTICS");
        out.setContent(new JsonObject().add("problem", problem).toString());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        this.send(out);
        return this.blockingReceive();
    }

    protected ACLMessage sendSubscribeSession(String agentType) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setProtocol("REGULAR");
        out.setContent(new JsonObject().add("type", agentType).toString());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        out.setConversationId(sessionConvID);
        this.send(out);
        return this.blockingReceive();
    }

    protected ACLMessage sendCANCELWM(String convID) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setContent("");
        out.setConversationId(convID);
        out.setProtocol("ANALYTICS");
        out.setPerformative(ACLMessage.CANCEL);
        send(out);
        return blockingReceive();
    }

    protected ACLMessage queryYellowPages(String im) {
        YellowPages res = null;

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setProtocol("ANALYTICS");
        out.setContent("");
        out.setPerformative(ACLMessage.QUERY_REF);
        this.send(out);
        return blockingReceive();
    }
}
