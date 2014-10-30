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
package com.bmd.jrt.channel;

import javax.annotation.Nonnull;

/**
 * Interface defining an input/output channel.
 * <p/>
 * An I/O channel is useful to make other asynchronous tasks communicate with a routine.<br/>
 * The channel output can be passed to a routine input channel in order to feed it with data coming
 * asynchronously from another source. Note however, that in both cases the
 * <b><code>close()</code></b> method must be called to correctly terminate the invocation
 * lifecycle.
 * <p/>
 * Created by davide on 10/25/14.
 *
 * @param <TYPE> the data type.
 */
public interface IOChannel<TYPE> {

    /**
     * Closes this channel.
     * <p/>
     * Note that this method must be always called when done with the channel.
     */
    public void close();

    /**
     * Returns the input end of this channel.
     *
     * @return the input channel.
     */
    @Nonnull
    public InputChannel<TYPE> input();

    /**
     * Returns the output end of this channel.
     *
     * @return the output channel.
     */
    @Nonnull
    public OutputChannel<TYPE> output();
}