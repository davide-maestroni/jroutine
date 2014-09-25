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

import com.bmd.jrt.execution.Execution;

/**
 * Created by davide on 9/24/14.
 */
interface RoutineInvocationProvider<INPUT, OUTPUT> {

    public Execution<INPUT, OUTPUT> create();

    public void discard(Execution<INPUT, OUTPUT> invocation);

    public void recycle(Execution<INPUT, OUTPUT> invocation);
}