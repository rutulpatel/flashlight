package com.onegoal.flashlight.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.triggertrap.seekarc.SeekArc;

import static android.view.View.OnClickListener;


public class MainActivity extends ActionBarActivity implements OnClickListener {

    private ImageView flashlight;
    private boolean hasFlash;
    private boolean isFlashOn = false;
    private boolean mainFlashOn = false;
    private boolean isFlasherOn = false;
    private Camera camera;
    Camera.Parameters params;
    private SharedPreferences prefs;
    private static final String myPrefs = "MyPrefs";
    private static final String flashStatus = "flashStatus";
    private static final String flasherStatus = "flasherStatus";
    private static final String mainFlashStatus = "mainFlashStatus";
    private static final String seekProgress = "seekProgress";
    private SeekArc mSeekArc;
    private int seekProgressValue=0;
    private android.os.Handler mHandler = new android.os.Handler();
    private int timeBetweenFlashes = 5000;
    private final int MAX_TIME_BETWEEN_FLASHES = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /**
         * Hiding the action bar for now as it is not needed
         */

        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion > Build.VERSION_CODES.FROYO) {
            ActionBar actionBar = getActionBar();
            actionBar.hide();
        }

        /**
         * get previous flash status from shared preferences
         */
        loadPrefs();

        /**
         * SeekArc initialization
         */
        mSeekArc = (SeekArc) findViewById(R.id.seekArc);
        mSeekArc.setProgress(seekProgressValue);
        if (seekProgressValue > 0) {
            isFlasherOn = true;
        }

        timeBetweenFlashes = getTimeBetweenFlashes(seekProgressValue);
        flashlight = (ImageView) findViewById(R.id.flashlight_img);
        flashlight.setOnClickListener(this);

        /**
         * Checking if device supports flash light or not
         */
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            AlertDialog flashNotSupportedAlertDialog = new AlertDialog.Builder(MainActivity.this)
                    .create();
            flashNotSupportedAlertDialog
                    .setTitle("Error");
            flashNotSupportedAlertDialog
                    .setMessage("Sorry, your device doesn't support flash light!");
            flashNotSupportedAlertDialog
                    .setButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Close application
                            finish();
                        }
                    });
            flashNotSupportedAlertDialog
                    .show();
            return;
        }

        getCamera();

        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            int stopProgressValue = 0;

            @Override
            public void onProgressChanged(SeekArc seekArc, int progress, boolean b) {
                stopProgressValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
                // Toast.makeText(getApplication(), ""+stopProgressValue, Toast.LENGTH_SHORT).show();
                if (stopProgressValue <= 1) {
                    seekArc.setProgress(0);
                    stopProgressValue = 0;
                }
                mainFlashOn = true;
                prefs.edit().putInt(seekProgress, stopProgressValue).commit();
                mHandler.removeCallbacks(flashTimer);

                if (stopProgressValue != 0) {
                    timeBetweenFlashes = getTimeBetweenFlashes(stopProgressValue);
                    mHandler.postDelayed(flashTimer, timeBetweenFlashes);

                } else if ((isFlashOn) || isFlasherOn) {
                    turnOnFlash();
                } else {
                    turnOffFlash();
                }
                if (stopProgressValue > 0 && isFlasherOn) {
                    flashlight.setImageResource(R.drawable.flashlight_on);
                }
            }
        });


    }

    /**
     * Flash timer
     */
    private Runnable flashTimer = new Runnable() {

        @Override
        public void run() {
                if (isFlasherOn) {
                    if ((timeBetweenFlashes > MAX_TIME_BETWEEN_FLASHES)) {
                        isFlasherOn = false;
                        //isFlashOn = true;
                    } else if (isFlashOn) {
                        turnOffFlash();
                    } else {
                        turnOnFlash();
                    }
                } else {
                    turnOnFlash();
                }
            mHandler.postDelayed(this, timeBetweenFlashes);
        }
    };

    /**
     * Load prefs
     */
    private void loadPrefs() {
        prefs = getSharedPreferences(myPrefs, Context.MODE_PRIVATE);

        if (prefs.contains(flashStatus)) {
            isFlashOn = prefs.getBoolean(flashStatus, false);
            seekProgressValue = prefs.getInt(seekProgress, 0);
            isFlasherOn = prefs.getBoolean(flasherStatus, false);
            mainFlashOn = prefs.getBoolean(mainFlashStatus, false);
        }

    }

    /**
     * Calculate time between flashes
     */
    private int getTimeBetweenFlashes(int seekValue) {
        int tBF=0;
        isFlasherOn = true;
        if (seekValue==0) {
            tBF = 100000;
            isFlasherOn = false;
        } else if (seekValue>95) {
            tBF = 50;
        } else if (seekValue>90) {
            tBF = 100;
        } else if (seekValue > 80) {
            tBF = 150;
        } else if (seekValue > 70) {
            tBF = 250;
        } else if (seekValue > 60) {
            tBF = 300;
        } else if (seekValue > 50) {
            tBF = 400;
        } else if (seekValue > 40) {
            tBF = 500;
        } else if (seekValue > 30) {
            tBF = 750;
        } else if (seekValue > 20) {
            tBF = 1000;
        } else if (seekValue > 10) {
            tBF = 1500;
        } else {
            tBF = 2000;
        }
        return tBF;
    }

    /**
     * Get Camera
     */
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                Log.e("Camera Error. Failed to Open. Error: ", e.getMessage());
            }
        }
    }

    /**
     * Turning on flash light
     */
    public void turnOnFlash() {
        if (camera == null || params == null) {
            return;
        }
        params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(params);
        camera.startPreview();
        isFlashOn = true;
        //flashlight.setText("OFF");
    }

    /**
     * Turning off flash light
     */
    private void turnOffFlash() {
        if (camera == null || params == null) {
            return;
        }
        params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(params);
        camera.stopPreview();
        isFlashOn = false;

        //flashlight.setText("ON");
    }

    /**
     * Release camera
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.flashlight_img:
                if (mainFlashOn) {
                    turnOffFlash();
                    flashlight.setImageResource(R.drawable.flashlight_off);
                    isFlasherOn=false;
                    mHandler.removeCallbacks(flashTimer);
                    mainFlashOn = false;
                } else {
                    turnOnFlash();
                    flashlight.setImageResource(R.drawable.flashlight_on);
                    seekProgressValue = prefs.getInt(seekProgress, 0);
                    timeBetweenFlashes = getTimeBetweenFlashes(seekProgressValue);
                    mHandler.postDelayed(flashTimer, timeBetweenFlashes);
                    mainFlashOn = true;
                }
                break;
            default:
                Toast.makeText(getBaseContext(), "Check code, something is wrong", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(flashTimer);
        mHandler.removeMessages(0);
        releaseCamera();
        prefs.edit().putBoolean(flashStatus, isFlashOn).commit();
        prefs.edit().putBoolean(flasherStatus, isFlasherOn).commit();
        prefs.edit().putBoolean(mainFlashStatus, mainFlashOn).commit();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrefs();
        getCamera();
        if (seekProgressValue > 0 && mainFlashOn) {
            mHandler.removeCallbacks(flashTimer);
            //Toast.makeText(getApplication(), "" + seekProgressValue, Toast.LENGTH_SHORT).show();
            timeBetweenFlashes = getTimeBetweenFlashes(seekProgressValue);
            mHandler.postDelayed(flashTimer, timeBetweenFlashes);
            flashlight.setImageResource(R.drawable.flashlight_on);
        } else if (!mainFlashOn) {
            turnOffFlash();
            flashlight.setImageResource(R.drawable.flashlight_off);
        } else {
            turnOnFlash();
            flashlight.setImageResource(R.drawable.flashlight_on);
        }

    }

    @Override
    protected void onStop() {
        mHandler.removeCallbacks(flashTimer);
        mHandler.removeMessages(0);
        releaseCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //releaseCamera();
    }


}
