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

import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.src.Floodgate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Class responsible for the writing of downloaded data into an output file.
 */
public class OutputHandler extends AbstractDam<Chunk, String> {

    private final File mDir;

    private File mOutputFile;

    private FileOutputStream mOutputStream;

    public OutputHandler(final File downloadDir) {

        mDir = downloadDir;
    }

    @Override
    public Object onDischarge(final Floodgate<Chunk, String> gate, final Chunk drop) {

        if (mOutputStream == null) {

            try {

                setupOutput(new URL(drop.getUrl()));

            } catch (final IOException e) {

                return e;
            }
        }

        if (drop.isComplete()) {

            resetOutput(false);

            gate.discharge(drop.getUrl());

            // Return true if everything worked as expected

            return true;
        }

        try {

            drop.writeTo(mOutputStream);

        } catch (final IOException e) {

            resetOutput(true);

            return e;
        }

        return null;
    }

    @Override
    public Object onPullDebris(final Floodgate<Chunk, String> gate, final Object debris) {

        resetOutput(true);

        return super.onPullDebris(gate, debris);
    }

    @Override
    public Object onPushDebris(final Floodgate<Chunk, String> gate, final Object debris) {

        resetOutput(true);

        return super.onPushDebris(gate, debris);
    }

    private void resetOutput(final boolean deleteFile) {

        DownloadUtils.safeClose(mOutputStream);

        mOutputStream = null;

        if (deleteFile && (mOutputFile != null)) {

            mOutputFile.delete();

            mOutputFile = null;
        }
    }

    private void setupOutput(final URL url) throws IOException {

        mOutputStream = null;

        final String path = url.getPath();

        String fileName = path.substring(path.lastIndexOf('/') + 1);

        if (fileName.equals("")) {

            fileName = Long.toString(System.nanoTime()) + ".tmp";
        }

        final File outFile = new File(mDir, fileName);
        outFile.deleteOnExit();

        mOutputStream = new FileOutputStream(outFile);
        mOutputFile = outFile;
    }
}