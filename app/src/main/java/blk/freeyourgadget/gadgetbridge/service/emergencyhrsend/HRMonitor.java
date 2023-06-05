package blk.freeyourgadget.gadgetbridge.service.emergencyhrsend;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.EmergencyHRActivity;
import blk.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.ActivityUser;
import blk.freeyourgadget.gadgetbridge.model.DeviceService;
import blk.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class HRMonitor  extends Service {
    Map<String, Number> heart_rate_threshold = updateHeartRateThreshold();

    public static final String P_HR_SMART;
    public static final String P_HR_MANUAL;
    public static final String P_HR_READMAX;
    private boolean isHRRunning;// this variable clears up everytime...

    final WhatsappSupport whatsappsupport = new WhatsappSupport();

    static {
        Context CONTEXT = GBApplication.getContext();
        P_HR_SMART = CONTEXT.getString(R.string.pref_emergency_hr_value_smart);
        P_HR_MANUAL = CONTEXT.getString(R.string.pref_emergency_hr_value_manual);
        P_HR_READMAX = CONTEXT.getString(R.string.pref_emergency_hr_value_readmax);
    }
    final ActivityUser activityUser = new ActivityUser();
    final int height = activityUser.getHeightCm();
    final int weight = activityUser.getWeightKg();
    final int birthYear = activityUser.getYearOfBirth();
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyHRActivity.class);

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
    //startService(new Intent(this, NotificationCollectorMonitorService.class)); soon
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

    private void setMeasurementResults(Serializable result) {
        //progressHR.setVisibility(View.GONE);
        if (isHRRunning == true){
            if (result instanceof ActivitySample) {
                isHRRunning = true;
                ActivitySample sample = (ActivitySample) result;
                if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())){
                    //if (sample.getHeartRate()<int.valueOf(updateSmartHeartRatePreferences().get("minHeartRateValue"))){}
                    LOG.debug("Emergency HR monitor is ongoing");
                    //textHR.setText(String.valueOf(sample.getHeartRate()));
                    if (sample.getHeartRate() > heart_rate_threshold.get("maxHeartRateValue").intValue()){
                        GB.toast(getBaseContext(), (getBaseContext().getString(R.string.emergency_hr_anomaly_high)), 2000, GB.WARN);
                        emergencyWarning(getBaseContext().getString(R.string.emergency_hr_anomaly_high));
                        //HIGH HEART RATE
                    }
                    if (sample.getHeartRate() < heart_rate_threshold.get("minHeartRateValue").intValue()){
                        GB.toast(getBaseContext(), (getBaseContext().getString(R.string.emergency_hr_anomaly_low)), 2000, GB.WARN);
                        emergencyWarning(getBaseContext().getString(R.string.emergency_hr_anomaly_low));
                        //LOW HEART RATE
                    }

                }
            }
        }
        else {
            enableEmergencyHRTracking(false);
        }
    }
    //HR VALUES:

    private void emergencyWarning(String HR){

        final Location lastKnownLocation = new CurrentPosition().getLastKnownLocation();
        final Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        final MediaPlayer player = MediaPlayer.create(this, notification);
        //this is whatsapp sending method. Set to false to prevent spamming
        if((getPrefs().getString("emergency_hr_telno_cc1","")!="" && getPrefs().getString("emergency_hr_telno1","")!="") ||
                (getPrefs().getString("emergency_hr_telno_cc1","")!="" || getPrefs().getString("emergency_hr_telno1","")!="")) {

            if (!whatsappsupport.isAccessibilityOn()) {
                accessibilityPopup();
            }
            else if (whatsappsupport.isAccessibilityOn() && !player.isPlaying()){//stops spam
                whatsappsupport.sendWAEmergency(getPrefs().getString("emergency_hr_telno_cc1", ""), getPrefs().getString("emergency_hr_telno1", ""), lastKnownLocation, HR,true);
                player.start();
                //finish();//prevent double, workaround
            }
        }

        //textHR.setText("!!!");
        enableEmergencyHRTracking(false);


        if(isHRRunning==true) {
            isHRRunning = false;
        }
        //finish();//prevent double, workaround
    }
    private void enableEmergencyHRTracking(boolean enable) {
        IntentFilter filter = new IntentFilter();
        boolean globalHRMonitor = getPrefs().getBoolean("pref_emergency_hr_enable",false);
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LOG.debug("HRRUNNING:"+String.valueOf(isHRRunning));
        LOG.debug("enable:"+String.valueOf(enable));
        if(globalHRMonitor){
            if (enable && !isHRRunning) {//first start
                //textHR.setText("Starting..");
                //textFlavourHRStatus.setText(getString(R.string.start));
                //progressHR.setVisibility(View.VISIBLE);
                LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
                getContext().registerReceiver(mReceiver, filter);
                isHRRunning = true;
            }else if (enable && isHRRunning) {//started but in process, prevent duplicates

                isHRRunning = true;
            }
            else if (!enable && isHRRunning) {//exiting
                //textHR.setText("Stopping..");
                //textFlavourHRStatus.setText(getString(R.string.stop));
                //progressHR.setVisibility(View.VISIBLE);
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
                getContext().unregisterReceiver(mReceiver);
                isHRRunning = false;
            }
            else if (!enable && !isHRRunning) {//started but in process, prevent duplicates
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
                getContext().unregisterReceiver(mReceiver);
                isHRRunning = false;
            }
        }
        else if(!globalHRMonitor){
            GB.toast(getBaseContext(), (getBaseContext().getString(R.string.error_hr_disabled)), 2000, 0);
            //textFlavourHRLimit.setText(getBaseContext().getString(R.string.error_hr_disabled));
            //progressHR.setVisibility(View.GONE);
            //btnStopHR.setVisibility(View.GONE);
            //btnStartHR.setVisibility(View.GONE);
        }
        else{
        }
    }

    private void accessibilityPopup(){
        LOG.debug("ACC ON:" +whatsappsupport.isAccessibilityOn());
        GB.toast(getBaseContext(), "Accessibility is not set for whatsapp messaging, please set first!", 5000, 0);
        Intent accessible = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        accessible.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(accessible);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
