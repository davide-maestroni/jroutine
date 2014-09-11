package com.bmd.jrt.routine;

import com.bmd.jrt.process.ResultPublisher;
import com.bmd.jrt.routine.Routine.ResultFilter;

/**
 * Created by davide on 9/11/14.
 */
public class ResultFilterAdapter<RESULT> implements ResultFilter<RESULT> {

    @Override
    public void onEnd(final ResultPublisher<RESULT> results) {

    }

    @Override
    public void onException(final Throwable throwable, final ResultPublisher<RESULT> results) {

        results.publishException(throwable);
    }

    @Override
    public void onReset(final ResultPublisher<RESULT> results) {

    }

    @Override
    public void onResult(final RESULT result, final ResultPublisher<RESULT> results) {

        results.publish(result);
    }
}