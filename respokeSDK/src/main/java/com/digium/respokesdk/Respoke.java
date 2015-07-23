/**
 * Copyright 2015, Digium, Inc. All rights reserved.
 *
 * This source code is licensed under The MIT License found in the LICENSE file
 * in the root directory of this source tree.
 *
 * For all details and documentation: https://www.respoke.io
 */
package com.digium.respokesdk;

import java.util.ArrayList;

/**
 * A global static class which provides access to the Respoke functionality.
 */
public class Respoke {

    public final static int GUID_STRING_LENGTH = 36;
    private static Respoke _instance;
    protected ArrayList<RespokeClient> instances;

    void clientConnected(RespokeClient aThis, String endpointID) {
    }

    public interface TaskCompletionListener {

        void onSuccess();

        void onError(String errorMessage);

    }

    public static void postTaskSuccess(final TaskCompletionListener completionListener) {

        if (null != completionListener) {
            completionListener.onSuccess();
        }

    }

    public static void postTaskError(final TaskCompletionListener completionListener, final String errorMessage) {

        if (null != completionListener) {
            completionListener.onError(errorMessage);
        }

    }

    protected Respoke() {
        instances = new ArrayList<RespokeClient>();
    }

    public static Respoke sharedInstance() {
        if (_instance == null) {
            _instance = new Respoke();
        }

        return _instance;
    }

    public RespokeClient createClient() {

        RespokeClient newClient = new RespokeClient();
        instances.add(newClient);

        return newClient;
    }

    public void unregisterClient(RespokeClient client) {
        instances.remove(client);
    }

    public static String makeGUID() {
        String uuid = "";
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int rnd = 0;
        int r;

        for (int i = 0; i < GUID_STRING_LENGTH; i += 1) {
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                uuid = uuid + "-";
            } else if (i == 14) {
                uuid = uuid + "4";
            } else {
                if (rnd <= 0x02) {
                    rnd = (int) (0x2000000 + Math.round(java.lang.Math.random() * 0x1000000));
                }
                r = rnd & 0xf;
                rnd = rnd >> 4;

                if (i == 19) {
                    uuid = uuid + chars.charAt((r & 0x3) | 0x8);
                } else {
                    uuid = uuid + chars.charAt(r);
                }
            }
        }
        return uuid;
    }





}
