package blk.freeyourgadget.gadgetbridge.activities;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.ActivityUser;
import blk.freeyourgadget.gadgetbridge.model.DeviceService;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class EmergencyHRActivity extends AbstractGBActivity  {
    //Activity for heart rate monitoring.
    private boolean isHRRunning;// this variable clears up everytime...
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyHRActivity.class);
    TextView textHR;
    GBDevice gbDevice;
    ProgressBar progressHR;
    TextView textFlavourHRLimit;

    private int maxHeartRateValue;
    private int minHeartRateValue;
    final ActivityUser activityUser = new ActivityUser();
    final int height = activityUser.getHeightCm();
    final int weight = activityUser.getWeightKg();
    final int birthYear = activityUser.getYearOfBirth();
    Button btnStopHR;
    Button btnStartHR;
    private ScheduledExecutorService pulseScheduler;
    Map<String, Number> heart_rate_threshold = updateHeartRateThreshold();

    public static final String P_HR_SMART;
    public static final String P_HR_MANUAL;
    public static final String P_HR_READMAX;


    static {
        Context CONTEXT = GBApplication.getContext();
        P_HR_SMART = CONTEXT.getString(R.string.pref_emergency_hr_value_smart);
        P_HR_MANUAL = CONTEXT.getString(R.string.pref_emergency_hr_value_manual);
        P_HR_READMAX = CONTEXT.getString(R.string.pref_emergency_hr_value_readmax);
    }

    public Map<String, Number> updateHeartRateThreshold(){
        Map<String, Number> hrvalues = new HashMap<>();
        Prefs prefs = GBApplication.getPrefs();
        String GetHRType = prefs.getString("pref_emergency_hr_type","smart");
        if (GetHRType.equals(P_HR_SMART)){
            //SMART TYPE = detect by age
            LOG.debug("hr is smart");
            if (birthYear==0){//failsafe if invalid
                LOG.debug("birthYear not set | get value from heartrate_alert_threshold");
                hrvalues.put("maxHeartRateValue",getMaxHeartRate());
                hrvalues.put("minHeartRateValue",getMinHeartRate());
            }
            else{
                LOG.debug("birthYear: "+String.valueOf(birthYear));
                hrvalues.put("maxHeartRateValue",getMaxHeartRateSmart());
                hrvalues.put("minHeartRateValue",getMinHeartRate());
                //add smart system here.
            }
        }
        else if (GetHRType.equals(P_HR_READMAX)){
            //per device type HR LIMIT
            LOG.debug("hr is per-device max");
            hrvalues.put("maxHeartRateValue",getMaxHeartRate());
            hrvalues.put("minHeartRateValue",getMinHeartRate());
        }
        else if (GetHRType.equals(P_HR_MANUAL)){
            LOG.debug("hr is manual");
            //MANUAL KEY-IN
            hrvalues.put("maxHeartRateValue",prefs.getInt("emergency_hr_max",150));
            hrvalues.put("minHeartRateValue",prefs.getInt("emergency_hr_min",45));
        }
        return hrvalues;
    }

    public int getEstimatedAge(){
        //estimation of current age
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);
        int age = todayYear-birthYear;
        //example is year 2000
        //return maxHeartRateValue;
        return age;
    }
    public int getMaxHeartRateSmart(){
        //estimation of current age
        int age = getEstimatedAge();
        int maxhr = 220;
        //return maxHeartRateValue;
        return maxhr-age;
    }

    public int getMinHeartRate(){
        //estimation of current age
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD, 45);
    }
    public int getMaxHeartRate(){
        //estimation of current age
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD, 150);
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    setMeasurementResults(intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                default:
                    LOG.info("ignoring intent action " + intent.getAction());
                    break;
            }
        }
    };

/*
    private void pulse() {

        // have to enable it again and again to keep it measuring
        GBApplication.deviceService().onEnableRealtimeHeartRateMeasurement(true);
    }
    private ScheduledExecutorService startActivityPulse() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                pulse();
            }
        }, 0, getPulseIntervalMillis(), TimeUnit.MILLISECONDS);
        return service;
    }

    private int getPulseIntervalMillis() {
        return 1000;
    }

    private void stopActivityPulse() {
        if (pulseScheduler != null) {
            pulseScheduler.shutdownNow();
            pulseScheduler = null;
        }

        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("emergency_hr_device", false);

        final Context appContext = this.getApplicationContext();

        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_heartrate_emergency);
        }
        textHR = findViewById(R.id.textHR);
        textFlavourHRLimit = findViewById(R.id.textFlavourHRLimit);
        btnStartHR = findViewById(R.id.btnStartHR);
        btnStopHR = findViewById(R.id.btnStopHR);
        progressHR = findViewById(R.id.progressHR);


        //enable broadcast if not detected, !receiverCheck meaning receiverCheck is false / nonexistent due to exception caused.
        if (getPrefs().getBoolean("pref_emergency_hr_enable",false) == true) {
            //pulseScheduler = startActivityPulse();
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                gbDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
            } else {
                throw new IllegalArgumentException("Must provide a device when invoking this activity");
            }
            //GBApplication.deviceService(gbDevice).onHeartRateTest();
            //GB.toast(getBaseContext(), "MAX HR:" + String.valueOf(heart_rate_threshold.get("maxHeartRateValue")) + " MIN HR: " + String.valueOf(heart_rate_threshold.get("minHeartRateValue")), 2000, 0);

            textFlavourHRLimit.setText(
                    "MAX HR:" + String.valueOf(heart_rate_threshold.get("maxHeartRateValue")) + " MIN HR: " + String.valueOf(heart_rate_threshold.get("minHeartRateValue"))
            );
            //pulseScheduler = startActivityPulse();
            btnStartHR.setText(getBaseContext().getString(R.string.start));
            btnStopHR.setText(getBaseContext().getString(R.string.stop));

            btnStopHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //enable broadcast if not detected, !receiverCheck meaning receiverCheck is false / nonexistent due to exception caused.
                    //receiverCheck meaning it's true / existent.
                    if(isHRRunning==true) {
                        isHRRunning = false;
                        GB.toast(getBaseContext(), "STOP", 2000, 0);
                    }else GB.toast(getBaseContext(), "ALREADY STOP", 2000, 0);


                }
            });
            btnStartHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isHRRunning==false){
                        isHRRunning = true;
                        GB.toast(getBaseContext(), "START", 2000, 0);
                    }
                    else GB.toast(getBaseContext(), "ALREADY START", 2000, 0);

                }
            });
        }
        else if (getPrefs().getBoolean("pref_emergency_hr_enable",false) == false) {

            GB.toast(getBaseContext(), (getBaseContext().getString(R.string.error_hr_disabled)), 2000, 0);
            textHR.setText(getBaseContext().getString(R.string.error_hr_disabled));
            btnStopHR.setVisibility(View.GONE);
            btnStartHR.setVisibility(View.GONE);
        }
        }

    /*
        @Override
    protected void onDestroy() {
        super.onDestroy();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

    }
    */
    private void enableEmergencyHRTracking(boolean enable) {
        IntentFilter filter = new IntentFilter();

        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        if (enable && !isHRRunning) {
            textHR.setText(getString(R.string.start));
            progressHR.setVisibility(View.VISIBLE);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
            getContext().registerReceiver(mReceiver, filter);
            isHRRunning = true;
        } else if (!enable && isHRRunning) {
            textHR.setText(getString(R.string.stop));
            progressHR.setVisibility(View.VISIBLE);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
            getContext().unregisterReceiver(mReceiver);
            isHRRunning = false;
        }
        else{
        }
    }

    @Override protected void onPause() {
        LOG.debug("HIDE THE WARNING SYSTEM");

        enableEmergencyHRTracking(false);
        super.onPause();
    }

    @Override protected void onResume() {
        LOG.debug("SHOW THE WARNING SYSTEM");

        enableEmergencyHRTracking(true);
        super.onResume();
    }

    private void setMeasurementResults(Serializable result) {
        progressHR.setVisibility(View.GONE);
        if (isHRRunning == true){
        if (result instanceof ActivitySample) {
            isHRRunning = true;
            ActivitySample sample = (ActivitySample) result;
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())){
                //if (sample.getHeartRate()<int.valueOf(updateSmartHeartRatePreferences().get("minHeartRateValue"))){}
                if (sample.getHeartRate() > heart_rate_threshold.get("maxHeartRateValue").intValue()){
                    LOG.debug("HR EXCEED");
                    //GB.toast(getBaseContext(), "EXCEED", 2000, 0);
                }
                if (sample.getHeartRate() < heart_rate_threshold.get("minHeartRateValue").intValue()){
                    LOG.debug("HR LOW");
                    //GB.toast(getBaseContext(), "LOW", 2000, 0);
                }
                textHR.setText(String.valueOf(sample.getHeartRate()));
            }
        }
        }
        else {
            enableEmergencyHRTracking(false);
        }
    }

}
