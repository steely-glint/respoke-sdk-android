/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.digium.respokesdk;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
import android.os.Handler;
import android.os.Looper;
public class AndroidRespokeClient extends RespokeClient{
    public static final String PROPERTY_LAST_VALID_PUSH_TOKEN = "pushToken";
    public static final String PROPERTY_LAST_VALID_PUSH_TOKEN_ID = "pushTokenServiceID";
    private String pushServiceID; ///< The push service ID
    void queueRunnable(Runnable r){
        new Handler(Looper.getMainLooper()).post(r);
    }

    public void registerPushServicesWithToken(final String token) {
        String httpURI;
        String httpMethod;
        JSONObject data = new JSONObject();
        try {
            data.put("token", token);
            data.put("service", "google");
            SharedPreferences prefs = appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE);
            if (null != prefs) {
                String lastKnownPushToken = prefs.getString(PROPERTY_LAST_VALID_PUSH_TOKEN, "notAvailable");
                String lastKnownPushTokenID = prefs.getString(PROPERTY_LAST_VALID_PUSH_TOKEN_ID, "notAvailable");
                if ((null == lastKnownPushToken) || (lastKnownPushToken.equals("notAvailable"))) {
                    httpURI = String.format("/v1/connections/%s/push-token", localConnectionID);
                    httpMethod = "post";
                    createOrUpdatePushServiceToken(token, httpURI, httpMethod, data, prefs);
                } else if (!lastKnownPushToken.equals("notAvailable") && !lastKnownPushToken.equals(token)) {
                    httpURI = String.format("/v1/connections/%s/push-token/%s", localConnectionID, lastKnownPushTokenID);
                    httpMethod = "put";
                    createOrUpdatePushServiceToken(token, httpURI, httpMethod, data, prefs);
                }
            }
        } catch (JSONException e) {
            Log.d("", "Invalid JSON format for token");
        }
    }

    private void createOrUpdatePushServiceToken(final String token, String httpURI, String httpMethod, JSONObject data, final SharedPreferences prefs) {
        signalingChannel.sendRESTMessage(httpMethod, httpURI, data, new RespokeSignalingChannel.RESTListener() {
            @Override
            public void onSuccess(Object response) {
                if (response instanceof JSONObject) {
                    try {
                        JSONObject responseJSON = (JSONObject) response;
                        pushServiceID = responseJSON.getString("id");
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(PROPERTY_LAST_VALID_PUSH_TOKEN, token);
                        editor.putString(PROPERTY_LAST_VALID_PUSH_TOKEN_ID, pushServiceID);
                        editor.apply();
                    } catch (JSONException e) {
                        Log.d(TAG, "Unexpected response from server while registering push service token");
                    }
                } else {
                    Log.d(TAG, "Unexpected response from server while registering push service token");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.d(TAG, "Error registering push service token: " + errorMessage);
            }
        });
    }

    public void unregisterFromPushServices(final Respoke.TaskCompletionListener completionListener) {
        if (isConnected()) {
            SharedPreferences prefs = appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE);
            if (null != prefs) {
                String lastKnownPushTokenID = prefs.getString(PROPERTY_LAST_VALID_PUSH_TOKEN_ID, "notAvailable");
                if ((null != lastKnownPushTokenID) && !lastKnownPushTokenID.equals("notAvailable")) {
                    // A push token has previously been registered successfully
                    String httpURI = String.format("/v1/connections/%s/push-token/%s", localConnectionID, lastKnownPushTokenID);
                    signalingChannel.sendRESTMessage("delete", httpURI, null, new RespokeSignalingChannel.RESTListener() {
                        @Override
                        public void onSuccess(Object response) {
                            // Remove the push token ID from shared memory so that push may be registered again in the future
                            SharedPreferences prefs = appContext.getSharedPreferences(appContext.getPackageName(), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove(PROPERTY_LAST_VALID_PUSH_TOKEN_ID);
                            editor.apply();
                            Respoke.postTaskSuccess(completionListener);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Respoke.postTaskError(completionListener, "Error unregistering push service token: " + errorMessage);
                        }
                    });
                } else {
                    Respoke.postTaskSuccess(completionListener);
                }
            } else {
                Respoke.postTaskError(completionListener, "Unable to access shared preferences to look for push token");
            }
        } else {
            Respoke.postTaskError(completionListener, "Can't complete request when not connected. Please reconnect!");
        }
    }
}
