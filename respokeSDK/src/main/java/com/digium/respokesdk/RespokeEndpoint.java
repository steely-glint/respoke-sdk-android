/**
 * Copyright 2015, Digium, Inc.
 * All rights reserved.
 *
 * This source code is licensed under The MIT License found in the
 * LICENSE file in the root directory of this source tree.
 *
 * For all details and documentation:  https://www.respoke.io
 */

package com.digium.respokesdk;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 *  Represents remote Endpoints. Endpoints are users of this application that are not the one logged into this
 *  instance of the application. An Endpoint could be logged in from multiple other instances of this app, each of
 *  which is represented by a Connection. The client can interact with endpoints by calling them or
 *  sending them messages. An endpoint can be a person using an app from a browser or a script using the APIs on
 *  a server.
 */
public class RespokeEndpoint {

    private WeakReference<Listener> listenerReference;
    private String endpointID;
    public ArrayList<RespokeConnection> connections;
    private RespokeSignalingChannel signalingChannel;
    public Object presence;
    private WeakReference<RespokeDirectConnection> directConnectionReference;
    private WeakReference<RespokeClient> clientReference;


    /**
     *  A listener interface to notify the receiver of events occurring with the endpoint
     */
    public interface Listener {

        /**
         *  Handle messages sent to the logged-in user from this one Endpoint.
         *
         *  @param message    The message
         *  @param timestamp  The timestamp of the message
         *  @param sender     The remote endpoint that sent the message
         */
        public void onMessage(String message, Date timestamp, RespokeEndpoint sender);


        /**
         *  A notification that the presence for an endpoint has changed
         *
         *  @param presence The new presence
         *  @param sender   The endpoint
         */
        public void onPresence(Object presence, RespokeEndpoint sender);

    }


    public RespokeEndpoint(RespokeSignalingChannel channel, String newEndpointID, RespokeClient client) {
        endpointID = newEndpointID;
        signalingChannel = channel;
        connections = new ArrayList<RespokeConnection>();
        clientReference = new WeakReference<RespokeClient>(client);
    }


    public void setListener(Listener listener) {
        listenerReference = new WeakReference<Listener>(listener);
    }
    void queueRunnable(Runnable r){
        r.run();
    }

    public void sendMessage(String message, boolean push, final Respoke.TaskCompletionListener completionListener) {
        if ((null != signalingChannel) && (signalingChannel.connected)) {
            try {
                JSONObject data = new JSONObject();
                data.put("to", endpointID);
                data.put("message", message);
                data.put("push", push);

                signalingChannel.sendRESTMessage("post", "/v1/messages", data, new RespokeSignalingChannel.RESTListener() {
                    @Override
                    public void onSuccess(Object response) {
                        Respoke.postTaskSuccess(completionListener);
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        Respoke.postTaskError(completionListener, errorMessage);
                    }
                });
            } catch (JSONException e) {
                Respoke.postTaskError(completionListener, "Error encoding message");
            }
        } else {
            Respoke.postTaskError(completionListener, "Can't complete request when not connected. Please reconnect!");
        }
    }


    public RespokeCall startCall(RespokeCall.Listener callListener,  Object glView, boolean audioOnly) {
        RespokeCall call = null;

        if ((null != signalingChannel) && (signalingChannel.connected)) {
            call = new RespokeCall(signalingChannel, this, false);
            call.setListener(callListener);
            call.startCall( glView, audioOnly);
        }

        return call;
    }


    public String getEndpointID() {
        return endpointID;
    }


    public RespokeConnection getConnection(String connectionID, boolean skipCreate) {
        RespokeConnection connection = null;

        for (RespokeConnection eachConnection : connections) {
            if (eachConnection.connectionID.equals(connectionID)) {
                connection = eachConnection;
                break;
            }
        }

        if ((null == connection) && !skipCreate) {
            connection = new RespokeConnection(signalingChannel, connectionID, this);
            connections.add(connection);
        }

        return connection;
    }


    public ArrayList<RespokeConnection> getConnections() {
        return connections;
    }


    public void didReceiveMessage(final String message, final Date timestamp) {
        queueRunnable(new Runnable() {
            @Override
            public void run() {
                if (null != listenerReference) {
                    Listener listener = listenerReference.get();
                    if (null != listener) {
                        listener.onMessage(message, timestamp, RespokeEndpoint.this);
                    }
                }
            }
        });
    }


    public void resolvePresence() {
        ArrayList<Object> list = new ArrayList<Object>();

        for (RespokeConnection eachConnection : connections) {
            Object connectionPresence = eachConnection.presence;

            if (null != connectionPresence) {
                list.add(connectionPresence);
            }
        }

        RespokeClient client = null;
        RespokeClient.ResolvePresenceListener resolveListener = null;
        if (null != clientReference) {
            client = clientReference.get();

            if (client != null) {
                resolveListener = client.getResolvePresenceListener();
            }
        }

        if (null != resolveListener) {
            presence = resolveListener.resolvePresence(list);
        } else {
            ArrayList<String> options = new ArrayList<String>();
            options.add("chat");
            options.add("available");
            options.add("away");
            options.add("dnd");
            options.add("xa");
            options.add("unavailable");

            String newPresence = null;
            for (String eachOption : options) {
                for (Object eachObject : list) {
                    if (eachObject instanceof String) {
                        String eachObjectString = (String) eachObject;

                        if (eachObjectString.toLowerCase().equals(eachOption)) {
                            newPresence = eachOption;
                        }
                    }
                }

                if (null != newPresence) {
                    break;
                }
            }

            if (null == newPresence) {
                newPresence = "unavailable";
            }

            presence = newPresence;
        }

        queueRunnable(new Runnable() {
            @Override
            public void run() {
                if (null != listenerReference) {
                    Listener listener = listenerReference.get();
                    if (null != listener) {
                        listener.onPresence(presence, RespokeEndpoint.this);
                    }
                }
            }
        });
    }


    public RespokeDirectConnection directConnection() {
        if (null != directConnectionReference) {
            return directConnectionReference.get();
        } else {
            return null;
        }
    }


    public void setDirectConnection(RespokeDirectConnection newDirectConnection) {
        if (null != newDirectConnection) {
            directConnectionReference = new WeakReference<RespokeDirectConnection>(newDirectConnection);
        } else {
            directConnectionReference = null;
        }
    }


    public RespokeDirectConnection startDirectConnection() {
        // The constructor will call the setDirectConnection method on this endpoint instance with a reference to the new RespokeDirectConnection object
        RespokeCall call = new RespokeCall(signalingChannel, this, true);
        call.startCall(null, null, false);

        return directConnection();
    }
}
