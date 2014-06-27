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
package com.bmd.wtf.crr;

import com.bmd.wtf.flw.Fall;

import java.util.concurrent.TimeUnit;

/**
 * Basic component of a waterfall.
 * <p/>
 * A current is responsible for transporting a flow of data feeding a {@link com.bmd.wtf.flw.Fall}.
 * <p/>
 * Its implementation may be synchronous or employ one or more separate threads. For this reason
 * it should always be thread safe.
 * <p/>
 * Created by davide on 6/7/14.
 */
public interface Current {

    public void discharge(Fall<?> fall);

    public void forward(Fall<?> fall, Throwable throwable);

    public <DATA> void push(Fall<DATA> fall, DATA drop);

    public <DATA> void pushAfter(Fall<DATA> fall, long delay, TimeUnit timeUnit, DATA drop);

    public <DATA> void pushAfter(Fall<DATA> fall, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);
}