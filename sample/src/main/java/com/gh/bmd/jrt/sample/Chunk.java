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
package com.gh.bmd.jrt.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Chunk of bytes.
 * <p/>
 * Created by davide on 10/17/14.
 */
public class Chunk {

    private final byte[] mData;

    private int mLength;

    public Chunk(final int maxSize) {

        mData = new byte[maxSize];
    }

    public boolean readFrom(final InputStream stream) throws IOException {

        mLength = stream.read(mData);
        return (mLength >= 0);
    }

    public void writeTo(final OutputStream stream) throws IOException {

        stream.write(mData, 0, mLength);
    }
}
