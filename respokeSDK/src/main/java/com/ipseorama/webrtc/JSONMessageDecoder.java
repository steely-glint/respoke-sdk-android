/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.webrtc;

import com.phono.srtplight.Log;
import java.io.StringWriter;

import org.json.*;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class JSONMessageDecoder implements Decoder.Text<JSONObject>, Encoder.Text<JSONObject> {

    @Override
    public JSONObject decode(String txt) throws DecodeException {
        JSONTokener toke = new JSONTokener(txt);
        JSONObject jo = new JSONObject(toke);
        return jo;
    }

    //Check if decode is possible. If not, return false
    @Override
    public boolean willDecode(String s) {
        Log.debug("Will decode asked for " + s);
        return true;
    }

    @Override
    public void init(EndpointConfig config) {
        Log.debug("init called on decoder");
    }

    @Override
    public void destroy() {
        Log.debug("destroy called on chatdecoder");
    }

    @Override
    public String encode(JSONObject object) {
        Log.debug("I have to encode " + object);
        String answer = object.toString();
        Log.debug("I encoded an object: " + answer);
        return answer;
    }
}
