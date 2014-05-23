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
 * An array balancer is an object used to sort the data drops flowing in a stream array.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface ArrayBalancer<IN, OUT> {

    /**
     * This method is called when a data drop is discharged through the balancer.
     *
     * @param springs The list of output springs.
     * @param drop    The data drop to discharge.
     * @return The debris to push downstream and pull upstream, or <code>null</code>.
     */
    public Object onDischarge(List<Spring<OUT>> springs, IN drop);

    /**
     * This method is called when data are flushed through the balancer.
     *
     * @param springs The list of output springs.
     * @return The debris to push downstream and pull upstream, or <code>null</code>.
     */
    public Object onFlush(List<Spring<OUT>> springs);

    /**
     * This method is called when an debris is pulled upstream through the balancer.
     *
     * @param streamNumber The number of the stream in which data is flowing.
     * @param gate         The gate instance to be used to discharge data into the waterfall.
     * @param debris       The pulled debris.
     * @return The debris to pull further upstream, or <code>null</code>.
     */
    public Object onPullDebris(int streamNumber, Floodgate<OUT, OUT> gate, Object debris);

    /**
     * This method is called when an object is pushed downstream through the dam.
     *
     * @param streamNumber The number of the stream in which data is flowing.
     * @param gate         The gate instance to be used to discharge data into the waterfall.
     * @param debris       The pushed debris.
     * @return The debris to push further downstream, or <code>null</code>.
     */
    public Object onPushDebris(int streamNumber, Floodgate<OUT, OUT> gate, Object debris);
}