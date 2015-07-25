/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.webrtc;

import com.digium.respokesdk.RespokeDirectConnection;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd thp@westhawk.co.uk 
 */
public class DataChannel {

    public State state() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean send(Buffer data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void registerObserver(RespokeDirectConnection aThis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void unregisterObserver() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public enum State {
        OPEN, CONNECTING, CLOSING, CLOSED
    }

    public static class Buffer {

        public boolean binary;
        public ByteBuffer data;

        public Buffer() {
        }

        public Buffer(ByteBuffer directData, boolean b) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public static class Init {

        public Init() {
        }
    }

}
