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

package com.github.dm.jrt.channel;

import org.jetbrains.annotations.NotNull;

/**
 * Empty abstract implementation of an output consumer.
 * <p/>
 * This class is useful to avoid the need of implementing all the methods defined in the interface.
 * <p/>
 * Created by davide-maestroni on 09/16/2014.
 *
 * @param <OUT> the output data type.
 */
public abstract class TemplateOutputConsumer<OUT> implements OutputConsumer<OUT> {

    public void onComplete() {

    }

    public void onError(@NotNull final RoutineException error) {

    }

    public void onOutput(final OUT output) {

    }
}
