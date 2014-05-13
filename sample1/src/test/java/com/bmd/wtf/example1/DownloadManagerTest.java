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
package com.bmd.wtf.example1;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.fest.assertions.api.Assertions.assertThat;

public class DownloadManagerTest extends TestCase {

    private static final String FAIL_URL = "http://this.domain.does.not.exist/test.txt";

    private static final String SMALL_FILE_URL1 =
            "http://upload.wikimedia.org/wikipedia/commons/4/4a/Logo_2013_Google.png";

    private static final String SMALL_FILE_URL2 =
            "http://upload.wikimedia.org/wikipedia/commons/2/24/Yahoo%21_logo.svg";

    private static final String SMALL_FILE_URL3 =
            "http://upload.wikimedia.org/wikipedia/commons/b/b1/Bing_logo_%282013%29.svg";

    private DownloadManager mDownloadManager;

    private String mTmpDirPath;

    public void testAll() throws IOException {

        final String fileName = DownloadUtils.getFileName(new URL(FAIL_URL));
        final String fileName1 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL1));
        final String fileName2 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL2));
        final String fileName3 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL3));

        final File outFile = new File(mTmpDirPath, fileName);
        outFile.delete();
        final File outFile1 = new File(mTmpDirPath, fileName1);
        outFile1.delete();
        final File outFile2 = new File(mTmpDirPath, fileName2);
        outFile2.delete();
        final File outFile3 = new File(mTmpDirPath, fileName3);
        outFile3.delete();

        assertThat(outFile).doesNotExist();
        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        mDownloadManager.download(SMALL_FILE_URL3);
        mDownloadManager.download(FAIL_URL);
        mDownloadManager.download(SMALL_FILE_URL1);
        mDownloadManager.download(SMALL_FILE_URL2);

        final long startTime = System.currentTimeMillis();

        waitFor(SMALL_FILE_URL1, startTime, 30000);
        waitFor(SMALL_FILE_URL2, startTime, 30000);
        waitFor(SMALL_FILE_URL3, startTime, 30000);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
        assertThat(outFile).doesNotExist();

        outFile1.delete();
        outFile2.delete();
        outFile3.delete();
    }

    public void testDownload() throws IOException {

        final String fileName1 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL1));
        final String fileName2 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL2));
        final String fileName3 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL3));

        final File outFile1 = new File(mTmpDirPath, fileName1);
        outFile1.delete();
        final File outFile2 = new File(mTmpDirPath, fileName2);
        outFile2.delete();
        final File outFile3 = new File(mTmpDirPath, fileName3);
        outFile3.delete();

        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        mDownloadManager.download(SMALL_FILE_URL1);
        mDownloadManager.download(SMALL_FILE_URL2);
        mDownloadManager.download(SMALL_FILE_URL3);

        final long startTime = System.currentTimeMillis();

        waitFor(SMALL_FILE_URL1, startTime, 30000);
        waitFor(SMALL_FILE_URL2, startTime, 30000);
        waitFor(SMALL_FILE_URL3, startTime, 30000);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();

        outFile1.delete();
        outFile2.delete();
        outFile3.delete();
    }

    public void testFail() throws IOException {

        final String fileName = DownloadUtils.getFileName(new URL(FAIL_URL));

        final File outFile = new File(mTmpDirPath, fileName);
        outFile.delete();

        assertThat(outFile).doesNotExist();

        mDownloadManager.download(FAIL_URL);

        final long startTime = System.currentTimeMillis();

        try {

            waitFor(FAIL_URL, startTime, 5000);

            fail();

        } catch (final IOException e) {

            // Ignore it
        }

        assertThat(outFile).doesNotExist();
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();

        mTmpDirPath = System.getProperty("java.io.tmpdir");

        mDownloadManager = new DownloadManager(2, new File(mTmpDirPath));
    }

    private void waitFor(final String url, final long startTime, final long timeoutMs) throws
            IOException {

        while (!mDownloadManager.isComplete(url)) {

            try {

                Thread.sleep(100);

            } catch (final InterruptedException e) {

                // Ignore it
            }

            if ((System.currentTimeMillis() - startTime) > timeoutMs) {

                throw new IOException();
            }
        }
    }
}