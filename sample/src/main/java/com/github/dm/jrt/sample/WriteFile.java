/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.sample;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.common.RoutineException;
import com.github.dm.jrt.core.ByteChannel;
import com.github.dm.jrt.core.ByteChannel.BufferInputStream;
import com.github.dm.jrt.core.ByteChannel.ByteBuffer;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Invocation writing the downloaded data into the output file.
 * <p/>
 * Created by davide-maestroni on 10/17/2014.
 */
public class WriteFile extends TemplateInvocation<ByteBuffer, Boolean> {

    private final File mFile;

    private BufferedOutputStream mOutputStream;

    /**
     * Constructor.
     *
     * @param file the output file.
     */
    public WriteFile(@NotNull final File file) {

        mFile = file;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onAbort(@NotNull final RoutineException reason) {

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
    public void onInput(final ByteBuffer buffer, @NotNull final ResultChannel<Boolean> result) {

        final BufferInputStream inputStream = ByteChannel.inputStream(buffer);
        final BufferedOutputStream outputStream = mOutputStream;
        try {
            inputStream.readAll(outputStream);

        } catch (final IOException e) {
            throw new InvocationException(e);

        } finally {
            inputStream.close();
        }
    }

    @Override
    public void onResult(@NotNull final ResultChannel<Boolean> result) {

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
