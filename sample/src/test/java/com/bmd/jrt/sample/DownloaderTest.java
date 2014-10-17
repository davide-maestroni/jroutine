package com.bmd.jrt.sample;

import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Downloader unit tests.
 * <p/>
 * Created by davide on 10/17/14.
 */
public class DownloaderTest extends TestCase {

    private static final String FAIL_URL = "http://this.domain.does.not.exist/test.txt";

    private static final String HUGE_FILE_URL =
            "http://dl.google.com/android/studio/install/0.4.6/android-studio-bundle-133"
                    + ".1028713-linux.tgz";

    private static final String SMALL_FILE_URL1 =
            "http://upload.wikimedia.org/wikipedia/commons/4/4a/Logo_2013_Google.png";

    private static final String SMALL_FILE_URL2 =
            "http://upload.wikimedia.org/wikipedia/commons/2/24/Yahoo%21_logo.svg";

    private static final String SMALL_FILE_URL3 =
            "http://upload.wikimedia.org/wikipedia/commons/b/b1/Bing_logo_%282013%29.svg";

    private final Downloader mDownloader;

    private final String mTmpDirPath;

    public DownloaderTest() throws IOException {

        mTmpDirPath = System.getProperty("java.io.tmpdir");
        mDownloader = new Downloader();
    }

    private static String getFileName(final URI uri) {

        final String path = uri.getPath();

        final String fileName = path.substring(path.lastIndexOf('/') + 1);

        if (fileName.equals("")) {

            return Long.toString(path.hashCode()) + ".tmp";
        }

        return fileName;
    }

    public void testAll() throws IOException, URISyntaxException {

        final URI uri = new URI(FAIL_URL);
        final URI uriH = new URI(HUGE_FILE_URL);
        final URI uri1 = new URI(SMALL_FILE_URL1);
        final URI uri2 = new URI(SMALL_FILE_URL2);
        final URI uri3 = new URI(SMALL_FILE_URL3);

        final String fileName = getFileName(uri);
        final String fileNameH = getFileName(uriH);
        final String fileName1 = getFileName(uri1);
        final String fileName2 = getFileName(uri2);
        final String fileName3 = getFileName(uri3);

        final File outFile = new File(mTmpDirPath, fileName);
        final File outFileH = new File(mTmpDirPath, fileNameH);
        final File outFile1 = new File(mTmpDirPath, fileName1);
        final File outFile2 = new File(mTmpDirPath, fileName2);
        final File outFile3 = new File(mTmpDirPath, fileName3);

        assertThat(outFile).doesNotExist();
        assertThat(outFileH).doesNotExist();
        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        mDownloader.download(uri, outFile);
        mDownloader.download(uri3, outFile3);
        mDownloader.download(uriH, outFileH);
        mDownloader.download(uri1, outFile1);
        mDownloader.download(uri2, outFile2);

        mDownloader.abort(uriH);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 30000);
        waitFor(uriH, startTime, 30000);
        waitFor(uri1, startTime, 30000);
        waitFor(uri2, startTime, 30000);
        waitFor(uri3, startTime, 30000);

        assertThat(mDownloader.isDownloaded(uri)).isFalse();
        assertThat(mDownloader.isDownloaded(uriH)).isFalse();
        assertThat(mDownloader.isDownloaded(uri1)).isTrue();
        assertThat(mDownloader.isDownloaded(uri2)).isTrue();
        assertThat(mDownloader.isDownloaded(uri3)).isTrue();

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
        assertThat(outFile).doesNotExist();

        mDownloader.abort(uri1);
        mDownloader.abort(uri2);
        mDownloader.abort(uri3);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
    }

    public void testDownload() throws IOException, URISyntaxException {

        final URI uri1 = new URI(SMALL_FILE_URL1);
        final URI uri2 = new URI(SMALL_FILE_URL2);
        final URI uri3 = new URI(SMALL_FILE_URL3);

        final String fileName1 = getFileName(uri1);
        final String fileName2 = getFileName(uri2);
        final String fileName3 = getFileName(uri3);

        final File outFile1 = new File(mTmpDirPath, fileName1);
        final File outFile2 = new File(mTmpDirPath, fileName2);
        final File outFile3 = new File(mTmpDirPath, fileName3);

        mDownloader.abort(uri1);
        mDownloader.abort(uri2);
        mDownloader.abort(uri3);

        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        mDownloader.download(uri1, outFile1);
        mDownloader.download(uri2, outFile2);
        mDownloader.download(uri3, outFile3);

        final long startTime = System.currentTimeMillis();

        waitFor(uri1, startTime, 30000);
        waitFor(uri2, startTime, 30000);
        waitFor(uri3, startTime, 30000);

        assertThat(mDownloader.isDownloaded(uri1)).isTrue();
        assertThat(mDownloader.isDownloaded(uri2)).isTrue();
        assertThat(mDownloader.isDownloaded(uri3)).isTrue();

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();

        mDownloader.abort(uri1);
        mDownloader.abort(uri2);
        mDownloader.abort(uri3);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
    }

    public void testFail() throws IOException, URISyntaxException {

        final URI uri = new URI(FAIL_URL);

        final String fileName = getFileName(uri);

        final File outFile = new File(mTmpDirPath, fileName);

        assertThat(outFile).doesNotExist();

        mDownloader.download(uri, outFile);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 5000);

        assertThat(mDownloader.isDownloaded(uri)).isFalse();

        assertThat(outFile).doesNotExist();
    }

    public void testRepeatedAbort() throws IOException, URISyntaxException {

        final URI uri = new URI(HUGE_FILE_URL);

        final String fileName = getFileName(uri);

        final File outFile = new File(mTmpDirPath, fileName);

        mDownloader.abort(uri);

        assertThat(outFile).doesNotExist();

        for (int i = 0; i < 10; i++) {

            mDownloader.download(uri, outFile);
            mDownloader.abort(uri);
        }

        mDownloader.abort(uri);
        mDownloader.download(uri, outFile);
        mDownloader.abort(uri);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 20000);

        assertThat(mDownloader.isDownloaded(uri)).isFalse();

        assertThat(outFile).doesNotExist();
    }

    public void testSimpleAbort() throws IOException, URISyntaxException {

        final URI uri = new URI(HUGE_FILE_URL);

        final String fileName = getFileName(uri);

        final File outFile = new File(mTmpDirPath, fileName);

        mDownloader.abort(uri);

        assertThat(outFile).doesNotExist();

        mDownloader.download(uri, outFile);

        final long startTime = System.currentTimeMillis();

        while (!outFile.exists()) {

            try {

                Thread.sleep(100);

            } catch (final InterruptedException ignored) {

            }

            if ((System.currentTimeMillis() - startTime) > 20000) {

                throw new IOException();
            }
        }

        mDownloader.abort(uri, TimeDuration.seconds(20));

        assertThat(mDownloader.isDownloaded(uri)).isFalse();

        assertThat(outFile).doesNotExist();
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();

        Logger.setDefaultLogLevel(LogLevel.SILENT);

        delete(HUGE_FILE_URL);
        delete(SMALL_FILE_URL1);
        delete(SMALL_FILE_URL2);
        delete(SMALL_FILE_URL3);
    }

    private boolean delete(final String url) throws MalformedURLException, URISyntaxException {

        return new File(mTmpDirPath, getFileName(new URI(url))).delete();
    }

    private void waitFor(final URI uri, final long startTime, final long timeoutMs) throws
            IOException {

        final long timeout = startTime + timeoutMs - System.currentTimeMillis();

        if (timeout < 0) {

            throw new IOException();
        }

        mDownloader.waitDone(uri, TimeDuration.millis(timeout));
    }
}