package com.bmd.jrt.routine;

import com.bmd.jrt.process.ResultPublisher;

/**
 * Created by davide on 9/10/14.
 */
public interface ScopedPublisher<OUTPUT> extends ResultPublisher<OUTPUT> {

    public void enter();

    public void exit();
}