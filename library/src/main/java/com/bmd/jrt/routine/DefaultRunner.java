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

import com.bmd.jrt.runner.Invocation;
import com.bmd.jrt.runner.Runner;

import java.util.concurrent.TimeUnit;

/**
 * Empty runner implementation used to identify the default runner in a
 * {@link com.bmd.jrt.routine.AsynMethod} annotation.
 * <p/>
 * Created by davide on 9/22/14.
 */
class DefaultRunner implements Runner {

    @Override
    public void run(final Invocation invocation, final long delay, final TimeUnit timeUnit) {

    }

    @Override
    public void runAbort(final Invocation invocation) {

    }
}