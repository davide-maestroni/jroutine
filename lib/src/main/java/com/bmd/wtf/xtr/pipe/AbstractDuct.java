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
package com.bmd.wtf.xtr.pipe;

import com.bmd.wtf.src.Spring;

/**
 * Base abstract implementation of {@link Duct}.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public abstract class AbstractDuct<IN, OUT> implements Duct<IN, OUT> {

    @Override
    public Object onFlush(final Spring<OUT> spring) {

        spring.flush();

        return null;
    }

    @Override
    public Object onPullDebris(final Spring<OUT> spring, final Object debris) {

        return debris;
    }

    @Override
    public Object onPushDebris(final Spring<OUT> spring, final Object debris) {

        return debris;
    }
}