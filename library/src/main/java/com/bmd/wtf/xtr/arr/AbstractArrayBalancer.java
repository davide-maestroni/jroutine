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
package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.util.List;

/**
 * Abstract base implementation of an {@link ArrayBalancer}. Flushes and debris are propagated
 * to all the streams.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public abstract class AbstractArrayBalancer<IN, OUT> implements ArrayBalancer<IN, OUT> {

    @Override
    public Object onFlush(final List<Spring<OUT>> springs) {

        for (final Spring<OUT> spring : springs) {

            spring.flush();
        }

        return null;
    }

    @Override
    public Object onPullDebris(final int streamNumber, final Floodgate<OUT, OUT> gate,
            final Object debris) {

        return debris;
    }

    @Override
    public Object onPushDebris(final int streamNumber, final Floodgate<OUT, OUT> gate,
            final Object debris) {

        return debris;
    }
}