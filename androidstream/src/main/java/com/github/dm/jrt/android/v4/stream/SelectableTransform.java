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

package com.github.dm.jrt.android.v4.stream;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.StreamChannel.StreamConfiguration;

import static com.github.dm.jrt.function.Functions.wrap;

/**
 * Selectable transform function.
 * <p>
 * Created by davide-maestroni on 05/08/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class SelectableTransform<IN, OUT> implements
        BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?, OUT>>,
                Function<Channel<?, IN>, Channel<?, ParcelableSelectable<OUT>>>> {

    private final int mIndex;

    /**
     * Constructor.
     *
     * @param index the selectable index.
     */
    SelectableTransform(final int index) {
        mIndex = index;
    }

    @Override
    public Function<Channel<?, IN>, Channel<?, ParcelableSelectable<OUT>>> apply(
            final StreamConfiguration configuration,
            final Function<Channel<?, IN>, Channel<?, OUT>> function) {
        return wrap(function).andThen(
                new BindSelectable<OUT>(configuration.asChannelConfiguration(), mIndex));
    }
}
