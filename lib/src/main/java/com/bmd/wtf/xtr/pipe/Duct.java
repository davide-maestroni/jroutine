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
 * A duct operates similarly to a {@link com.bmd.wtf.dam.Dam} with the fundamental difference of
 * being a one way flow of data. In fact, since the asynchronous mean is other than the
 * {@link com.bmd.wtf.Waterfall} one, data can flow in it only downstream and not in the opposite
 * direction.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface Duct<IN, OUT> {

    /**
     * This method is called when a data drop is discharged through the duct.
     *
     * @param spring The spring instance to be used to discharge data and objects into the
     *               pipeline.
     * @param drop   The drop of data discharged.
     * @return The debris to push downstream and pull upstream, or <code>null</code>.
     */
    public Object onDischarge(Spring<OUT> spring, IN drop);

    /**
     * This method is called when data are flushed through the duct.
     *
     * @param spring The spring instance to be used to discharge data and objects into the
     *               pipeline.
     * @return The debris to push downstream and pull upstream, or <code>null</code>.
     */
    public Object onFlush(Spring<OUT> spring);

    /**
     * This method is called when an debris is pulled upstream through the duct.
     *
     * @param spring The spring instance to be used to discharge data and objects into the
     *               pipeline.
     * @param debris The pulled debris.
     * @return The debris to pull further or <code>null</code> if nothing more has to be pulled
     * upstream.
     */
    public Object onPullDebris(Spring<OUT> spring, Object debris);

    /**
     * This method is called when an debris is pushed downstream through the duct.
     *
     * @param spring The spring instance to be used to discharge data and objects into the
     *               pipeline.
     * @param debris The pushed debris.
     * @return The debris to push further or <code>null</code> if nothing more has to be pushed
     * downstream.
     */
    public Object onPushDebris(Spring<OUT> spring, Object debris);
}