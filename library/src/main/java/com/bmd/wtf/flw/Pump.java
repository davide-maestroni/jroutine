/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHDATA WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.flw;

/**
 * A pump manages the way in which the data drops are distributed among a number of streams.
 * <p/>
 * Created by davide on 7/29/14.
 *
 * @param <DATA> the data type.
 */
public interface Pump<DATA> {

    /**
     * Indicates that the data drop must be pushed to all the downstream streams.
     */
    public static final int ALL_STREAMS = -1;

    /**
     * Indicates that the data drop must be pushed to the less busy downstream stream.
     */
    public static final int DEFAULT_STREAM = -2;

    /**
     * Indicates that the data drop must not be pushed downstream.
     */
    public static final int NO_STREAM = -3;

    /**
     * This method is called when a data drop is pushed through the pump.
     *
     * @param drop the drop of data.
     * @return the stream number to push the drop into or one of the constants defining the default
     * stream (that is, the one processing less data), all the streams or none of them.
     */
    public int onPush(DATA drop);
}