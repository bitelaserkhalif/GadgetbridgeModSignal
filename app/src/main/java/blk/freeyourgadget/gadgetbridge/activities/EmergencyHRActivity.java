package blk.freeyourgadget.gadgetbridge.activities;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.activities.HeartRateUtils.MAX_HEART_RATE_VALUE;
import static blk.freeyourgadget.gadgetbridge.activities.HeartRateUtils.MIN_HEART_RATE_VALUE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.ActivityUser;
import blk.freeyourgadget.gadgetbridge.model.DeviceService;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.GBPrefs;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class EmergencyHRActivity extends AbstractGBActivity  {
    //Activity for heart rate monitoring.
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyHRActivity.class);
    TextView textHR;
    GBDevice gbDevice;
    private int maxHeartRateValue;
    private int minHeartRateValue;
    final ActivityUser activityUser = new ActivityUser();
    final int height = activityUser.getHeightCm();
    final int weight = activityUser.getWeightKg();
    final int birthYear = activityUser.getYearOfBirth();
    Button EmergencyHRActivityButton;
    private ScheduledExecutorService pulseScheduler;
    Map<String, Number> heart_rate_threshold = updateSmartHeartRatePreferences();

    public Map<String, Number> updateSmartHeartRatePreferences(){
        Map<String, Number> hrvalues = new HashMap<>();
        Prefs prefs = GBApplication.getPrefs();
        prefs.getString("pref_emergency_hr_max_value","smart");
        if (birthYear==0){
            LOG.debug("birthYear not set | get value from heartrate_alert_threshold");

            hrvalues.put("maxHeartRateValue",prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD, 150));
            hrvalues.put("minHeartRateValue",prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD, 45));
        }
        else{
            LOG.debug("birthYear: "+String.valueOf(birthYear));
            hrvalues.put("maxHeartRateValue",getMaxHeartRateSmart());
            hrvalues.put("minHeartRateValue",getMinHeartRate());
            //add smart system here.
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
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = getIntent();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        getContext().registerReceiver(mReceiver, filter);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            gbDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        if (birthYear==0){
            LOG.debug("birthYear not set");
        }
        else{
            LOG.debug("birthYear: "+String.valueOf(birthYear));

        }
        GBApplication.deviceService(gbDevice).onHeartRateTest();
        LOG.debug("age: "+String.valueOf(getEstimatedAge()));
        GB.toast(getBaseContext(), "MAX HR:"+ String.valueOf(heart_rate_threshold.get("maxHeartRateValue")) + " MIN HR: " + String.valueOf(heart_rate_threshold.get("minHeartRateValue")), 2000, 0);
        super.onCreate(savedInstanceState);
        final Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_heartrate_emergency);
        }
        textHR = findViewById(R.id.textHR);
        EmergencyHRActivityButton = findViewById(R.id.btnStopHR);
        pulseScheduler = startActivityPulse();

        EmergencyHRActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GB.toast(getBaseContext(), "STOP", 2000, 0);
                stopActivityPulse();

            }
        });

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        getContext().unregisterReceiver(mReceiver);
        super.onDestroy();
    }
    private void setMeasurementResults(Serializable result) {

        if (result instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) result;
            textHR.setVisibility(View.VISIBLE);
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate()))
                textHR.setText(String.valueOf(sample.getHeartRate()));
        }
    }

}
