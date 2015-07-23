/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.digium.respokesdk;
import android.os.Handler;
import android.os.Looper;
/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class AndroidRespokeGroup extends RespokeGroup {
    
    public AndroidRespokeGroup(String newGroupID, RespokeSignalingChannel channel, RespokeClient newClient) {
        super(newGroupID,  channel,  newClient);
    }
    
    void queueRunnable(Runnable r){
        new Handler(Looper.getMainLooper()).post(r);
    }
}
