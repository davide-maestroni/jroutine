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
package com.bmd.jrt.channel;

import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a result channel, that is the channel used by the routine invocation to
 * publish the results into the output channel.
 * <p/>
 * Created by davide on 9/15/14.
 *
 * @param <OUTPUT> the output type.
 */
public interface ResultChannel<OUTPUT> extends InputChannel<OUTPUT> {

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> after(@Nonnull TimeDuration delay);

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> after(long delay, @Nonnull TimeUnit timeUnit);

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> now();

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable OutputChannel<OUTPUT> channel);

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable Iterable<? extends OUTPUT> outputs);

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable OUTPUT output);

    @Override
    @Nonnull
    public ResultChannel<OUTPUT> pass(@Nullable OUTPUT... outputs);
}