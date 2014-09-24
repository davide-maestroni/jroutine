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
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/24/14.
 */
class DefaultResultChannel<OUTPUT> implements ResultChannel<OUTPUT> {

    private final InvocationHandler<?, OUTPUT> mHandler;

    public DefaultResultChannel(final InvocationHandler<?, OUTPUT> handler) {

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

        return mHandler.resultAbort(throwable);
    }

    @Override
    public boolean isOpen() {

        return mHandler.isResultOpen();
    }

    @Override
    public ResultChannel<OUTPUT> after(final TimeDuration delay) {

        mHandler.resultAfter(delay);

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> after(final long delay, final TimeUnit timeUnit) {

        return after(TimeDuration.fromUnit(delay, timeUnit));
    }

    @Override
    public ResultChannel<OUTPUT> pass(final OutputChannel<OUTPUT> channel) {

        mHandler.resultPass(channel);

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> pass(final Iterable<? extends OUTPUT> outputs) {

        mHandler.resultPass(outputs);

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> pass(final OUTPUT output) {

        mHandler.resultPass(output);

        return this;
    }

    @Override
    public ResultChannel<OUTPUT> pass(final OUTPUT... outputs) {

        mHandler.resultPass(outputs);

        return this;
    }
}