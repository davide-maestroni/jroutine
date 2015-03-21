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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.channel.OutputChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Empty abstract implementation of a routine.
 * <p/>
 * This class is useful to avoid the need of implementing some of the methods defined in the
 * interface.
 * <p/>
 * Created by davide on 10/17/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class TemplateRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    @Nonnull
    public OutputChannel<OUTPUT> callAsync() {

        return invokeAsync().result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callAsync(@Nullable final INPUT input) {

        return invokeAsync().pass(input).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callAsync(@Nullable final INPUT... inputs) {

        return invokeAsync().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callAsync(@Nullable final Iterable<? extends INPUT> inputs) {

        return invokeAsync().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callAsync(@Nullable final OutputChannel<? extends INPUT> inputs) {

        return invokeAsync().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callParallel() {

        return invokeParallel().result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callParallel(@Nullable final INPUT input) {

        return invokeParallel().pass(input).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callParallel(@Nullable final INPUT... inputs) {

        return invokeParallel().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callParallel(@Nullable final Iterable<? extends INPUT> inputs) {

        return invokeParallel().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callParallel(
            @Nullable final OutputChannel<? extends INPUT> inputs) {

        return invokeParallel().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callSync() {

        return invokeSync().result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callSync(@Nullable final INPUT input) {

        return invokeSync().pass(input).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callSync(@Nullable final INPUT... inputs) {

        return invokeSync().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callSync(@Nullable final Iterable<? extends INPUT> inputs) {

        return invokeSync().pass(inputs).result();
    }

    @Nonnull
    public OutputChannel<OUTPUT> callSync(@Nullable final OutputChannel<? extends INPUT> inputs) {

        return invokeSync().pass(inputs).result();
    }

    public void purge() {

    }
}
