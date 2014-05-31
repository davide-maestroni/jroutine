package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.bdr.Stream;

/**
 * A {@link com.bmd.wtf.bdr.Stream} factory used to provide instances for an array of streams.
 * <p/>
 * Created by davide on 3/4/14.
 *
 * @param <SOURCE> The spring data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 * @param <NIN>    The returned stream input data type.
 * @param <NOUT>   The returned stream output data type.
 */
public interface StreamFactory<SOURCE, IN, OUT, NIN, NOUT> {

    /**
     * Creates the output stream associated to the specified input.
     *
     * @param stream       The input stream.
     * @param streamNumber The number of the stream.
     * @return The associated stream.
     */
    public Stream<SOURCE, NIN, NOUT> createFrom(Stream<SOURCE, IN, OUT> stream, int streamNumber);
}