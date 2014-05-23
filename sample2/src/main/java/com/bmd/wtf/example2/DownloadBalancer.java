package com.bmd.wtf.example2;

import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.arr.QueueArrayBalancer;

/**
 * This balancer removes from the queue the urls contained in the exception messages flowing
 * upstream.
 */
public class DownloadBalancer extends QueueArrayBalancer<String> {

    @Override
    public Object onPullDebris(final int streamNumber, final Floodgate<String, String> gate,
            final Object debris) {

        if (debris instanceof Throwable) {

            super.onPullDebris(streamNumber, gate, ((Throwable) debris).getMessage());

            return debris;
        }

        return super.onPullDebris(streamNumber, gate, debris);
    }
}