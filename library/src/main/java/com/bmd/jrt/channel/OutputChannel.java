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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 9/4/14.
 */
public interface OutputChannel<OUTPUT> extends Channel, Iterable<OUTPUT> {

    public OutputChannel<OUTPUT> afterMax(TimeDuration timeout);

    public OutputChannel<OUTPUT> afterMax(long timeout, TimeUnit timeUnit);

    public OutputChannel<OUTPUT> bind(OutputConsumer<OUTPUT> consumer);

    public OutputChannel<OUTPUT> eventuallyThrow(RuntimeException exception);

    public OutputChannel<OUTPUT> immediately();

    public List<OUTPUT> readAll();

    public OutputChannel<OUTPUT> readAllInto(List<OUTPUT> results);

    public boolean waitDone();
}