package com.lambdasoup.choreoblink;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getName();
    private final static boolean[] CHOREO = new boolean[]{
            true,
            false,
            true,
            false,
            true,
            false,
            true,
            false,
            true,
            false
    };
    private final Torch torch = new Torch();
    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            Log.d(TAG, "torch enabled: " + enabled);
            if (enabled) {
//                onDelay = System.currentTimeMillis() - start;
            } else {
//                offDelay = System.currentTimeMillis() - start;
            }
        }
    };
    private ScheduledExecutorService executor = null;
    private String cameraId = null;
    private long delta = 0;
    private long onDelay = 0;
    private long offDelay = 0;
    private long gpsTime = 0;
    private final LocationListener locationListener = new android.location.LocationListener() {

        public void onLocationChanged(android.location.Location location) {

            long gpsTime = location.getTime();
            long currTime = System.currentTimeMillis();
            delta = gpsTime - currTime;
            MainActivity.this.gpsTime = gpsTime;
        }

        public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };
    private boolean showChoreo = false;
    private CameraManager cameraManager;
    private LocationManager locationManager;
    private TextView local;
    private TextView gps;
    private TextView d;
    private TextView dlay;
    private TextView corr;
    private final Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    d.setText(String.format(Locale.ENGLISH, "delta: %dms", delta));
                    dlay.setText(String.format(Locale.ENGLISH, "on_delay: %dms", onDelay));
                    gps.setText(formatTime(gpsTime));
                    local.setText(formatTime(System.currentTimeMillis()));
                    long real = System.currentTimeMillis() + delta;
                    corr.setText(formatTime(real));

                    long corr2 = real + onDelay;
                        choreo((int) (corr2 % 10000 / 1000));
                }
            });
        }
    };
    private Switch toggle;

    private static String formatTime(long t) {
        long ms = t % 1000;
        long s = t / 1000;
        return String.format(Locale.ENGLISH, "%ds%dms", s, ms);
    }

    private void choreo(int t) {
        if (!showChoreo) {
            return;
        }

        boolean on = CHOREO[t];
        Log.d(TAG, "on: " + String.valueOf(on) + "   t: " + t);
        if (on) {
            torch.on();
        } else {
            torch.off();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            }
        });

        local = findViewById(R.id.local);
        gps = findViewById(R.id.gps);
        d = findViewById(R.id.delta);
        dlay = findViewById(R.id.dlay);
        corr = findViewById(R.id.corr);
        toggle = findViewById(R.id.toggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    torch.off();
                }

                showChoreo = isChecked;
            }
        });
        findViewById(R.id.delay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                measureTorchDelay();
            }
        });

        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            Log.d(TAG, "list: " + cameraIdList);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIdList[0]);
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Log.d(TAG, "characteristics: " + characteristics);
            cameraId = cameraIdList[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void measureTorchDelay() {
        if (torch.on) {
            torch.off();
        } else {
            torch.on();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }

        cameraManager.registerTorchCallback(torchCallback, null);

        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(updateTime, 0, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onPause() {

        cameraManager.unregisterTorchCallback(torchCallback);

        locationManager.removeUpdates(locationListener);

        executor.shutdown();

        torch.off();

        super.onPause();
    }

    private class Torch {

        private boolean on = false;

        void on() {
            if (on) {
                return;
            }
            on = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraManager.setTorchMode(cameraId, true);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        void off() {
            if (!on) {
                return;
            }
            on = false;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraManager.setTorchMode(cameraId, false);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }


}
