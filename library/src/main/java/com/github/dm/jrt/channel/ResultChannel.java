/*
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
package com.github.dm.jrt.channel;

import com.github.dm.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a result channel, that is the channel used by the routine invocation to
 * publish the results into the output channel.
 * <p/>
 * Created by davide-maestroni on 9/15/14.
 *
 * @param <OUTPUT> the output data type.
 */
public interface ResultChannel<OUTPUT> extends InputChannel<OUTPUT> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> after(@Nonnull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> after(long delay, @Nonnull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> now();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> pass(@Nullable OutputChannel<? extends OUTPUT> channel);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> pass(@Nullable Iterable<? extends OUTPUT> outputs);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> pass(@Nullable OUTPUT output);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUTPUT> pass(@Nullable OUTPUT... outputs);
}
