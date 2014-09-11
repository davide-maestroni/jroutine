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

import com.bmd.jrt.time.PositiveDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/4/14.
 */
public interface InputChannel<INPUT, OUTPUT> extends Channel {

    public InputChannel<INPUT, OUTPUT> after(PositiveDuration delay);

    public InputChannel<INPUT, OUTPUT> after(long delay, TimeUnit timeUnit);

    public OutputChannel<OUTPUT> end();

    public InputChannel<INPUT, OUTPUT> push(INPUT... inputs);

    public InputChannel<INPUT, OUTPUT> push(Iterable<? extends INPUT> inputs);

    public InputChannel<INPUT, OUTPUT> push(INPUT input);
}