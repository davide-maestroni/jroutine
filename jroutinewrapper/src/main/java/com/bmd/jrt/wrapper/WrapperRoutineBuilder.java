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
package com.bmd.jrt.wrapper;

/**
 * Created by davide on 11/7/14.
 */
public class WrapperRoutineBuilder {

    protected WrapperRoutineBuilder(final Object target) {

    }

    public static WrapperRoutineBuilder wrap(final Object target) {

        return new WrapperRoutineBuilder(target);
    }

    //TODO: log, logLevel, runner, sequential, queued, parallelId, tryCatch
}
