/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.channel;

import com.gh.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a result channel, that is the channel used by the routine invocation to
 * publish the results into the output channel.
 * <p/>
 * Created by davide on 9/15/14.
 *
 * @param <OUTPUT> the output data type.
 */
public interface ResultChannel<OUTPUT> extends InputChannel<OUTPUT> {

    @Nonnull
    @Override
    ResultChannel<OUTPUT> after(@Nonnull TimeDuration delay);

    @Nonnull
    @Override
    ResultChannel<OUTPUT> after(long delay, @Nonnull TimeUnit timeUnit);

    @Nonnull
    @Override
    ResultChannel<OUTPUT> now();

    @Nonnull
    @Override
    ResultChannel<OUTPUT> pass(@Nullable OutputChannel<? extends OUTPUT> channel);

    @Nonnull
    @Override
    ResultChannel<OUTPUT> pass(@Nullable Iterable<? extends OUTPUT> outputs);

    @Nonnull
    @Override
    ResultChannel<OUTPUT> pass(@Nullable OUTPUT output);

    @Nonnull
    @Override
    ResultChannel<OUTPUT> pass(@Nullable OUTPUT... outputs);
}
