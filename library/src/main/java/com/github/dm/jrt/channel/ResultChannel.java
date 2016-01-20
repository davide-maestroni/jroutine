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

import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

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
    @NotNull
    ResultChannel<OUT> after(@NotNull TimeDuration delay);

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> after(long delay, @NotNull TimeUnit timeUnit);

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> now();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> orderByCall();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> orderByDelay();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> pass(@Nullable OutputChannel<? extends OUT> channel);

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> pass(@Nullable Iterable<? extends OUT> outputs);

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> pass(@Nullable OUT output);

    /**
     * {@inheritDoc}
     */
    @NotNull
    ResultChannel<OUT> pass(@Nullable OUT... outputs);
}
