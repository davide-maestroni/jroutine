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
package com.bmd.wtf.xtr.qdc;

import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.util.List;

/**
 * Base abstract implementation of {@link Archway}.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public abstract class AbstractArchway<IN, OUT> implements Archway<IN, OUT> {

    @Override
    public void onDrop(final Floodgate<IN, OUT> gate, final List<Spring<OUT>> springs,
            final Object debris) {

        for (final Spring<OUT> spring : springs) {

            spring.drop(debris);
        }
    }

    @Override
    public void onFlush(final Floodgate<IN, OUT> gate, final List<Spring<OUT>> springs) {

        for (final Spring<OUT> spring : springs) {

            spring.flush();
        }
    }
}