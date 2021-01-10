package practica3;


import IntegratedAgent.IntegratedAgent;
import Map2D.Map2DGrayscale;
import YellowPages.YellowPages;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

 /**
  * Clase que hace de interfaz del Coach y del rescuer.
  * Extendida de la clase IntegratedAgent
  *
  * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
  * @version Practica 3 (1.0)
  */

public class AgentInterface extends IntegratedAgent{

    protected YellowPages myYP;
    protected String myStatus, myService, myWorldManager, myWorld, sessionConvID;
    protected boolean myError;
    protected ACLMessage in, out;
    protected Map2DGrayscale myMap;

     /**
    * Método encargado de iniciar la comunicación con el servidor.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    */

    @Override
    public void setup()   {
        // Hardcoded the only known agent: Sphinx
        _identitymanager = "Sphinx";
        super.setup();

        Info("Booting");

        // Description of my group
        myService = "Analytics group Inditex";

        // The world I am going to open
        myWorld = "World2";

        // First state of the agent
        myStatus = "CHECKIN-LARVA";

        // To detect possible errors
        myError = false;

        _exitRequested = false;
    }

     /**
    * Método encargado de desconectar del servicio de Larva
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 2 (1.0)
    */
    
    @Override
    public void takeDown() {
        Info("Taking down");
        super.takeDown();
    }

    /**
    * Método encargado de enviar la respuesta del agente al servidor
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param perf Performativa del mensaje
    * @param protocol Protocolo del mensaje
    * @param cont contenido del mensaje
    */   

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

    /**
    * Método encargado de enviar un mensaje a otro agente.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param reciever Destinatario del mensaje.
    * @param perf Performativa del mensaje
    * @param protocol Protocolo del mensaje
    * @param cont contenido del mensaje
    */   

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

    /**
    * Método encargado de enviar una peticion de login a LARVA
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param im Destinatario del mensaje
    * @return Mensaje que la petición para iniciar sesión en LARVA 
    */   
    
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

    /**
    * Método encargado de enviar una peticion para desconectarse de LARVA
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param im Destinatario del mensaje
    * @return Mensaje que contiene la peticion para desloguearse de LARVA.
    */   

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

    /**
    * Método encargado de enviar una petición para subscribirse a un mapa.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param problem Mapa al que loguearte
    * @return Mensaje que contiene a qué mapa va a loguerase. 
    */   

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

    /**
    * Método encargado de enviar una petición para loguearse de un agente y su tipo.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param agentType Tipo de agente que se va a subscribir
    * @return mensaje que contiene el tipo de agente que se va a subscribir a la sesion
    */   

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

    /**
    * Método encargado de enviar para cerrar la sesion con el World Manager
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param convID ID de la conversación.
    * @return Mensaje que contiene la petición para cerrar sesion con el WM.
    */ 
    
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

    /**
    * Método encargado de pedir las paginas amarillas.
    * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
    * @version Practica 3 (1.0)
    * @param im Destinatario del mensaje
    * @return Mensaje que contiene las paginas amarillas.
    */ 

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
