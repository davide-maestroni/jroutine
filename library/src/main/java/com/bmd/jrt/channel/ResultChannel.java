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
public interface ResultChannel<INPUT> extends InputChannel<INPUT> {

    public ResultChannel<INPUT> after(TimeDuration delay);

    public ResultChannel<INPUT> after(long delay, TimeUnit timeUnit);

    public ResultChannel<INPUT> pass(OutputChannel<INPUT> channel);

    public ResultChannel<INPUT> pass(Iterable<? extends INPUT> inputs);

    public ResultChannel<INPUT> pass(INPUT input);

    public ResultChannel<INPUT> pass(INPUT... inputs);
}