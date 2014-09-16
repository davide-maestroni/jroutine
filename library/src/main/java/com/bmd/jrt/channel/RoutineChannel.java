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
package com.bmd.jrt.channel;

import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/15/14.
 */
public interface RoutineChannel<INPUT, OUTPUT> extends InputChannel<INPUT> {

    public RoutineChannel<INPUT, OUTPUT> after(TimeDuration delay);

    public RoutineChannel<INPUT, OUTPUT> after(long delay, TimeUnit timeUnit);

    public RoutineChannel<INPUT, OUTPUT> push(OutputChannel<INPUT> channel);

    public RoutineChannel<INPUT, OUTPUT> push(Iterable<? extends INPUT> inputs);

    public RoutineChannel<INPUT, OUTPUT> push(INPUT input);

    public RoutineChannel<INPUT, OUTPUT> push(INPUT... inputs);

    public OutputChannel<OUTPUT> close();
}