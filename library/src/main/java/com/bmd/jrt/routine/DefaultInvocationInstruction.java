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

import com.bmd.jrt.invocation.RoutineInvocation;
import com.bmd.jrt.runner.InvocationInstruction;

/**
 * Created by davide on 9/24/14.
 */
class DefaultInvocationInstruction<INPUT, OUTPUT> implements InvocationInstruction {

    private final InvocationHandler<INPUT, OUTPUT> mHandler;

    private final Object mInvocationMutex = new Object();

    private final RoutineInvocationProvider<INPUT, OUTPUT> mInvocationProvider;

    private final DefaultResultChannel<OUTPUT> mResultChannel;

    private RoutineInvocation<INPUT, OUTPUT> mRoutine;

    public DefaultInvocationInstruction(final InvocationHandler<INPUT, OUTPUT> handler,
            final RoutineInvocationProvider<INPUT, OUTPUT> provider) {

        if (provider == null) {

            throw new IllegalArgumentException();
        }

        mHandler = handler;
        mInvocationProvider = provider;
        mResultChannel = new DefaultResultChannel<OUTPUT>(handler);
    }

    @Override
    public void abort() {

        synchronized (mInvocationMutex) {

            final InvocationHandler<INPUT, OUTPUT> handler = mHandler;
            final RoutineInvocationProvider<INPUT, OUTPUT> provider = mInvocationProvider;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;
            RoutineInvocation<INPUT, OUTPUT> invocation = null;

            if (!handler.isAborting()) {

                return;
            }

            final Throwable exception = handler.getAbortException();

            try {

                invocation = initInvocation();

                invocation.onAbort(exception);
                resultChannel.abort(exception);

                invocation.onReturn();
                provider.recycle(invocation);

            } catch (final Throwable t) {

                if (invocation != null) {

                    provider.discard(invocation);
                }

                resultChannel.abort(t);

            } finally {

                handler.onAbortComplete();
            }
        }
    }

    @Override
    public void run() {

        synchronized (mInvocationMutex) {

            final InvocationHandler<INPUT, OUTPUT> handler = mHandler;
            final DefaultResultChannel<OUTPUT> resultChannel = mResultChannel;

            try {

                if (!handler.onProcessInput()) {

                    return;
                }

                final RoutineInvocation<INPUT, OUTPUT> invocation = initInvocation();

                while (handler.hasInput()) {

                    invocation.onInput(handler.nextInput(), resultChannel);
                }

                if (handler.isResult()) {

                    invocation.onResult(resultChannel);
                    handler.resultClose();

                    invocation.onReturn();
                    mInvocationProvider.recycle(invocation);
                }

            } catch (final Throwable t) {

                resultChannel.abort(t);
            }
        }
    }

    private RoutineInvocation<INPUT, OUTPUT> initInvocation() {

        final RoutineInvocation<INPUT, OUTPUT> invocation;

        if (mRoutine != null) {

            invocation = mRoutine;

        } else {

            invocation = (mRoutine = mInvocationProvider.create());
            invocation.onInit();
        }

        return invocation;
    }
}