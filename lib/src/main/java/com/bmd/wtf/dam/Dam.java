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
package com.bmd.wtf.dam;

import com.bmd.wtf.src.Floodgate;

/**
 * Basic component of a {@link com.bmd.wtf.Waterfall}.
 * <p/>
 * A <code>Dam</code> is responsible for transforming and filtering data and objects through the
 * provided {@link com.bmd.wtf.src.Floodgate} instance, and is ensured to be unique inside the
 * <code>Waterfall</code>.<br/>
 * The reason behind that is to try to prevent unsafe use of the same instances across different
 * threads. Each <code>Dam</code> should only retain references to its internal state and
 * communicate to other instances exclusively through the methods provided by <code>Floodgate</code>
 * objects.
 * <p/>
 * Note that the <code>Floodgate</code> is only accessible inside the callback methods. Any later use,
 * especially inside a different thread, will raise an
 * {@link com.bmd.wtf.bdr.UnauthorizedDischargeException}.
 * <p/>
 * Created by davide on 2/25/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface Dam<IN, OUT> {

    /**
     * This method is called when a data drop is discharged through the dam.
     *
     * @param gate The gate instance to be used to discharge data into the waterfall.
     * @param drop The drop of data discharged.
     * @return The debris to push downstream and pull upstream, or <code>null</code>.
     */
    public Object onDischarge(Floodgate<IN, OUT> gate, IN drop);

    /**
     * This method is called when data are flushed through the dam.
     *
     * @param gate The gate instance to be used to discharge data into the waterfall.
     * @return The debris to push downstream and pull upstream, or <code>null</code>.
     */
    public Object onFlush(Floodgate<IN, OUT> gate);

    /**
     * This method is called when an debris is pulled upstream through the dam.
     *
     * @param gate   The gate instance to be used to discharge data into the waterfall.
     * @param debris The pulled debris.
     * @return The debris to pull further upstream, or <code>null</code>.
     */
    public Object onPullDebris(Floodgate<IN, OUT> gate, Object debris);

    /**
     * This method is called when an object is pushed downstream through the dam.
     *
     * @param gate   The gate instance to be used to discharge data into the waterfall.
     * @param debris The pushed debris.
     * @return The debris to push further downstream, or <code>null</code>.
     */
    public Object onPushDebris(Floodgate<IN, OUT> gate, Object debris);
}