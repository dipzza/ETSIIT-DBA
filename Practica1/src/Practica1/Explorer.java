/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Practica1;

import AppBoot.ConsoleBoot;

public class Explorer {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("DRON", args);
        app.selectConnection();
        
        app.launchAgent("Bryan", MyWorldExplorer.class);
        app.shutDown();        
    }
    
}
