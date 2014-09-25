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

import com.bmd.jrt.time.TimeDuration;

import java.util.Iterator;

/**
 * Created by davide on 9/24/14.
 */
class DefaultIterator<OUTPUT> implements Iterator<OUTPUT> {

    private final ExecutionHandler<?, OUTPUT> mHandler;

    private final TimeDuration mTimeout;

    private final RuntimeException mTimeoutException;

    private boolean mRemoved = true;

    public DefaultIterator(final ExecutionHandler<?, OUTPUT> handler, final TimeDuration timeout,
            final RuntimeException timeoutException) {

        if (handler == null) {

            throw new IllegalArgumentException();
        }

        if (timeout == null) {

            throw new IllegalArgumentException();
        }

        mHandler = handler;
        mTimeout = timeout;
        mTimeoutException = timeoutException;
    }

    @Override
    public boolean hasNext() {

        return mHandler.hasOutput(mTimeout, mTimeoutException);
    }

    @Override
    public OUTPUT next() {

        final OUTPUT output = mHandler.nextOutput(mTimeout, mTimeoutException);

        mRemoved = false;

        return output;
    }

    @Override
    public void remove() {

        mHandler.validateOutput();

        if (mRemoved) {

            throw new IllegalStateException();
        }

        mRemoved = true;
    }
}