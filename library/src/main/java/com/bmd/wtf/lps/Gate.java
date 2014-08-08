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
 * A gate is responsible for transforming and filtering data and exceptions flowing through the
 * provided river instances.
 * <br/>
 * A gate is ensured to be unique inside the waterfall.<br/>
 * The reason behind that, is to try to prevent unsafe use of the same instance across different
 * threads. Each gate should only retain references to its internal state and communicate with
 * other instances exclusively through the methods provided by the river objects.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface Gate<IN, OUT> {

    /**
     * This method is called when data are flushed through the gate.
     *
     * @param upRiver    the upstream river instance.
     * @param downRiver  the downstream river instance.
     * @param fallNumber the number identifying the fall formed by this gate.
     */
    public void onFlush(River<IN> upRiver, River<OUT> downRiver, int fallNumber);

    /**
     * This method is called when a data drop is pushed through the gate.
     *
     * @param upRiver    the upstream river instance.
     * @param downRiver  the downstream river instance.
     * @param fallNumber the number identifying the fall formed by this gate.
     * @param drop       the drop of data.
     */
    public void onPush(River<IN> upRiver, River<OUT> downRiver, int fallNumber, IN drop);

    /**
     * This method is called when an unhandled exception is pushed downstream through the gate.
     *
     * @param upRiver    the upstream river instance.
     * @param downRiver  the downstream river instance.
     * @param fallNumber the number identifying the fall formed by this gate.
     * @param throwable  the unhandled exception.
     */
    public void onUnhandled(River<IN> upRiver, River<OUT> downRiver, int fallNumber,
            Throwable throwable);
}