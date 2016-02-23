package com.ishan1608.panoramadisplay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Downloads and returns the File object
 * Created by ishan on 9/2/16.
 */
public class FileDownloader {

    private static final String TAG = "FILE_DOWNLOADER";
    private static final String PROGRESS_LISTENER_ERROR_MESSAGE = "Progress listener not registered";
    private static final String DOWNLOADED_LISTENER_ERROR_MESSAGE = "File downloaded listener not registered";
    private Context context;
    private FileDownloadedListener fileDownloadedListener;
    private FileDownloadProgressListener progressListener;
    private static final String LAST_DOWNLOADED_FILE = "com.ishan1608.tobedeleted:LAST_DOWNLOADED_FILE";
    private SharedPreferences fileDownloaderPreferences;
    private SharedPreferences.Editor fileDownloaderPreferencesEditor;

    /**
     * Initializes {@link FileDownloader}
     * @param context context to work upon
     */
    public FileDownloader(Context context) {
        initialize(context);
    }

    @SuppressLint("CommitPrefEdits")
    private void initialize(Context context) {
        this.context = context;
        // Shared Preferences Editor
        fileDownloaderPreferences = context.getSharedPreferences(
                FileDownloader.class.getSimpleName(),
                Context.MODE_PRIVATE);
        fileDownloaderPreferencesEditor = fileDownloaderPreferences.edit();
        fileDownloaderPreferencesEditor.commit();
    }

    /**
     * Interface definition for a callback to be invoked when file is downloaded
     */
    public interface FileDownloadedListener {
        /**
         * Called when file is downloaded
         * @param file downloaded file
         */
        void onFileDownloaded(File file);
    }

    /**
     * Register a callback to be invoked when download finished
     * @param listener The callback that will run
     */
    public void setFileDownloadedListener(FileDownloadedListener listener) {
        this.fileDownloadedListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when file download progresses
     */
    public interface FileDownloadProgressListener {
        /**
         * Called when download progresses
         * @param progress current total progress
         * @param length total content length
         */
        void onFileDownloadProgress(int progress, int length);
    }

    /**
     * Register a callback to be invoked when download progresses
     * @param listener The callback that will run
     */
    public void setFileDownloadProgressListener(FileDownloadProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Starts download of the file
     * @param url url of the file to download
     * @throws NullPointerException when {@link FileDownloader#fileDownloadedListener} and
     * {@link FileDownloader#fileDownloadedListener} aren't registered
     */
    public void downloadFile(final String url) throws NullPointerException{
        Log.d(TAG, "downloadFile called with " + url);
        // Check if last file downloaded was different
        String lastDownloadedFile = fileDownloaderPreferences.getString(LAST_DOWNLOADED_FILE, "");
        if (lastDownloadedFile.equalsIgnoreCase("") || !lastDownloadedFile.equalsIgnoreCase(url)) {
            Log.d(TAG, "Previous download is different");
            downloadFileFromUrl(url);
        } else {
            Log.d(TAG, "File already downloaded");
            File panoramaJpgFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/panorama.jpg");
            fileDownloadedListener.onFileDownloaded(panoramaJpgFile);
        }
    }

    /**
     * Actual file download, downloads and saves the file on disk, overriding previously saved file
     * @param url url of the file to download
     */
    private void downloadFileFromUrl(final String url) {
        // Downloading file on a different thread
        new Thread() {
            @Override
            public void run() {
                try {
                    URL fileURL = new URL(url);
                    URLConnection connection = fileURL.openConnection();
                    int length = connection.getContentLength();
                    int progress = 0;
                    if (progressListener != null) {
                        progressListener.onFileDownloadProgress(0, length);
                    } else {
                        throw new NullPointerException(PROGRESS_LISTENER_ERROR_MESSAGE);
                    }
                    InputStream in = connection.getInputStream();
                    File panoramaJpgFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/panorama.jpg");
                    FileOutputStream fos = new FileOutputStream(panoramaJpgFile);
                    byte[] buf = new byte[512];
                    while (true) {
                        int len = in.read(buf);
                        if (len == -1) {
                            break;
                        } else {
                            progress += len;
                            if (progressListener != null) {
                                progressListener.onFileDownloadProgress(progress, length);
                            } else {
                                throw new NullPointerException(PROGRESS_LISTENER_ERROR_MESSAGE);
                            }
                        }
                        fos.write(buf, 0, len);
                    }
                    in.close();
                    fos.flush();
                    fos.close();

                    Log.d(TAG, "Path : " + panoramaJpgFile.getAbsolutePath());
                    fileDownloaderPreferencesEditor.putString(LAST_DOWNLOADED_FILE, url);
                    fileDownloaderPreferencesEditor.commit();
                    if (fileDownloadedListener != null) {
                        fileDownloadedListener.onFileDownloaded(panoramaJpgFile);
                    } else {
                        throw new NullPointerException(DOWNLOADED_LISTENER_ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
