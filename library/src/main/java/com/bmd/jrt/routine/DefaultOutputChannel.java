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
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/24/14.
 */
class DefaultOutputChannel<OUTPUT> implements OutputChannel<OUTPUT> {

    private final ExecutionHandler<?, OUTPUT> mHandler;

    public DefaultOutputChannel(final ExecutionHandler<?, OUTPUT> handler) {

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

        return mHandler.outputAbort(throwable);
    }

    @Override
    public boolean isOpen() {

        return mHandler.isOutputOpen();
    }

    @Override
    public OutputChannel<OUTPUT> afterMax(final TimeDuration timeout) {

        mHandler.outputAfterMax(timeout);

        return this;
    }

    @Override
    public OutputChannel<OUTPUT> afterMax(final long timeout, final TimeUnit timeUnit) {

        return afterMax(TimeDuration.fromUnit(timeout, timeUnit));
    }

    @Override
    public OutputChannel<OUTPUT> bind(final OutputConsumer<OUTPUT> consumer) {

        mHandler.outputBind(consumer);

        return this;
    }

    @Override
    public OutputChannel<OUTPUT> eventuallyThrow(final RuntimeException exception) {

        mHandler.outputTimeoutException(exception);

        return this;
    }

    @Override
    public OutputChannel<OUTPUT> immediately() {

        return afterMax(TimeDuration.ZERO);
    }

    @Override
    public List<OUTPUT> readAll() {

        final ArrayList<OUTPUT> results = new ArrayList<OUTPUT>();
        readAllInto(results);

        return results;
    }

    @Override
    public OutputChannel<OUTPUT> readAllInto(final List<OUTPUT> results) {

        mHandler.outputReadInto(results);

        return this;
    }

    @Override
    public boolean waitDone() {

        return mHandler.outputWaitDone();
    }

    @Override
    public Iterator<OUTPUT> iterator() {

        return mHandler.outputIterator();
    }
}