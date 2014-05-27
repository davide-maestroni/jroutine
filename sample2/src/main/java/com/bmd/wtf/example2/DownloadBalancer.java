package com.bmd.wtf.example2;

import com.bmd.wtf.src.Spring;

import java.util.List;

/**
 * This balancer removes from the queue the urls contained in the exception messages flowing
 * upstream.
 */
public class DownloadBalancer extends QueueArrayBalancer<String> {

    @Override
    public void onDrop(final List<Spring<String>> springs, final Object debris) {

        if (debris instanceof Throwable) {

            super.onDrop(springs, ((Throwable) debris).getMessage());

            return;
        }

        super.onDrop(springs, debris);
    }
}