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
package com.bmd.wtf.gts;

import com.bmd.wtf.flw.River;

/**
 * Base abstract implementation of a gate. By default it behaves like a open gate.
 * <p/>
 * Created by davide on 6/9/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 * @see OpenGate
 */
public abstract class AbstractGate<IN, OUT> implements Gate<IN, OUT> {

    @Override
    public void onFlush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber) {

        downRiver.flush();
    }

    @Override
    public void onUnhandled(final River<IN> upRiver, final River<OUT> downRiver,
            final int fallNumber, final Throwable throwable) {

        downRiver.forward(throwable);
    }
}