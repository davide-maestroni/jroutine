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

package com.github.dm.jrt.function;

/**
 * Interface representing an operation that accepts two input arguments and produces a result.
 * <p>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 * @param <OUT> the output data type.
 */
public interface BiFunction<IN1, IN2, OUT> {

    /**
     * Applies this function to the given arguments.
     *
     * @param in1 the first input argument.
     * @param in2 the second input argument.
     * @return the function result.
     */
    OUT apply(IN1 in1, IN2 in2);
}
