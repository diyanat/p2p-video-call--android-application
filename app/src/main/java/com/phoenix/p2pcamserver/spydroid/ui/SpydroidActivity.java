/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.phoenix.p2pcamserver.spydroid.ui;

import com.phoenix.p2pcamserver.http.TinyHttpServer;

import com.phoenix.p2pcamserver.R;

import com.phoenix.p2pcamserver.spydroid.SpydroidApplication;
import com.phoenix.p2pcamserver.spydroid.Utilities;
import com.phoenix.p2pcamserver.spydroid.api.CustomHttpServer;
import com.phoenix.p2pcamserver.spydroid.api.CustomRtspServer;
import com.phoenix.p2pcamserver.streaming.SessionBuilder;
import com.phoenix.p2pcamserver.streaming.gl.SurfaceView;
import com.phoenix.p2pcamserver.streaming.rtsp.RtspServer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

/**
 * Spydroid basically launches an RTSP server and an HTTP server,
 * clients can then connect to them and start/stop audio/video streams on the phone.
 */
public class SpydroidActivity extends FragmentActivity {

    static final public String TAG = "SpydroidActivity";
    Button startBtn;
    VideoView videoView;
    TextView ipAddress;
    EditText ipSource;
    public final int HANDSET = 0x01;

    boolean flag=true;
    // We assume that the device is a phone
    public int device = HANDSET;

    private SurfaceView mSurfaceView;
    private SpydroidApplication mApplication;
    private RtspServer mRtspServer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (SpydroidApplication) getApplication();
        setContentView(R.layout.spydroid);
        startBtn = (Button) findViewById(R.id.startBtn);
        ipAddress = (TextView) findViewById(R.id.ipText);
        ipSource = (EditText) findViewById(R.id.ipSource);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mSurfaceView = (SurfaceView) findViewById(R.id.handset_camera_view);
        SessionBuilder.getInstance().setSurfaceView(mSurfaceView);
        SessionBuilder.getInstance().setPreviewOrientation(90);
        String ip = "";
        if ((ip = Utilities.getLocalIpAddress(true)) != null) {
            ipAddress.setText(ip);
            ipSource.setText(ip);
        }

        // Starts the service of the RTSP server
        this.startService(new Intent(this, CustomRtspServer.class));
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               videoManager(ipSource.getText().toString());
               // startActivity(new Intent(SpydroidActivity.this,Playing.class));
            }
        });




    }



//Playin Video
    public void videoManager(String url) {
//
//        AudioManager  audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        audioManager.setMode(AudioManager.MODE_IN_CALL);
//        audioManager.setSpeakerphoneOn(false);
//        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);

        videoView = (VideoView) findViewById(R.id.video);
        MediaController mc = new MediaController(this);

        //videoView.setMediaController(mc);
        videoView.setVideoURI(Uri.parse("rtsp://" + url + ":8086"));
        videoView.requestFocus();
        videoView.start();




    }


    public void onStart() {
        super.onStart();
        bindService(new Intent(this, CustomRtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onStop() {
        super.onStop();

        if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);
        unbindService(mRtspServiceConnection);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplication.applicationForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mApplication.applicationForeground = false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "SpydroidActivity destroyed");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.options:
                // Starts QualityListActivity where user can change the streaming quality
                intent = new Intent(this.getBaseContext(), OptionsActivity.class);
                startActivityForResult(intent, 0);
                return true;
            case R.id.quit:
                quitSpydroid();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void quitSpydroid() {
        // Removes notification
        if (mApplication.notificationEnabled) removeNotification();
        // Kills HTTP server
        this.stopService(new Intent(this, CustomHttpServer.class));
        // Kills RTSP server
        this.stopService(new Intent(this, CustomRtspServer.class));
        // Returns to home menu
        finish();
    }

    private ServiceConnection mRtspServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRtspServer = (CustomRtspServer) ((RtspServer.LocalBinder) service).getService();
            mRtspServer.addCallbackListener(mRtspCallbackListener);
            mRtspServer.start();
            //ipAddress.setText(ipAddress.getText().toString() + "req");
            update();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }



    };

    public void update() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                while (flag) {
//                    if ((mRtspServer != null && mRtspServer.isStreaming())){
//                        ipAddress.setText(ipAddress.getText().toString() + "  start");
//                        flag = false;
//                    }
//
//                }
//                if ((mRtspServer != null && mRtspServer.isStreaming()))
//                    ipAddress.setText(ipAddress.getText().toString() + "  start");
//                else
//                    ipAddress.setText(ipAddress.getText().toString() + "---");

            }
        });
    }

    private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

        @Override
        public void onError(RtspServer server, Exception e, int error) {
            // We alert the user that the port is already used by another app.
            if (error == RtspServer.ERROR_BIND_FAILED) {
                new AlertDialog.Builder(SpydroidActivity.this)
                        .setTitle(R.string.port_used)
                        .setMessage(getString(R.string.bind_failed, "RTSP"))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                startActivityForResult(new Intent(SpydroidActivity.this, OptionsActivity.class), 0);
                            }
                        })
                        .show();
            }
        }
        @Override
        public void onMessage(RtspServer server, int message) {
            if (message == RtspServer.MESSAGE_STREAMING_STARTED) {
              //  ipAddress.setText(ipAddress.getText().toString()+"   Start");
//                if (mAdapter != null && mAdapter.getHandsetFragment() != null)
//                    mAdapter.getHandsetFragment().update();
            } else if (message == RtspServer.MESSAGE_STREAMING_STOPPED) {
                //ipAddress.setText(ipAddress.getText().toString()+"   end");
//                if (mAdapter != null && mAdapter.getHandsetFragment() != null)
//                    mAdapter.getHandsetFragment().update();
            }
        }



    };

    private void removeNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    }

    public void log(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

}