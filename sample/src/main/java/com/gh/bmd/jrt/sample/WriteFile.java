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

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.ByteChannel;
import com.gh.bmd.jrt.core.ByteChannel.BufferInputStream;
import com.gh.bmd.jrt.core.ByteChannel.ByteBuffer;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.invocation.TemplateInvocation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Invocation writing the downloaded data into the output file.
 * <p/>
 * Created by davide-maestroni on 10/17/14.
 */
public class WriteFile extends TemplateInvocation<ByteBuffer, Boolean> {

    private final File mFile;

    private BufferedOutputStream mOutputStream;

    /**
     * Constructor.
     *
     * @param file the output file.
     */
    public WriteFile(@Nonnull final File file) {

        mFile = file;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onAbort(@Nullable final RoutineException reason) {

        closeStream();
        mFile.delete();
    }

    @Override
    public void onInitialize() {

        try {

            mOutputStream = new BufferedOutputStream(new FileOutputStream(mFile));

        } catch (final FileNotFoundException e) {

            throw new InvocationException(e);
        }
    }

    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public void onInput(final ByteBuffer buffer, @Nonnull final ResultChannel<Boolean> result) {

        final BufferInputStream inputStream = ByteChannel.newStream(buffer);

        try {

            while (inputStream.read(mOutputStream) > 0) {

                // Keep looping...
            }

        } catch (final IOException e) {

            throw new InvocationException(e);

        } finally {

            inputStream.close();
        }
    }

    @Override
    public void onResult(@Nonnull final ResultChannel<Boolean> result) {

        closeStream();
        result.pass(true);
    }

    private void closeStream() {

        try {

            mOutputStream.close();

        } catch (final IOException e) {

            throw new InvocationException(e);
        }
    }
}
