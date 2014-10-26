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

import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.runner.Runners.sharedRunner;

/**
 * Default implementation of an input/output channel.
 * <p/>
 * Created by davide on 10/24/14.
 *
 * @param <T> the data type.
 */
class DefaultIOChannel<T> implements com.bmd.jrt.channel.IOChannel<T> {

    private final DefaultResultChannel<T> mInputChannel;

    private final OutputChannel<T> mOutputChannel;

    /**
     * Constructor.a
     *
     * @param isOrdered whether the input is ordered.
     * @param maxAge    the channel max age.
     * @param log       the log instance.
     * @param level     the log level.
     */
    DefaultIOChannel(final boolean isOrdered, @Nonnull final TimeDuration maxAge,
            @Nonnull final Log log, @Nonnull final LogLevel level) {

        final Runner runner = sharedRunner();
        final Logger logger = Logger.create(log, level, this);
        final DefaultResultChannel<T> inputChannel =
                new DefaultResultChannel<T>(new AbortHandler() {

                    @Override
                    public void onAbort(@Nullable final Throwable reason, final long delay,
                            @Nonnull final TimeUnit timeUnit) {

                        mInputChannel.close(reason);
                    }
                }, runner, isOrdered, logger);
        mInputChannel = inputChannel;
        mOutputChannel = inputChannel.getOutput();

        if (!maxAge.isInfinite()) {

            runner.run(new Execution() {

                @Override
                public void run() {

                    logger.dbg("closing aged channel %s", maxAge);

                    close();
                }
            }, maxAge.time, maxAge.unit);
        }
    }

    @Override
    public void close() {

        mInputChannel.close();
    }

    @Override
    @Nonnull
    public InputChannel<T> input() {

        return mInputChannel;
    }

    @Override
    @Nonnull
    public OutputChannel<T> output() {

        return mOutputChannel;
    }
}