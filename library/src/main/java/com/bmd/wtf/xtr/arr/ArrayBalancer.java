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

/**
 * An array balancer is used to sort the data flow in a stream array.
 * <p/>
 * The index returned by the balancer indicates into which stream to propagate the flow of data.
 * <br/>In case -1 is returned, no further propagation will happen. While, if the total stream
 * count is returned instead, the data or object will be propagated to all the streams in the
 * array. All the other out-of-range values will be ignored.
 * <p/>
 * Created by davide on 5/15/14.
 */
public interface ArrayBalancer<DATA> {

    /**
     * Chooses the index of the stream into which to discharge the specified data drop.
     * <br/><code>-1</code> means no stream and <code>streamCount</code> means all the streams.
     *
     * @param drop        The data drop to discharge.
     * @param streamCount The total count of streams in the array.
     * @return The index of the target stream.
     */
    public int chooseDataStream(DATA drop, int streamCount);

    /**
     * Chooses the index of the stream into which to propagate the flush.
     * <br/><code>-1</code> means no stream and <code>streamCount</code> means all the streams.
     *
     * @param streamCount The total count of streams in the array.
     * @return The index of the target stream.
     */
    public int chooseFlushStream(int streamCount);

    /**
     * Chooses if to pull the specified debris further upstream.
     *
     * @param debris       The debris to pull.
     * @param streamNumber The number of the stream in which data is flowing.
     * @return Whether to pull the debris upstream.
     */
    public boolean choosePulledDebrisStream(Object debris, int streamNumber);

    /**
     * Chooses if to push the specified debris further downstream.
     *
     * @param debris       The debris to push.
     * @param streamNumber The number of the stream in which data is flowing.
     * @return Whether to push the debris downstream.
     */
    public boolean choosePushedDebrisStream(Object debris, int streamNumber);
}