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
package com.bmd.android.wtf.crr;

import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.flw.Fall;
import com.bmd.wtf.flw.Stream;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a current employing {@link android.os.AsyncTask} instances to execute the
 * waterfall commands in a background thread.
 * <p/>
 * Created by davide on 8/11/14.
 */
class AsyncTaskCurrent implements Current {

    private final Executor mExecutor;

    private final Handler mHandler;

    /**
     * Constructor.
     * <p/>
     * Note that, in case a null executor is passed as parameter, the default one will be used.
     *
     * @param executor the executor.
     */
    public AsyncTaskCurrent(final Executor executor) {

        mExecutor = executor;
        // the handler is needed to ensure that the async task is started from the main thread
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public <DATA> void flush(final Fall<DATA> fall, final Stream<DATA> origin) {

        mHandler.post(new FlushTask<DATA>(mExecutor, fall, origin));
    }

    @Override
    public void forward(final Fall<?> fall, final Throwable throwable) {

        mHandler.post(new ForwardTask(mExecutor, fall, throwable));
    }

    @Override
    public <DATA> void push(final Fall<DATA> fall, final DATA drop) {

        mHandler.post(new PushTask<DATA>(mExecutor, fall, drop));
    }

    @Override
    public <DATA> void pushAfter(final Fall<DATA> fall, final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        mHandler.postDelayed(new PushTask<DATA>(mExecutor, fall, drop), timeUnit.toMillis(delay));
    }

    @Override
    public <DATA> void pushAfter(final Fall<DATA> fall, final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        final Executor executor = mExecutor;

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                for (final DATA drop : drops) {

                    new PushTask<DATA>(executor, fall, drop).run();
                }
            }
        }, timeUnit.toMillis(delay));
    }

    /**
     * AsyncTask handling the flush of data.
     *
     * @param <DATA> the data type.
     */
    private static class FlushTask<DATA> extends RunnableTask {

        private final Fall<DATA> mFall;

        private final Stream<DATA> mOrigin;

        public FlushTask(final Executor executor, final Fall<DATA> fall,
                final Stream<DATA> origin) {

            super(executor);

            mFall = fall;
            mOrigin = origin;
        }

        @Override
        protected Void doInBackground(final Void... voids) {

            mFall.flush(mOrigin);

            return null;
        }
    }

    /**
     * AsyncTask handling the forwarding of unhandled exceptions.
     */
    private static class ForwardTask extends RunnableTask {

        private final Fall<?> mFall;

        private final Throwable mThrowable;

        public ForwardTask(final Executor executor, final Fall<?> fall, final Throwable throwable) {

            super(executor);

            mFall = fall;
            mThrowable = throwable;
        }

        @Override
        protected Void doInBackground(final Void... voids) {

            mFall.forward(mThrowable);

            return null;
        }
    }

    /**
     * AsyncTask handling the push of data drops.
     *
     * @param <DATA> the data type.
     */
    private static class PushTask<DATA> extends RunnableTask {

        private final DATA mDrop;

        private final Fall<DATA> mFall;

        public PushTask(final Executor executor, final Fall<DATA> fall, final DATA drop) {

            super(executor);

            mFall = fall;
            mDrop = drop;
        }

        @Override
        protected Void doInBackground(final Void... voids) {

            mFall.push(mDrop);

            return null;
        }
    }

    /**
     * Abstract implementation of an async task whose execution starts in a runnable.
     */
    private static abstract class RunnableTask extends AsyncTask<Void, Void, Void>
            implements Runnable {

        private final Executor mExecutor;

        public RunnableTask(final Executor executor) {

            mExecutor = executor;
        }

        @Override
        public void run() {

            boolean isExecuted = false;

            if (mExecutor != null) {

                if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {

                    isExecuted = true;
                    executeOnExecutor(mExecutor);
                }
            }

            if (!isExecuted) {

                execute();
            }
        }
    }
}