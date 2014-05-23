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
package com.bmd.wtf.example2;

import com.bmd.wtf.example1.DownloadUtils;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * {@link DownloadManager} unit test.
 */
public class DownloadManagerTest extends TestCase {

    private static final String FAIL_URL = "http://this.domain.does.not.exist/test.txt";

    private static final String SMALL_FILE_URL1 =
            "http://upload.wikimedia.org/wikipedia/commons/4/4a/Logo_2013_Google.png";

    private static final String SMALL_FILE_URL2 =
            "http://upload.wikimedia.org/wikipedia/commons/2/24/Yahoo%21_logo.svg";

    private static final String SMALL_FILE_URL3 =
            "http://upload.wikimedia.org/wikipedia/commons/b/b1/Bing_logo_%282013%29.svg";

    private final DownloadManager mDownloadManager;

    private final String mTmpDirPath;

    public DownloadManagerTest() throws IOException {

        mTmpDirPath = System.getProperty("java.io.tmpdir");
        mDownloadManager = new DownloadManager(2, new File(mTmpDirPath));
    }

    private static boolean deleteFile(final File file) {

        return file.delete();
    }

    public void testAll() throws IOException {

        final String fileName = DownloadUtils.getFileName(new URL(FAIL_URL));
        final String fileName1 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL1));
        final String fileName2 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL2));
        final String fileName3 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL3));

        final File outFile = new File(mTmpDirPath, fileName);
        deleteFile(outFile);
        final File outFile1 = new File(mTmpDirPath, fileName1);
        deleteFile(outFile1);
        final File outFile2 = new File(mTmpDirPath, fileName2);
        deleteFile(outFile2);
        final File outFile3 = new File(mTmpDirPath, fileName3);
        deleteFile(outFile3);

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

        deleteFile(outFile1);
        deleteFile(outFile2);
        deleteFile(outFile3);
    }

    public void testDownload() throws IOException {

        final String fileName1 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL1));
        final String fileName2 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL2));
        final String fileName3 = DownloadUtils.getFileName(new URL(SMALL_FILE_URL3));

        final File outFile1 = new File(mTmpDirPath, fileName1);
        deleteFile(outFile1);
        final File outFile2 = new File(mTmpDirPath, fileName2);
        deleteFile(outFile2);
        final File outFile3 = new File(mTmpDirPath, fileName3);
        deleteFile(outFile3);

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

        deleteFile(outFile1);
        deleteFile(outFile2);
        deleteFile(outFile3);
    }

    public void testFail() throws IOException {

        final String fileName = DownloadUtils.getFileName(new URL(FAIL_URL));

        final File outFile = new File(mTmpDirPath, fileName);
        deleteFile(outFile);

        assertThat(outFile).doesNotExist();

        mDownloadManager.download(FAIL_URL);

        final long startTime = System.currentTimeMillis();

        try {

            waitFor(FAIL_URL, startTime, 5000);

            fail();

        } catch (final IOException ignored) {

        }

        assertThat(outFile).doesNotExist();
    }

    private void waitFor(final String url, final long startTime, final long timeoutMs) throws
            IOException {

        while (!mDownloadManager.isComplete(url)) {

            try {

                Thread.sleep(100);

            } catch (final InterruptedException ignored) {

            }

            if ((System.currentTimeMillis() - startTime) > timeoutMs) {

                throw new IOException();
            }
        }
    }
}