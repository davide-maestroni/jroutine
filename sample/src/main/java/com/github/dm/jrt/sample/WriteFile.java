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

import com.github.dm.jrt.channel.io.ByteChannel;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Invocation writing the downloaded data into the output file.
 * <p>
 * Created by davide-maestroni on 10/17/2014.
 */
@SuppressWarnings("WeakerAccess")
public class WriteFile extends TemplateInvocation<ByteChunk, Boolean> {

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
  public void onAbort(@NotNull final RoutineException reason) throws IOException {
    closeStream();
    mFile.delete();
  }

  @Override
  public void onComplete(@NotNull final Channel<Boolean, ?> result) throws IOException {
    closeStream();
    result.pass(true);
  }

  @Override
  public void onInput(final ByteChunk chunk, @NotNull final Channel<Boolean, ?> result) throws
      IOException {
    ByteChannel.getInputStream(chunk).transferTo(mOutputStream);
  }

  @Override
  public void onRestart() throws FileNotFoundException {
    mOutputStream = new BufferedOutputStream(new FileOutputStream(mFile));
  }

  private void closeStream() throws IOException {
    mOutputStream.close();
  }
}
