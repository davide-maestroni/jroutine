/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.channel;

import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneInput;
import com.gh.bmd.jrt.invocation.Invocations.Function1;
import com.gh.bmd.jrt.routine.JRoutine;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for handling channels.
 * <p/>
 * Created by davide on 3/15/15.
 */
public class Channels {

    /**
     * Avoid direct instantiation.
     */
    protected Channels() {

    }

    /**
     * Merges the specified channels into a selectable one.
     * <p/>
     * Note that the channels will be bound as a result of the call.
     *
     * @param channels the channels to merge.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     * @throws java.lang.IllegalArgumentException if the specified list is empty.
     * @throws java.lang.NullPointerException     if the specified list is null.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<Selectable<OUTPUT>> select(
            @Nonnull final List<? extends OutputChannel<? extends OUTPUT>> channels) {

        if (channels.isEmpty()) {

            throw new IllegalArgumentException("the list of channels cannot be empty");
        }

        final StandaloneChannel<Selectable<OUTPUT>> standalone =
                JRoutine.standalone().buildChannel();
        final StandaloneInput<Selectable<OUTPUT>> input = standalone.input();
        int i = 0;

        for (final OutputChannel<? extends OUTPUT> channel : channels) {

            input.pass(selectable(i++, channel));
        }

        input.close();
        return standalone.output();
    }

    /**
     * Returns a new channel making the specified one selectable.<br/>
     * Each output will be passed along unchanged.
     * <p/>
     * Note that the channel will be bound as a result of the call.
     *
     * @param index    the channel index.
     * @param channel  the channel to make selectable.
     * @param <OUTPUT> the output data type.
     * @return the selectable output channel.
     */
    @Nonnull
    public static <OUTPUT> OutputChannel<Selectable<OUTPUT>> selectable(final int index,
            @Nullable final OutputChannel<? extends OUTPUT> channel) {

        return JRoutine.onFunction(new Function1<OUTPUT, Selectable<OUTPUT>>() {

            @Override
            public Selectable<OUTPUT> call(final OUTPUT param1) {

                return new Selectable<OUTPUT>(param1, channel, index);
            }
        }).callSync(channel);
    }

    /**
     * Record class storing information about the origin of the data.
     *
     * @param <DATA> the data type.
     */
    public static class Selectable<DATA> {

        /**
         * The origin channel.
         */
        public final Channel channel;

        /**
         * The data object.
         */
        public final DATA data;

        /**
         * The origin channel index.
         */
        public final int index;

        /**
         * Constructor.
         *
         * @param data    the data object.
         * @param channel the origin channel.
         * @param index   the channel index.
         */
        public Selectable(final DATA data, final Channel channel, final int index) {

            this.data = data;
            this.channel = channel;
            this.index = index;
        }
    }
}
