package practica3;

import AppBoot.ConsoleBoot;

 /**
  * Clase que sirve de main encargada de realizar el boot del dron.
  * Se seleciona la conexión y se lanza el agente con un identificador y usando la clase MyWorldExplorer.
  *
  * @author Javier, Jose Miguel, Alvaro y Bryan Alfonso.
  * @version Practica 2 (1.0)
  */

public class Explorer {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("DRON", args);
        app.selectConnection();
        
        app.launchAgent("SeñorVisor", awacs.Awacs.class);
        app.launchAgent("ElPepe", Coach.class);
        app.launchAgent("SeñorEscucha", Listener.class);
        app.launchAgent("SeñorBusca1", Rescuer.class);
        app.launchAgent("SeñorBusca2", Rescuer.class);
        app.launchAgent("SeñorBusca3", Rescuer.class);
        app.launchAgent("SeñorBusca4", Rescuer.class);

        app.shutDown();
    }
    
}
