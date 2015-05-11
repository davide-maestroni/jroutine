/*
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
package com.gh.bmd.jrt.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

/**
 * Chunk of bytes.<br/>
 * The objects are immutable since the internal data are read in the constructor and then written to
 * an output stream.
 * <p/>
 * Created by davide on 10/17/14.
 */
public class Chunk {

    private final byte[] mData;

    private int mLength;

    /**
     * Constructor.
     *
     * @param maxSize the maximum number of bytes in the chunk.
     * @param stream  the input stream from which to read the data.
     * @throws IOException if an I/O error occurred.
     */
    public Chunk(final int maxSize, @Nonnull final InputStream stream) throws IOException {

        final byte[] data = new byte[maxSize];
        mLength = stream.read(data);
        mData = data;
    }

    /**
     * Gets the number of meaningful bytes in this chunk.
     *
     * @return the data length.
     */
    public int getLength() {

        return mLength;
    }

    /**
     * Writes the bytes of this chunk into the specified output stream.
     *
     * @param stream the output stream.
     * @throws IOException if an I/O error occurred.
     */
    public void writeTo(@Nonnull final OutputStream stream) throws IOException {

        stream.write(mData, 0, mLength);
    }
}
