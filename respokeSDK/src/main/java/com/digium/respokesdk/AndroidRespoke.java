/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.digium.respokesdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRendererGui;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class AndroidRespoke extends Respoke {
    private String pushToken;
    private static boolean factoryStaticInitialized;

    /**
     * A helper function to post success to a TaskCompletionListener on the UI
     * thread
     *
     * @param completionListener The TaskCompletionListener to notify
     */
    public static void postTaskSuccess(final TaskCompletionListener completionListener) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (null != completionListener) {
                    completionListener.onSuccess();
                }
            }
        });
    }

    /**
     * A helper function to post an error message to a TaskCompletionListener on
     * the UI thread
     *
     * @param completionListener The TaskCompletionListener to notify
     * @param errorMessage The error message to post
     */
    public static void postTaskError(final TaskCompletionListener completionListener, final String errorMessage) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (null != completionListener) {
                    completionListener.onError(errorMessage);
                }
            }
        });
    }

    public void clientConnected(RespokeClient client, String endpointID) {
        if (null != pushToken) {
            registerPushServices();
        }
        if (!factoryStaticInitialized) {
            PeerConnectionFactory.initializeAndroidGlobals(context, true, true, true, VideoRendererGui.getEGLContext());
            factoryStaticInitialized = true;
        }
    }

    public void registerPushServices() {
        AndroidRespokeClient activeInstance = null;

        // If there are already client instances running, check if any of them have already connected
        for (RespokeClient eachInstance : instances) {
            if ((eachInstance instanceof AndroidRespokeClient) && (eachInstance.isConnected())) {
                // The push service only supports one endpoint per device, so the token only needs to be registered for the first active client (if there is more than one)
                activeInstance = (AndroidRespokeClient) eachInstance;
            }
        }

        if (null != activeInstance) {
            // Notify the Respoke servers that this device is eligible to receive notifications directed at this endpointID
            activeInstance.registerPushServicesWithToken(pushToken);
        }
    }

    public void unregisterPushServices(TaskCompletionListener completionListener) {
        AndroidRespokeClient activeInstance = null;

        // If there are already client instances running, check if any of them have already connected
        for (RespokeClient eachInstance : instances) {
            
            if ((eachInstance instanceof AndroidRespokeClient) &&  (eachInstance.isConnected())) {
                // The push service only supports one endpoint per device, so the token only needs to be registered for the first active client (if there is more than one)
                activeInstance = (AndroidRespokeClient) eachInstance;
                break;
            }
        }

        if (null != activeInstance) {
            activeInstance.unregisterFromPushServices(completionListener);
        } else {
            postTaskError(completionListener, "There is no active client to unregister");
        }
    }
   

    public void registerPushToken(String token) {
        pushToken = token;

        if (instances.size() > 0) {
            registerPushServices();
        }
    }
}
