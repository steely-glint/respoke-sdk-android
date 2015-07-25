/**
 * Copyright 2015, Digium, Inc. All rights reserved.
 *
 * This source code is licensed under The MIT License found in the LICENSE file
 * in the root directory of this source tree.
 *
 * For all details and documentation: https://www.respoke.io
 */
package com.digium.respokesdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.digium.respokesdk.RestAPI.APITransaction;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.phono.srtplight.Log;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The purpose of this class is to make a method call for each API call to the
 * backend REST interface. This class takes care of App authentication,
 * websocket connection, Endpoint authentication, and all App interactions
 * thereafter.
 */
public class RespokeSignalingChannel {

    private static final String TAG = "RespokeSignalingChannel: ";
    private static final String RESPOKE_SOCKETIO_PORT = "443";
    private static final Integer REST_TASK_TIMEOUT = 30;

    public boolean connected;
    private WeakReference<Listener> listenerReference;
    private String appToken;
    private String connectionID;
    private String baseURL;
    private RespokeWorkerThread workerThread;
    private Socket client;
    private Emitter.Listener onJoin;
    private Emitter.Listener onLeave;
    private Emitter.Listener onMessage;
    private Emitter.Listener onSignal;
    private Emitter.Listener onPubsub;
    private Emitter.Listener onPresence;

    /**
     * A delegate protocol to notify the receiver of events occurring with the
     * connection status of the signaling channel
     */
    public interface Listener {

        /**
         * Receive a notification from the signaling channel that it has
         * connected to the cloud infrastructure
         *
         * @param sender The signaling channel that triggered the event
         * @param endpointID The endpointID for this connection, as reported by
         * the server
         */
        void onConnect(RespokeSignalingChannel sender, String endpointID, String connectionID);

        /**
         * Receive a notification from the signaling channel that it has
         * disconnected to the cloud infrastructure
         *
         * @param sender The signaling channel that triggered the event
         */
        void onDisconnect(RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that a remote
         * endpoint is attempting to start a call
         *
         * @param sdp The SDP data for the call
         * @param sessionID The session ID of the call
         * @param connectionID The connectionID that is calling
         * @param endpointID The endpointID that is calling
         * @param timestamp The timestamp when the call was initiated
         * @param sender The signaling channel that triggered the event
         */
        void onIncomingCall(JSONObject sdp, String sessionID, String connectionID, String endpointID, Date timestamp, RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that a remote
         * endpoint is attempting to start a direct connection
         *
         * @param sdp The SDP data for the directConnection
         * @param sessionID The session ID of the directConnection
         * @param connectionID The connectionID that is calling
         * @param endpointID The endpointID that is calling
         * @param timestamp The timestamp when the call was initiated
         * @param sender The signaling channel that triggered the event
         */
        void onIncomingDirectConnection(JSONObject sdp, String sessionID, String connectionID, String endpointID, Date timestamp, RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that an error has
         * occurred
         *
         * @param errorMessage Error message
         * @param sender The signaling channel that triggered the event
         */
        void onError(String errorMessage, RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that an endpoint
         * has joined this group.
         *
         * @param groupID The ID of the group triggering the join message
         * @param endpointID The ID of the endpoint that to which the connection
         * belongs
         * @param connectionID The ID of the connection that has joined the
         * group
         * @param sender The signaling channel that triggered the event
         */
        void onJoinGroup(String groupID, String endpointID, String connectionID, RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that an endpoint
         * has left this group.
         *
         * @param groupID The ID of the group triggering the leave message
         * @param endpointID The ID of the endpoint that to which the connection
         * belongs
         * @param connectionID The ID of the connection that has left the group
         * @param sender The signaling channel that triggered the event
         */
        void onLeaveGroup(String groupID, String endpointID, String connectionID, RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that a message has
         * been sent to this group
         *
         * @param message The body of the message
         * @param timestamp The timestamp of the message
         * @param endpointID The ID of the endpoint sending the message
         * @param sender The signaling channel that triggered the event
         */
        void onMessage(String message, Date timestamp, String endpointID, RespokeSignalingChannel sender);

        /**
         * Receive a notification that a group message was received
         *
         * @param message The body of the message
         * @param groupID The ID of the group to which the message was sent
         * @param endpointID The ID of the endpoint that sent the message
         * @param sender The signaling channel that triggered the event
         * @param timestamp The time at which the message was sent
         */
        void onGroupMessage(String message, String groupID, String endpointID, RespokeSignalingChannel sender, Date timestamp);

        /**
         * Receive a notification that a presence change message was received
         *
         * @param presence The new presence value
         * @param connectionID The connection ID whose presence changed
         * @param endpointID The endpoint ID to which the connection belongs
         * @param sender The signaling channel that triggered the event
         */
        void onPresence(Object presence, String connectionID, String endpointID, RespokeSignalingChannel sender);

        /**
         * Receive a notification from the signaling channel that a call has
         * been created
         *
         * @param call The RespokeCall instance that was created
         */
        void callCreated(RespokeCall call);

        /**
         * Receive a notification from the signaling channel that a call has
         * terminated
         *
         * @param call The RespokeCall instance that was terminated
         */
        void callTerminated(RespokeCall call);

        /**
         * Find a call with the specified session ID
         *
         * @param sessionID SessionID to find
         *
         * @return The RespokeCall instance with that sessionID. If not found,
         * will return nil.
         */
        RespokeCall callWithID(String sessionID);

        /**
         * This event is fired when the logged-in endpoint is receiving a
         * request to open a direct connection to another endpoint. If the user
         * wishes to allow the direct connection, calling 'accept' on the direct
         * connection will allow the connection to be set up.
         *
         * @param directConnection The direct connection object
         * @param endpoint The remote endpoint
         */
        void directConnectionAvailable(RespokeDirectConnection directConnection, RespokeEndpoint endpoint);
    }

    /**
     * A listener interface to receive a notification that the REST message
     * transmission has completed
     */
    public interface RESTListener {

        void onSuccess(Object response);

        void onError(String errorMessage);

    }

    /**
     * A listener interface to receive a notification that this client has been
     * registered to receive presence updates for a specific endpoint
     */
    public interface RegisterPresenceListener {

        void onSuccess(JSONArray initialPresenceData);

        void onError(String errorMessage);

    }

    public RespokeSignalingChannel(String token, Listener newListener, String baseURL) {
        appToken = token;
        this.baseURL = baseURL;
        listenerReference = new WeakReference<Listener>(newListener);
        workerThread = new RespokeWorkerThread("RespokeWorkerThread");
        workerThread.start();
        workerThread.prepareHandler();

    }

    public Listener GetListener() {
        return listenerReference.get();
    }

    public void authenticate() {
        String connectURL = "https://api.respoke.io/socket.io/1/";

        Log.debug("Connecting socket "+connectURL);
        Emitter.Listener onError = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                    Log.debug(TAG + "Socket error" + args.toString());
            }
        };
        Socket socket;
        try {
            IO.Options opts = new IO.Options();
            opts.query = "__sails_io_sdk_version=0.10.0&app-token=" + appToken;

            socket = IO.socket(connectURL, opts);
            setHooks();

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.debug(TAG + "Socket connected to respoke");
                    connected = true;
                    client = socket;
                    onConnect();
                }

            }).on("join", onJoin
            ).on("leave", onLeave
            ).on("message", onMessage
            ).on("signal", onSignal
            ).on("pubsub", onPubsub
            ).on("presence", onPresence
            ).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.debug(TAG + "Socket disconnected");
                    socket.disconnect();
                    client = null;
                    if (connected) {
                        connected = false;
                        workerThread.cancelAllTasks();

                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onDisconnect(RespokeSignalingChannel.this);
                        }
                    }
                }

            }).on(Socket.EVENT_ERROR, onError).on(Socket.EVENT_CONNECT_ERROR, onError).on(Socket.EVENT_CONNECT_TIMEOUT, onError);
            socket.connect();
        } catch (URISyntaxException ex) {
            Log.error("error in url " + ex.getMessage() + " " + connectURL);
        }
    }

    /*
     public void onError(SocketIOException socketIOException) {
     Log.debug(TAG + "Socket error: " + socketIOException.getMessage());
            
     Listener listener = listenerReference.get();
     if (null != listener) {
     listener.onError(socketIOException.getMessage(), RespokeSignalingChannel.this);
     }
     }
     */
    public void setHooks() {
        Log.debug("Setting Hooks");

        onJoin = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (int ii = 0; ii < args.length; ii++) {
                    try {
                        JSONObject eachEvent = (JSONObject) args[ii];
                        String endpoint = eachEvent.getString("endpointId");
                        String connection = eachEvent.getString("connectionId");
                        JSONObject header = eachEvent.getJSONObject("header");
                        String groupID = header.getString("channel");

                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onJoinGroup(groupID, endpoint, connection, RespokeSignalingChannel.this);
                        }
                    } catch (JSONException e) {
                        Log.debug(TAG + "Error parsing received event");
                    }
                }
            }
        };
        onLeave = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (int ii = 0; ii < args.length; ii++) {
                    try {
                        JSONObject eachEvent = (JSONObject) args[ii];
                        String endpoint = eachEvent.getString("endpointId");
                        String connection = eachEvent.getString("connectionId");
                        JSONObject header = eachEvent.getJSONObject("header");
                        String groupID = header.getString("channel");

                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onLeaveGroup(groupID, endpoint, connection, RespokeSignalingChannel.this);
                        }
                    } catch (JSONException e) {
                        Log.debug(TAG + "Error parsing received event");
                    }
                }
            }
        };
        onMessage = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (int ii = 0; ii < args.length; ii++) {
                    try {
                        JSONObject eachEvent = (JSONObject) args[ii];
                        String message = eachEvent.getString("body");
                        JSONObject header = eachEvent.getJSONObject("header");
                        String endpoint = header.getString("from");
                        Date messageDate;

                        if (!header.isNull("timestamp")) {
                            messageDate = new Date(header.getLong("timestamp"));
                        } else {
                            // Just use the current time if no date is specified in the header data
                            messageDate = new Date();
                        }

                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onMessage(message, messageDate, endpoint, RespokeSignalingChannel.this);
                        }
                    } catch (JSONException e) {
                        Log.debug(TAG + "Error parsing received event");
                    }
                }
            }
        };
        onSignal = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (int ii = 0; ii < args.length; ii++) {
                    try {
                        JSONObject eachEvent = (JSONObject) args[ii];
                        routeSignal(eachEvent);
                    } catch (JSONException e) {
                        Log.debug(TAG + "Error parsing received event");
                    }
                }
            }
        };
        onPubsub = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (int ii = 0; ii < args.length; ii++) {
                    try {
                        JSONObject eachEvent = (JSONObject) args[ii];
                        String message = eachEvent.getString("message");
                        JSONObject header = eachEvent.getJSONObject("header");
                        String endpointID = header.getString("from");
                        String groupID = header.getString("channel");
                        Date messageDate;

                        if (!header.isNull("timestamp")) {
                            messageDate = new Date(header.getLong("timestamp"));
                        } else {
                            // Just use the current time if no date is specified in the header data
                            messageDate = new Date();
                        }

                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onGroupMessage(message, groupID, endpointID, RespokeSignalingChannel.this, messageDate);
                        }
                    } catch (JSONException e) {
                        Log.debug(TAG + "Error parsing received event");
                    }
                }
            }
        };
        onPresence = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (int ii = 0; ii < args.length; ii++) {
                    try {
                        JSONObject eachEvent = (JSONObject) args[ii];
                        Object type = eachEvent.getString("type");
                        JSONObject header = eachEvent.getJSONObject("header");
                        String endpointID = header.getString("from");
                        String connectionID = header.getString("fromConnection");

                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onPresence(type, connectionID, endpointID, RespokeSignalingChannel.this);
                        }
                    } catch (JSONException e) {
                        Log.debug(TAG + "Error parsing received event");
                    }
                }
            }
        };

    }

    void onConnect() {
        /*
         final SharedPreferences prefs = appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE);
                
         final String lastKnownPushTokenID = prefs.getString(RespokeClient.PROPERTY_LAST_VALID_PUSH_TOKEN_ID, "notAvailable");
         */
        JSONObject data = new JSONObject();
        String lastKnownPushTokenID = null;

        if ((null != lastKnownPushTokenID) && (!lastKnownPushTokenID.equals(
                "notAvailable"))) {
            try {
                data.put("pushTokenId", lastKnownPushTokenID);
            } catch (JSONException e) {
                Log.debug("Invalid JSON format for token");
            }
        } else {
            data = null;
        }

        // Once the socket is connected, perform a post to get the connection and endpoint IDs for this client
        sendRESTMessage(
                "post", "/v1/connections", data, new RESTListener() {
                    @Override
                    public void onSuccess(Object response
                    ) {
                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            if (response instanceof JSONObject) {
                                try {
                                    JSONObject responseJSON = (JSONObject) response;
                                    String endpointID = responseJSON.getString("endpointId");
                                    connectionID = responseJSON.getString("id");

                                    listener.onConnect(RespokeSignalingChannel.this, endpointID, connectionID);
                                } catch (JSONException e) {
                                    listener.onError("Unexpected response from server", RespokeSignalingChannel.this);
                                }
                            } else {
                                listener.onError("Unexpected response from server", RespokeSignalingChannel.this);
                            }
                        }
                    }

                    @Override
                    public void onError(String errorMessage
                    ) {
                        /*
                         if ((null != lastKnownPushTokenID) && !lastKnownPushTokenID.equals("notAvailable")) {
                         SharedPreferences.Editor editor = prefs.edit();
                         editor.remove(RespokeClient.PROPERTY_LAST_VALID_PUSH_TOKEN);
                         editor.remove(RespokeClient.PROPERTY_LAST_VALID_PUSH_TOKEN_ID);
                         editor.commit();
                         }
                         */
                        Listener listener = listenerReference.get();
                        if (null != listener) {
                            listener.onError(errorMessage, RespokeSignalingChannel.this);
                        }
                    }

                });
    }

    public void disconnect() {
        workerThread.cancelAllTasks();

        if (null != client) {
            client.disconnect();
        }
    }

    public void registerPresence(ArrayList<String> endpointList, final RegisterPresenceListener completionListener) {
        if (connected) {
            JSONObject data = new JSONObject();

            try {
                data.put("endpointList", new JSONArray(endpointList));

                sendRESTMessage("post", "/v1/presenceobservers", data, new RESTListener() {
                    @Override
                    public void onSuccess(Object response) {
                        JSONArray responseArray = null;

                        if (response != null) {
                            if (response instanceof JSONArray) {
                                responseArray = (JSONArray) response;
                            } else if (response instanceof String) {
                                try {
                                    responseArray = new JSONArray((String) response);
                                } catch (JSONException e) {
                                    // An exception will trigger the onError handler
                                }
                            }
                        }

                        if (null == responseArray) {
                            completionListener.onError("Unexpected response from server");
                        } else {
                            completionListener.onSuccess(responseArray);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        completionListener.onError(errorMessage);
                    }
                });
            } catch (JSONException e) {
                completionListener.onError("Unable to JSON encode message");
            }
        } else {
            completionListener.onError("Can't complete request when not connected. Please reconnect!");
        }
    }

    public void sendRESTMessage(String httpMethod, String url, JSONObject data, final RESTListener completionListener) {
        if (connected) {
            JSONArray array = new JSONArray();

            try {
                JSONObject message = new JSONObject("{'headers':{'App-Token':'" + appToken + "'},'url':'" + url + "'}");

                if (null != data) {
                    message.put("data", data);
                }

                array.put(message);

                if (array.toString().getBytes("UTF-8").length <= APITransaction.bodySizeLimit) {
                    sendEvent(httpMethod, array, 1, completionListener, 0);
                } else {
                    completionListener.onError("Request body is too big");
                }
            } catch (JSONException e) {
                completionListener.onError("Unable to JSON encode message");
            } catch (UnsupportedEncodingException e) {
                completionListener.onError("Unable to encode message");
            }
        } else {
            completionListener.onError("Can't complete request when not connected. Please reconnect!");
        }
    }

    private void sendEvent(final String httpMethod, final JSONArray array, final Integer attempt, final RESTListener completionListener, long delayMillis) {
        // Queue the request with the socket workerThread
        workerThread.postTaskDelayed(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    final CountDownLatch asyncTaskSignal = new CountDownLatch(1);
                    client.emit(httpMethod, array, new Ack() {

                        @Override
                        public void call(Object... os) {
                            JSONArray arguments = new JSONArray(os);
                            // There should only ever be one element in this array. Anything else is ignored for the time being.
                            if ((arguments != null) && (arguments.length() > 0)) {
                                try {
                                    Object responseObject = arguments.get(0);
                                    JSONObject jsonResponse = null;
                                    Object responseBody = null;
                                    String errorMessage = null;
                                    boolean rateLimitErrorPresent = false;
                                    Integer rateLimitDelay = 1000; // 1 second unless specified otherwise

                                    if (responseObject instanceof JSONObject) {
                                        jsonResponse = (JSONObject) responseObject;
                                    } else {
                                        errorMessage = "Unexpected response received";
                                    }

                                    // If the response contained json, parse it for error messages
                                    if (null != jsonResponse) {
                                        try {
                                            int statusCode = jsonResponse.getInt("statusCode");
                                            int[] validCodes = {200, 204, 205, 302, 401, 403, 404, 418, 429};
                                            if (Arrays.binarySearch(validCodes, statusCode) < 0) {
                                                errorMessage = "An unknown error occurred";
                                            } else if (429 == statusCode) {
                                                // The request was rejected due to a rate limit error
                                                rateLimitErrorPresent = true;

                                                // If there was a rate limit error, extract the limit info from the headers
                                                try {
                                                    JSONObject headers = jsonResponse.getJSONObject("headers");
                                                    Integer limit = headers.getInt("RateLimit-Limit");
                                                    rateLimitDelay = 1000 / limit;
                                                } catch (JSONException e) {
                                                    // If the limit info could not be found, use the default
                                                }
                                            }
                                        } catch (JSONException e) {
                                            // If the limit info could not be found, use the default
                                        }

                                        responseBody = jsonResponse.get("body");

                                        if (responseBody instanceof String) {
                                            String responseString = (String) responseBody;

                                            if (responseString.equals("null")) {
                                                responseBody = null;
                                            } else {
                                                try {
                                                    responseBody = new JSONObject(responseString);
                                                } catch (JSONException e) {
                                                    // It's not a jsonobject. Pass the data to the calling object as is
                                                }
                                            }
                                        }

                                        if (responseBody instanceof JSONObject) {
                                                    // The body of the response was decoded into JSON. Look for error messages
                                            // If there was a server error, there will be a key named 'error' or 'status'
                                            try {
                                                errorMessage = ((JSONObject) responseBody).getString("error");
                                                String details = ((JSONObject) responseBody).getString("details");
                                                errorMessage = errorMessage + " (" + details + ")";
                                            } catch (JSONException e) {
                                                // If there was no 'error' key, then assume the operation was successful
                                            }
                                        }
                                    }

                                    if (rateLimitErrorPresent) {
                                        if (attempt < 3) {
                                            Log.debug(TAG + "Performing rate-limited retry " + attempt + 1);
                                            sendEvent(httpMethod, array, attempt + 1, completionListener, rateLimitDelay);
                                        } else {
                                            completionListener.onError("API rate limit was exceeded");
                                        }
                                    } else {
                                        if (null == errorMessage) {
                                            completionListener.onSuccess(responseBody);
                                        } else {
                                            completionListener.onError(errorMessage);
                                        }
                                    }
                                } catch (JSONException e) {
                                    completionListener.onError("Unexpected response from server");
                                }
                            } else {
                                completionListener.onError("Unexpected response from server");
                            }

                            // Signal that processing is complete and the next task in the worker thread may proceed
                            asyncTaskSignal.countDown();
                        }

                    });

                    try {
                        // Wait for the previous task to complete before moving to the next item in the worker thread queue. This is to avoid multiple socket requests from occurring simultaneously which causes instability in the socket.io library
                        asyncTaskSignal.await(REST_TASK_TIMEOUT, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    completionListener.onError("Can't complete request when not connected. Please reconnect!");
                }
            }
        }, delayMillis);
    }

    public void sendSignal(JSONObject message, String toEndpointID, final Respoke.TaskCompletionListener completionListener) {
        JSONObject data = new JSONObject();

        try {
            data.put("to", toEndpointID);
            data.put("signal", message.toString());
            data.put("toType", "web");

            sendRESTMessage("post", "/v1/signaling", data, new RESTListener() {
                @Override
                public void onSuccess(Object response) {
                    completionListener.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    completionListener.onError(errorMessage);
                }
            });
        } catch (JSONException e) {
            completionListener.onError("Error encoding signal to json");
        }
    }

    private void routeSignal(JSONObject message) {
        try {
            JSONObject signal = (JSONObject) message.get("body");
            JSONObject header = (JSONObject) message.get("header");
            String from = header.getString("from");
            String fromConnection = header.getString("fromConnection");

            if ((null != signal) && (null != from)) {
                String signalType = null;
                String sessionID = null;
                String target = null;
                String toConnection = null;

                try {
                    signalType = signal.getString("signalType");
                    sessionID = signal.getString("sessionId");
                    target = signal.getString("target");
                    toConnection = signal.getString("connectionId");
                } catch (JSONException e) {
                    // do nothing
                }

                if ((null != sessionID) && (null != signalType) && (null != target)) {
                    Log.debug(TAG + "Received signal " + signalType);
                    boolean isDirectConnection = target.equals("directConnection");

                    Listener listener = listenerReference.get();
                    if (null != listener) {
                        RespokeCall call = listener.callWithID(sessionID);

                        if (target.equals("call") || isDirectConnection) {
                            if (null != call) {
                                if (signalType.equals("bye")) {
                                    call.hangupReceived();
                                } else if (signalType.equals("answer")) {
                                    JSONObject sdp = (JSONObject) signal.get("sessionDescription");
                                    call.answerReceived(sdp, fromConnection);
                                } else if (signalType.equals("connected")) {
                                    if (null != toConnection) {
                                        if (toConnection.equals(connectionID)) {
                                            call.connectedReceived();
                                        } else {
                                            Log.debug(TAG + "Another device answered, hanging up.");
                                            call.hangupReceived();
                                        }
                                    } else {
                                        Log.debug(TAG + "Unable to find out which endpoint won the call, hanging up");
                                        call.hangupReceived();
                                    }
                                } else if (signalType.equals("iceCandidates")) {
                                    JSONArray candidates = (JSONArray) signal.get("iceCandidates");
                                    call.iceCandidatesReceived(candidates);
                                }
                            } else if (signalType.equals("offer")) {
                                JSONObject sdp = (JSONObject) signal.get("sessionDescription");

                                if (null != sdp) {
                                    Date timestamp;

                                    if (!header.isNull("timestamp")) {
                                        timestamp = new Date(header.getLong("timestamp"));
                                    } else {
                                        // Just use the current time if no date is specified in the header data
                                        timestamp = new Date();
                                    }

                                    if (isDirectConnection) {
                                        listener.onIncomingDirectConnection(sdp, sessionID, fromConnection, from, timestamp, RespokeSignalingChannel.this);
                                    } else {
                                        listener.onIncomingCall(sdp, sessionID, fromConnection, from, timestamp, RespokeSignalingChannel.this);
                                    }
                                } else {
                                    Log.debug(TAG + "Error: Offer missing sdp");
                                }
                            }
                        }
                    }
                } else {
                    Log.debug(TAG + "Error: Could not parse signal data");
                }
            } else {
                Log.debug(TAG + "Error: signal missing header data");
            }
        } catch (JSONException e) {
            Log.debug(TAG + "Unable to parse received signal");
        }
    }

};
