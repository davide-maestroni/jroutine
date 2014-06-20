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
package com.bmd.wtf.flw;

import com.bmd.wtf.flg.GateControl;
import com.bmd.wtf.fll.Classification;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/7/14.
 */
public interface River<SOURCE, DATA> extends Stream<DATA> {

    public void drain();

    public void drain(int streamNumber);

    public void dryUp();

    public void dryUp(int streamNumber);

    public River<SOURCE, DATA> flush(int streamNumber);

    @Override
    public River<SOURCE, DATA> flush();

    @Override
    public River<SOURCE, DATA> forward(Throwable throwable);

    @Override
    public River<SOURCE, DATA> push(DATA... drops);

    @Override
    public River<SOURCE, DATA> push(Iterable<? extends DATA> drops);

    @Override
    public River<SOURCE, DATA> push(DATA drop);

    @Override
    public River<SOURCE, DATA> pushAfter(long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    @Override
    public River<SOURCE, DATA> pushAfter(long delay, TimeUnit timeUnit, DATA drop);

    @Override
    public River<SOURCE, DATA> pushAfter(long delay, TimeUnit timeUnit, DATA... drops);

    public River<SOURCE, DATA> forward(int streamNumber, Throwable throwable);

    public River<SOURCE, DATA> push(int streamNumber, DATA... drops);

    public River<SOURCE, DATA> push(int streamNumber, Iterable<? extends DATA> drops);

    public River<SOURCE, DATA> push(int streamNumber, DATA drop);

    public River<SOURCE, DATA> pushAfter(int streamNumber, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    public River<SOURCE, DATA> pushAfter(int streamNumber, long delay, TimeUnit timeUnit,
            DATA drop);

    public River<SOURCE, DATA> pushAfter(int streamNumber, long delay, TimeUnit timeUnit,
            DATA... drops);

    public int size();

    public River<SOURCE, SOURCE> source();

    public <TYPE> GateControl<TYPE> when(Classification<TYPE> gate);
}