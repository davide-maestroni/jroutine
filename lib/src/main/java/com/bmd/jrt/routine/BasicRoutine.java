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

import java.util.List;

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
public abstract class BasicRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    @Nonnull
    @Override
    public List<OUTPUT> call() {

        return run().readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> call(@Nullable final INPUT input) {

        return run(input).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> call(@Nullable final INPUT... inputs) {

        return run(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> call(@Nullable final Iterable<? extends INPUT> inputs) {

        return run(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> call(@Nullable final OutputChannel<INPUT> inputs) {

        return run(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callAsync() {

        return runAsync().readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callAsync(@Nullable final INPUT input) {

        return runAsync(input).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callAsync(@Nullable final INPUT... inputs) {

        return runAsync(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callAsync(@Nullable final Iterable<? extends INPUT> inputs) {

        return runAsync(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callAsync(@Nullable final OutputChannel<INPUT> inputs) {

        return runAsync(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callParallel() {

        return runParallel().readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callParallel(@Nullable final INPUT input) {

        return runParallel(input).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callParallel(@Nullable final INPUT... inputs) {

        return runParallel(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callParallel(@Nullable final Iterable<? extends INPUT> inputs) {

        return runParallel(inputs).readAll();
    }

    @Nonnull
    @Override
    public List<OUTPUT> callParallel(@Nullable final OutputChannel<INPUT> inputs) {

        return runParallel(inputs).readAll();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> run() {

        return invoke().results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> run(@Nullable final INPUT input) {

        return invoke().pass(input).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> run(@Nullable final INPUT... inputs) {

        return invoke().pass(inputs).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> run(@Nullable final Iterable<? extends INPUT> inputs) {

        return invoke().pass(inputs).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> run(@Nullable final OutputChannel<INPUT> inputs) {

        return invoke().pass(inputs).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runAsync() {

        return invokeAsync().results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runAsync(@Nullable final INPUT input) {

        return invokeAsync().pass(input).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runAsync(@Nullable final INPUT... inputs) {

        return invokeAsync().pass(inputs).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runAsync(@Nullable final Iterable<? extends INPUT> inputs) {

        return invokeAsync().pass(inputs).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runAsync(@Nullable final OutputChannel<INPUT> inputs) {

        return invokeAsync().pass(inputs).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runParallel() {

        return invokeParallel().results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runParallel(@Nullable final INPUT input) {

        return invokeParallel().pass(input).results();
    }

    @Nonnull
    @Override
    public OutputChannel<OUTPUT> runParallel(@Nullable final INPUT... inputs) {

        return invokeParallel().pass(inputs).results();
    }
}
