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
package com.bmd.wtf.xtr.dam;

import com.bmd.wtf.fll.Waterfall;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/11/14.
 */
public interface CollectorBasin<SOURCE, DATA> {

    public CollectorBasin<SOURCE, DATA> afterMax(long maxDelay, TimeUnit timeUnit);

    public CollectorBasin<SOURCE, DATA> all();

    public CollectorBasin<SOURCE, DATA> collect(List<DATA> bucket);

    public CollectorBasin<SOURCE, DATA> collect(int streamNumber, List<DATA> bucket);

    public CollectorBasin<SOURCE, DATA> collectUnhandled(List<Throwable> bucket);

    public CollectorBasin<SOURCE, DATA> collectUnhandled(int streamNumber, List<Throwable> bucket);

    public CollectorBasin<SOURCE, DATA> empty();

    public CollectorBasin<SOURCE, DATA> eventuallyThrow(RuntimeException exception);

    public CollectorBasin<SOURCE, DATA> immediately();

    public CollectorBasin<SOURCE, DATA> max(int maxCount);

    public CollectorBasin<SOURCE, DATA> onFlush();

    public DATA pull();

    public DATA pull(int streamNumber);

    public Throwable pullUnhandled();

    public Throwable pullUnhandled(int streamNumber);

    public Waterfall<SOURCE, DATA, DATA> release();

    public CollectorBasin<SOURCE, DATA> when(Condition<SOURCE, DATA> condition);

    public CollectorBasin<SOURCE, DATA> whenAvailable();

    public interface Condition<SOURCE, DATA> {

        public boolean matches(CollectorBasin<SOURCE, DATA> basin, List<List<DATA>> data,
                List<List<Throwable>> unhandled);
    }
}