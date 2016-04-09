/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.core.runner;

import android.os.Handler;
import android.os.Looper;

import com.github.dm.jrt.core.runner.RunnerDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.runner.Runners.zeroDelayRunner;

/**
 * Implementation of a runner employing an Android {@link android.os.Looper} queue to execute the
 * routine invocations.
 * <p>
 * Created by davide-maestroni on 09/28/2014.
 */
class LooperRunner extends RunnerDecorator {

    /**
     * Constructor.
     * <p>
     * Note that, when the invocation runs in the looper thread, the executions with a delay of 0
     * will be performed synchronously, while the ones with a positive delay will be posted on the
     * same thread.
     *
     * @param looper the looper to employ.
     */
    LooperRunner(@NotNull final Looper looper) {

        super(zeroDelayRunner(new HandlerRunner(new Handler(looper))));
    }
}
