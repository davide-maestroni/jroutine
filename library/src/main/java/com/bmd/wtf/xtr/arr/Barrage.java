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

/**
 * A barrage is a special {@link com.bmd.wtf.dam.Dam} which handles multiple
 * {@link com.bmd.wtf.bdr.Stream}s flowing through it.
 * <p/>
 * A Barrage instance is used in conjunction with a {@link WaterfallArray}
 * to filter a {@link StreamArray} based on a distributed behavior. For example: it can let only
 * the data coming from the first discharging stream flow through it.
 * <p/>
 * Like dams also barrages cannot be used multiple times in a waterfall.
 * <p/>
 * Created by davide on 3/3/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface Barrage<IN, OUT> {

    /**
     * This method is called when a data drop is discharged through the barrage.
     *
     * @param streamNumber The number of the stream in which data is flowing.
     * @param gate         The gate instance to be used to discharge data and objects into the
     *                     waterfall.
     * @param drop         The drop of data discharged.
     */
    public void onDischarge(int streamNumber, Floodgate<IN, OUT> gate, IN drop);

    /**
     * This method is called when an debris is dropped downstream through the barrage.
     *
     * @param streamNumber The number of the stream in which data is flowing.
     * @param gate         The gate instance to be used to discharge data and objects into the
     *                     waterfall.
     * @param debris       The dropped debris.
     */
    public void onDrop(int streamNumber, Floodgate<IN, OUT> gate, Object debris);

    /**
     * This method is called when data are flushed through the barrage.
     *
     * @param streamNumber The number of the stream in which data is flowing.
     * @param gate         The gate instance to be used to discharge data and objects into the
     */
    public void onFlush(int streamNumber, Floodgate<IN, OUT> gate);
}