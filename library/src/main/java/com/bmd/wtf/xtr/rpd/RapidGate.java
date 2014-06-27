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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/20/14.
 */
public interface RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> extends Gate<TYPE>, River<SOURCE, IN> {

    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate();

    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate(int streamNumber);

    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain();

    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain(int streamNumber);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> eventually();

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> eventuallyThrow(RuntimeException exception);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> immediately();

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> meets(
            ConditionEvaluator<? super TYPE> evaluator);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> discharge();

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> forward(Throwable throwable);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(long delay, TimeUnit timeUnit,
            Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(long delay, TimeUnit timeUnit,
            IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(long delay, TimeUnit timeUnit,
            IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> discharge(int streamNumber);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> forward(int streamNumber, Throwable throwable);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(int streamNumber, IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(int streamNumber,
            Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(int streamNumber, IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(int streamNumber, long delay,
            TimeUnit timeUnit, Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(int streamNumber, long delay,
            TimeUnit timeUnit, IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(int streamNumber, long delay,
            TimeUnit timeUnit, IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, SOURCE, OUT, TYPE> source();

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> when(Class<NTYPE> gateType);

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> when(
            Classification<NTYPE> gateClassification);

    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> meetsCondition(Object... args);

    public RapidGate<SOURCE, MOUTH, MOUTH, OUT, TYPE> mouth();

    public TYPE perform();

    public Waterfall<SOURCE, MOUTH, OUT> waterfall();
}