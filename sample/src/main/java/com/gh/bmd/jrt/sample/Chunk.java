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
package com.gh.bmd.jrt.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public Chunk(final int maxSize, final InputStream stream) throws IOException {

        final byte[] data = new byte[maxSize];
        mLength = stream.read(data);
        mData = data;
    }

    public int getLength() {

        return mLength;
    }

    public void writeTo(final OutputStream stream) throws IOException {

        stream.write(mData, 0, mLength);
    }
}
