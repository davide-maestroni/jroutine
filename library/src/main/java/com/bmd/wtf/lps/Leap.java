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
package com.bmd.wtf.lps;

import com.bmd.wtf.flw.River;

/**
 * Basic component of a waterfall.
 * <p/>
 * A leap is responsible for transforming and filtering data and exceptions flowing through the
 * provided river instances.
 * <br/>
 * A leap is ensured to be unique inside the waterfall.<br/>
 * The reason behind that, is to try to prevent unsafe use of the same instance across different
 * threads. Each leap should only retain references to its internal state and communicate with
 * other instances exclusively through the methods provided by the river objects.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <SOURCE> The river source data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 */
public interface Leap<SOURCE, IN, OUT> {

    /**
     * This method is called when data are discharged through the leap.
     *
     * @param upRiver    The upstream river instance.
     * @param downRiver  The downstream river instance.
     * @param fallNumber The number identifying the fall formed by this leap.
     */
    public void onDischarge(River<SOURCE, IN> upRiver, River<SOURCE, OUT> downRiver,
            int fallNumber);

    /**
     * This method is called when a data drop is pushed through the leap.
     *
     * @param upRiver    The upstream river instance.
     * @param downRiver  The downstream river instance.
     * @param fallNumber The number identifying the fall formed by this leap.
     * @param drop       The drop of data.
     */
    public void onPush(River<SOURCE, IN> upRiver, River<SOURCE, OUT> downRiver, int fallNumber,
            IN drop);

    /**
     * This method is called when an unhandled exception is pushed downstream through the leap.
     *
     * @param upRiver    The upstream river instance.
     * @param downRiver  The downstream river instance.
     * @param fallNumber The number identifying the fall formed by this leap.
     * @param throwable  The unhandled exception.
     */
    public void onUnhandled(River<SOURCE, IN> upRiver, River<SOURCE, OUT> downRiver, int fallNumber,
            Throwable throwable);
}