package com.bmd.jrt.runner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Class implementing a runner employing an executor service.
 * <p/>
 * Created by davide on 10/14/14.
 */
class ScheduledRunner implements Runner {

    private final ScheduledExecutorService mService;

    /**
     * Constructor.
     *
     * @param service the executor service.
     */
    @SuppressWarnings("ConstantConditions")
    ScheduledRunner(@Nonnull final ScheduledExecutorService service) {

        if (service == null) {

            throw new NullPointerException("the executor service must not be null");
        }

        mService = service;
    }

    @Override
    public void run(@Nonnull final Invocation invocation, final long delay,
            @Nonnull final TimeUnit timeUnit) {

        if (delay > 0) {

            mService.schedule(invocation, delay, timeUnit);

        } else {

            mService.execute(invocation);
        }
    }
}