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
 * Interface representing an operation that accepts an input argument and returns no result.
 * <p>
 * Created by davide-maestroni on 09/21/2015.
 *
 * @param <IN> the input data type.
 */
public interface Consumer<IN> {

    /**
     * Performs this operation on the given argument.
     *
     * @param in the input argument.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void accept(IN in) throws Exception;
}
