package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.dam.Dam;

/**
 * A {@link com.bmd.wtf.dam.Dam} factory used to provide instances for an array of streams.
 * <p/>
 * Created by davide on 3/4/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface DamFactory<IN, OUT> {

    /**
     * Creates the dam associated to the specified stream number.
     *
     * @param streamNumber The number of the stream.
     * @return The associated dam.
     */
    public Dam<IN, OUT> createForStream(int streamNumber);
}