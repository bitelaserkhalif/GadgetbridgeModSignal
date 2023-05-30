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
        super.onCreate(savedInstanceState);

        final Context appContext = this.getApplicationContext();
        if (getPrefs().getBoolean("pref_emergency_hr_enable",false) == true) {

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
            //GBApplication.deviceService(gbDevice).onHeartRateTest();
            GB.toast(getBaseContext(), "MAX HR:" + String.valueOf(heart_rate_threshold.get("maxHeartRateValue")) + " MIN HR: " + String.valueOf(heart_rate_threshold.get("minHeartRateValue")), 2000, 0);
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
        else if (getPrefs().getBoolean("pref_emergency_hr_enable",false) == false) {
            if (appContext instanceof GBApplication) {
                setContentView(R.layout.activity_heartrate_emergency);
            }
            textHR = findViewById(R.id.textHR);
            EmergencyHRActivityButton = findViewById(R.id.btnStopHR);
            getBaseContext().getString(R.string.on);
            GB.toast(getBaseContext(), (getBaseContext().getString(R.string.error_hr_disabled)), 2000, 0);
            textHR.setText(getBaseContext().getString(R.string.error_hr_disabled));
            EmergencyHRActivityButton.setVisibility(View.GONE);
        }
        }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOG.debug("destroyed");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }
    private void setMeasurementResults(Serializable result) {

        if (result instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) result;
            textHR.setVisibility(View.VISIBLE);
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())){
                //if (sample.getHeartRate()<int.valueOf(updateSmartHeartRatePreferences().get("minHeartRateValue"))){}
                if (sample.getHeartRate() > heart_rate_threshold.get("maxHeartRateValue").intValue()){
                    GB.toast(getBaseContext(), "EXCEED", 2000, 0);
                }
                if (sample.getHeartRate() < heart_rate_threshold.get("minHeartRateValue").intValue()){
                    GB.toast(getBaseContext(), "LOW", 2000, 0);
                }
                textHR.setText(String.valueOf(sample.getHeartRate()));
            }
        }
    }

}
