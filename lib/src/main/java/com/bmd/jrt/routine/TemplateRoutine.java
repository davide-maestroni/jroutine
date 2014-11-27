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

import com.bmd.jrt.channel.OutputChannel;

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
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
 */
public abstract class TemplateRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> call() {

        return invoke().result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> call(@Nullable final INPUT input) {

        return invoke().pass(input).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> call(@Nullable final INPUT... inputs) {

        return invoke().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> call(@Nullable final Iterable<? extends INPUT> inputs) {

        return invoke().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> call(@Nullable final OutputChannel<INPUT> inputs) {

        return invoke().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callAsync() {

        return invokeAsync().result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callAsync(@Nullable final INPUT input) {

        return invokeAsync().pass(input).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callAsync(@Nullable final INPUT... inputs) {

        return invokeAsync().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callAsync(@Nullable final Iterable<? extends INPUT> inputs) {

        return invokeAsync().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callAsync(@Nullable final OutputChannel<INPUT> inputs) {

        return invokeAsync().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callParallel() {

        return invokeParallel().result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callParallel(@Nullable final INPUT input) {

        return invokeParallel().pass(input).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callParallel(@Nullable final INPUT... inputs) {

        return invokeParallel().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callParallel(@Nullable final Iterable<? extends INPUT> inputs) {

        return invokeParallel().pass(inputs).result();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> callParallel(@Nullable final OutputChannel<INPUT> inputs) {

        return invokeParallel().pass(inputs).result();
    }
}
