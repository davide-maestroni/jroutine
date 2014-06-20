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
package com.bmd.wtf.flg;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/13/14.
 */
public interface GateControl<TYPE> {

    public GateControl<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    public GateControl<TYPE> eventually();

    public GateControl<TYPE> eventuallyThrow(RuntimeException exception);

    public GateControl<TYPE> meets(ConditionEvaluator<TYPE> evaluator);

    public <RESULT> RESULT perform(Action<RESULT, TYPE> action, Object... args);

    public interface Action<RESULT, TYPE> {

        public RESULT doOn(TYPE gate, Object... args);
    }

    public interface ConditionEvaluator<TYPE> {

        public boolean isSatisfied(TYPE gate);
    }
}