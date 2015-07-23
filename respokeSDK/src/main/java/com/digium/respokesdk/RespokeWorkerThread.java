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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Implements a worker thread for queueing and processing socket transactions with the Respoke service
 */
public class RespokeWorkerThread extends Timer {

    HashMap <TimerTask,TimerTask> tasks ;
    RespokeWorkerThread(String name) {
        super(name);
        tasks = new HashMap();
    }

    void prepareHandler() {
        
    }

    void cancelAllTasks() {
        for (TimerTask t:tasks.keySet()){
            t.cancel();
        }
        tasks.clear();
    }

    void postTaskDelayed(final Runnable errand, long delayMillis) {
        TimerTask t = new TimerTask(){
            @Override
            public void run() {
                errand.run();
                tasks.remove(this);
            }
        };
        tasks.put(t, t);
        this.schedule(t, delayMillis);
    }

    void start() {
    }


}
