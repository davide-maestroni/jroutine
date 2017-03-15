/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.stream.config.StreamConfiguration;

/**
 * Interface defining a function lifting a stream builder.
 * <p>
 * Created by davide-maestroni on 01/30/2017.
 *
 * @param <IN>     the input data type.
 * @param <OUT>    the output data type.
 * @param <BEFORE> the input type after the lifting.
 * @param <AFTER>  the output type after the lifting.
 */
public interface LiftFunction<IN, OUT, BEFORE, AFTER> extends
    BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?, OUT>>,
        Function<Channel<?, BEFORE>, Channel<?, AFTER>>> {

}
