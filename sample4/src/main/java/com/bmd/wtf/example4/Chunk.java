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
package com.bmd.wtf.example4;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class storing a chunk of downloaded data.
 */
public class Chunk {

    private final byte[] mBuffer;

    private final long mTotal;

    private final String mUrl;

    private int mRead;

    public Chunk(final String url, final int maxSize, final long totalSize) {

        mUrl = url;
        mBuffer = new byte[maxSize];
        mTotal = totalSize;
    }

    public String getUrl() {

        return mUrl;
    }

    public boolean isComplete() {

        return (mRead == -1);
    }

    public void readFrom(final InputStream inputStream) throws IOException {

        mRead = inputStream.read(mBuffer);
    }

    public int size() {

        return mRead;
    }

    public long totalBytes() {

        return mTotal;
    }

    public void writeTo(final OutputStream outputStream) throws IOException {

        outputStream.write(mBuffer, 0, mRead);
    }
}