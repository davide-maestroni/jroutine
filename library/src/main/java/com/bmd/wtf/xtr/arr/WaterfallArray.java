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

import com.bmd.wtf.bdr.Stream;

import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * This class enables the joined handling of an array of parallel {@link com.bmd.wtf.bdr.Stream}s.
 * <p/>
 * Both a single stream can be split in several ones and an array of them can be joined into
 * a single one.
 * <p/>
 * Data flowing through the WaterfallArray are filtered in a parallel way for all the composing
 * streams by employing {@link Barrage}s.
 * <p/>
 * In order to retain behavior consistency, like happens with {@link com.bmd.wtf.dam.Dam}s, one
 * Barrage instance is not allowed more than one time.
 * <p/>
 * Created by davide on 3/3/14.
 *
 * @param <SOURCE> The spring data type.
 * @param <IN>     The input data type of the upstream pool.
 * @param <OUT>    The transported data type, that is the output data type of the upstream pool.
 */
public class WaterfallArray<SOURCE, IN, OUT> {

    private final Stream<SOURCE, IN, OUT> mSourceStream;

    /**
     * Private constructor.
     *
     * @param sourceStream The source stream instance.
     */
    private WaterfallArray(final Stream<SOURCE, IN, OUT> sourceStream) {

        if (sourceStream == null) {

            throw new IllegalArgumentException("the source stream cannot be null");
        }

        mSourceStream = sourceStream;
    }

    /**
     * Creates a new waterfall array starting from the specified stream.
     *
     * @param stream   The source stream.
     * @param <SOURCE> The spring data type.
     * @param <IN>     The input data type of the upstream pool.
     * @param <OUT>    The transported data type, that is the output data type of the upstream
     *                 pool.
     * @return The new waterfall.
     */
    public static <SOURCE, IN, OUT> WaterfallArray<SOURCE, IN, OUT> formingFrom(
            final Stream<SOURCE, IN, OUT> stream) {

        return new WaterfallArray<SOURCE, IN, OUT>(stream);
    }

    /**
     * Creates a new stream array starting from the specified streams.
     *
     * @param streams  The source streams.
     * @param <SOURCE> The spring data type.
     * @param <IN>     The input data type of the upstream pool.
     * @param <OUT>    The transported data type, that is the output data type of the upstream
     *                 pool.
     * @return The new stream array.
     */
    public static <SOURCE, IN, OUT> StreamArray<SOURCE, IN, OUT> formingFrom(
            final Stream<SOURCE, IN, OUT>... streams) {

        if ((streams == null) || (streams.length == 0)) {

            throw new IllegalArgumentException("the starting stream array cannot be null or empty");
        }

        return formingFrom(Arrays.asList(streams));
    }

    /**
     * Creates a new stream array starting from the ones returned by the specified iterable.
     *
     * @param streams  The iterable returning the source streams.
     * @param <SOURCE> The spring data type.
     * @param <IN>     The input data type of the upstream pool.
     * @param <OUT>    The transported data type, that is the output data type of the upstream
     *                 pool.
     * @return The new stream array.
     */
    public static <SOURCE, IN, OUT> StreamArray<SOURCE, IN, OUT> formingFrom(
            final Iterable<? extends Stream<SOURCE, IN, OUT>> streams) {

        final LinkedHashSet<Stream<SOURCE, IN, OUT>> streamList =
                new LinkedHashSet<Stream<SOURCE, IN, OUT>>();

        for (final Stream<SOURCE, IN, OUT> stream : streams) {

            streamList.add(stream);
        }

        return new StreamArray<SOURCE, IN, OUT>(streamList);
    }

    /**
     * Splits this waterfall in the specified number of streams and returns the newly created
     * stream array.
     *
     * @param streamNumber The number of streams to split the waterfall into.
     * @return The new stream array.
     */
    public StreamArray<SOURCE, IN, OUT> thenSplittingIn(final int streamNumber) {

        return new StreamArray<SOURCE, IN, OUT>(mSourceStream, streamNumber);
    }
}