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
 * An archway operates similarly to a {@link com.bmd.wtf.dam.Dam} with the fundamental difference
 * of employing a set of springs to propagate the flow of data further downstream.<br/>
 * It can be used to sort the data flowing into the different streams running at the different
 * levels of the archway.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface Archway<IN, OUT> {

    /**
     * This method is called when a data drop is discharged through the archway.
     * <p/>
     * Note that the floodgate can be used only to recharge data since downstream the flow is
     * interrupted. The provided spring must be used instead to discharge data.
     *
     * @param gate    The gate instance to be used to recharge data into the waterfall.
     * @param springs The list of spring instances to be used to discharge data and debris into
     *                the waterfall.
     * @param drop    The drop of data discharged.
     */
    public void onDischarge(Floodgate<IN, OUT> gate, List<Spring<OUT>> springs, IN drop);

    /**
     * This method is called when an debris is dropped downstream through the archway.
     * <p/>
     * Note that the floodgate can be used only to recharge data since downstream the flow is
     * interrupted. The provided spring must be used instead to discharge data.
     *
     * @param gate    The gate instance to be used to recharge data into the waterfall.
     * @param springs The list of spring instances to be used to discharge data and debris into
     *                the waterfall.
     * @param debris  The dropped debris.
     */
    public void onDrop(Floodgate<IN, OUT> gate, List<Spring<OUT>> springs, Object debris);

    /**
     * This method is called when data are flushed through the archway.
     * <p/>
     * Note that the floodgate can be used only to recharge data since downstream the flow is
     * interrupted. The provided spring must be used instead to discharge data.
     *
     * @param gate    The gate instance to be used to recharge data into the waterfall.
     * @param springs The list of spring instances to be used to discharge data and debris into
     *                the waterfall.
     */
    public void onFlush(Floodgate<IN, OUT> gate, List<Spring<OUT>> springs);
}