package com.ishan1608.panoramadisplay;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.panorama.Panorama;
import com.google.android.gms.panorama.PanoramaApi.PanoramaResult;

import java.io.File;
import com.ishan1608.panoramadisplay.FileDownloader.FileDownloadedListener;

/**
 * Created by Ishan on 23-02-2016.
 */
public class MainActivity extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private GoogleApiClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG", TAG);
        mClient = new GoogleApiClient.Builder(this, this, this)
                .addApi(Panorama.API)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected");
//        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.casabella_hall_1);

        FileDownloader mDownloader = new FileDownloader(this);
        mDownloader.setFileDownloadProgressListener(new FileDownloader.FileDownloadProgressListener() {
            @Override
            public void onFileDownloadProgress(int progress, int length) {
                Log.d(TAG, "Download Progress " + progress + " / " + length);
            }
        });
        mDownloader.setFileDownloadedListener(new FileDownloadedListener() {
            @Override
            public void onFileDownloaded(File file) {
                Log.d(TAG, "onFileDownloaded : " + Uri.fromFile(file).getPath());
                Panorama.PanoramaApi.loadPanoramaInfo(mClient, Uri.fromFile(file)).setResultCallback(
                        new ResultCallback<PanoramaResult>() {
                            @Override
                            public void onResult(PanoramaResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Intent viewerIntent = result.getViewerIntent();
                                    Log.i(TAG, "found viewerIntent: " + viewerIntent);
                                    if (viewerIntent != null) {
                                        startActivity(viewerIntent);
                                    }
                                } else {
                                    Log.e(TAG, "error: " + result);
                                }
                            }
                        });
            }
        });
        mDownloader.downloadFile("https://dl.dropboxusercontent.com/s/ow6tpp8marvo0f8/pano1.jpg?dl=0");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "connection suspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult status) {
        Log.e(TAG, "connection failed: " + status);
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }
}
