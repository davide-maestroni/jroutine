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
 * Created by davide-maestroni on 09/15/2014.
 *
 * @param <OUT> the output data type.
 */
public interface ResultChannel<OUT> extends InputChannel<OUT> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> after(@Nonnull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> after(long delay, @Nonnull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> now();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> orderByChance();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> pass(@Nullable OutputChannel<? extends OUT> channel);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> pass(@Nullable Iterable<? extends OUT> outputs);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> pass(@Nullable OUT output);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ResultChannel<OUT> pass(@Nullable OUT... outputs);
}
