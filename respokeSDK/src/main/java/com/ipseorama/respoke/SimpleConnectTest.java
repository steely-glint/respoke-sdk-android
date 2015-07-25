/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.respoke;

import com.digium.respokesdk.Respoke;
import com.digium.respokesdk.RespokeCall;
import com.digium.respokesdk.RespokeClient;
import com.digium.respokesdk.RespokeDirectConnection;
import com.digium.respokesdk.RespokeEndpoint;
import com.digium.respokesdk.RespokeGroup;
import com.github.nkzawa.socketio.client.Socket;
import com.phono.srtplight.Log;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author Westhawk Ltd thp@westhawk.co.uk 
 */
public class SimpleConnectTest implements RespokeClient.Listener {

    public RespokeClient client;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Log.setLevel(Log.VERB);
        LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.ALL); 
        Handler h = new Handler(){

            @Override
            public void publish(LogRecord record) {
                Log.debug(record.toString());
            }

            @Override
            public void flush() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void close() throws SecurityException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
        };
        
        LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).addHandler(h);
        SimpleConnectTest me = new SimpleConnectTest();
    }

    public SimpleConnectTest() {
        // Create an instance of the Respoke client
        client = Respoke.sharedInstance().createClient();
        client.setListener(this);

        // App ID from the Respoke Dashboard for your App
        String appId = "fb4db1ba-d419-4d0c-a9f0-cce399942b49";

        // The unique username identifying the user
        String endpointId = "spock@enterprise.com";

        // Execute some signin event, then connect to Respoke with
        client.connect(endpointId, appId, true, null, new RespokeClient.ConnectCompletionListener() {
            @Override
            public void onError(String errorMessage) {
                Log.debug("main" + errorMessage);
            }
        });
    }

    // RespokeClient Listeners
    // "connect" event fired after successful connection to Respoke
    public void onConnect(RespokeClient client) {
        Log.debug("main:" + "Connected to Respoke!");
    }

    @Override
    public void onDisconnect(RespokeClient sender, boolean reconnecting) {
        Log.debug("main:" + "disconnected, reconnecting =  " + reconnecting);
    }

    @Override
    public void onError(RespokeClient sender, String errorMessage) {
        Log.debug("main:" + "error " + errorMessage);

    }

    @Override
    public void onCall(RespokeClient sender, RespokeCall call) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onIncomingDirectConnection(RespokeDirectConnection directConnection, RespokeEndpoint endpoint) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onMessage(String message, RespokeEndpoint sender, RespokeGroup group, Date timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
