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

import com.bmd.wtf.flg.GateControl;
import com.bmd.wtf.fll.Classification;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/20/14.
 */
public interface RapidControl<TYPE> extends GateControl<TYPE> {

    public RapidControl<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    public RapidControl<TYPE> eventually();

    public RapidControl<TYPE> eventuallyThrow(RuntimeException exception);

    public RapidControl<TYPE> meets(ConditionEvaluator<TYPE> evaluator);

    public <NTYPE> RapidControl<NTYPE> as(final Class<NTYPE> gateType);

    public <NTYPE> RapidControl<NTYPE> as(final Classification<NTYPE> gate);

    public TYPE perform();
}