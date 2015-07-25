/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.digium.respokesdk;

import com.ipseorama.webrtc.PeerConnection;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Westhawk Ltd thp@westhawk.co.uk
 */
public class RespokeCall {

    void directConnectionDidAccept() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    PeerConnection getPeerConnection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void directConnectionDidClose(RespokeDirectConnection aThis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void directConnectionDidOpen(RespokeDirectConnection aThis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void hangupReceived() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void answerReceived(JSONObject sdp, String fromConnection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void connectedReceived() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void iceCandidatesReceived(JSONArray candidates) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Object getSessionID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void startCall(Object glView, boolean audioOnly) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    /**
     *  A delegate protocol to notify the receiver of events occurring with the call
     */
    public interface Listener {


        /**
         *  Receive a notification that an error has occurred while on a call
         *
         *  @param errorMessage A human-readable description of the error.
         *  @param sender       The RespokeCall that experienced the error
         */
        public void onError(String errorMessage, RespokeCall sender);


        /**
         *  When on a call, receive notification the call has been hung up
         *
         *  @param sender The RespokeCall that has hung up
         */
        public void onHangup(RespokeCall sender);


        /**
         *  When on a call, receive remote media when it becomes available. This is what you will need to provide if you want
         *  to show the user the other party's video during a call.
         *
         *  @param sender The RespokeCall that has connected
         */
        public void onConnected(RespokeCall sender);


        /**
         *  This event is fired when the local end of the directConnection is available. It still will not be
         *  ready to send and receive messages until the 'open' event fires.
         *
         *  @param directConnection The direct connection object
         *  @param endpoint         The remote endpoint
         */
        public void directConnectionAvailable(RespokeDirectConnection directConnection, RespokeEndpoint endpoint);
    }
    RespokeCall(RespokeSignalingChannel signalingChannel, JSONObject sdp, String sessionID, String connectionID, RespokeEndpoint endpoint, boolean b, Date timestamp) {
    }

    RespokeCall(RespokeSignalingChannel signalingChannel, RespokeEndpoint aThis, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void startCall(Object object, Object object0, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setListener(RespokeCall.Listener callListener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
