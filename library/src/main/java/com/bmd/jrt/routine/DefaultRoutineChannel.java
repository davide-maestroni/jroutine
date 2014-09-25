/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.jrt.routine;

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/24/14.
 */
class DefaultRoutineChannel<INPUT, OUTPUT> implements RoutineChannel<INPUT, OUTPUT> {

    private final ExecutionHandler<INPUT, OUTPUT> mHandler;

    public DefaultRoutineChannel(final ExecutionHandler<INPUT, OUTPUT> handler) {

        if (handler == null) {

            throw new IllegalArgumentException();
        }

        mHandler = handler;
    }

    @Override
    public boolean abort() {

        return abort(null);
    }

    @Override
    public boolean abort(final Throwable throwable) {

        return mHandler.inputAbort(throwable);
    }

    @Override
    public boolean isOpen() {

        return mHandler.isInputOpen();
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> after(final TimeDuration delay) {

        mHandler.inputAfter(delay);

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> after(final long delay, final TimeUnit timeUnit) {

        return after(TimeDuration.fromUnit(delay, timeUnit));
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final OutputChannel<INPUT> channel) {

        mHandler.inputPass(channel);

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final Iterable<? extends INPUT> inputs) {

        mHandler.inputPass(inputs);

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final INPUT input) {

        mHandler.inputPass(input);

        return this;
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> pass(final INPUT... inputs) {

        mHandler.inputPass(inputs);

        return this;
    }

    @Override
    public OutputChannel<OUTPUT> close() {

        return mHandler.inputClose();
    }
}