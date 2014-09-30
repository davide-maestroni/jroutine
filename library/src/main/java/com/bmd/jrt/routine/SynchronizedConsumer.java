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

import com.bmd.jrt.channel.OutputConsumer;

/**
 * Decorator of an output consumer instance which synchronizes the access to its methods.
 * <p/>
 * Created by davide on 9/18/14.
 *
 * @param <OUTPUT> the output type.
 */
class SynchronizedConsumer<OUTPUT> implements OutputConsumer<OUTPUT> {

    private final OutputConsumer<OUTPUT> mConsumer;

    private final Object mMutex = new Object();

    /**
     * Constructor.
     *
     * @param wrapped the wrapped output consumer.
     * @throws NullPointerException is the specified consumer is null.
     */
    SynchronizedConsumer(final OutputConsumer<OUTPUT> wrapped) {

        if (wrapped == null) {

            throw new NullPointerException("the consumer instance must not be null");
        }

        mConsumer = wrapped;
    }

    @Override
    public void onAbort(final Throwable throwable) {

        synchronized (mMutex) {

            mConsumer.onAbort(throwable);
        }
    }

    @Override
    public void onClose() {

        synchronized (mMutex) {

            mConsumer.onClose();
        }
    }

    @Override
    public void onOutput(final OUTPUT output) {

        synchronized (mMutex) {

            mConsumer.onOutput(output);
        }
    }
}