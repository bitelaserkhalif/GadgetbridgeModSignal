package blk.freeyourgadget.gadgetbridge.activities;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

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
import blk.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class EmergencyHRActivity extends AbstractGBActivity  {
    //Activity for heart rate monitoring.

    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private boolean isHRRunning;// this variable clears up everytime...
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyHRActivity.class);
    TextView textHR;
    GBDevice gbDevice;
    ProgressBar progressHR;
    TextView textFlavourHRLimit;
    TextView textFlavourHRStatus;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context appContext = this.getApplicationContext();

        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_heartrate_emergency);
        }
        textHR = findViewById(R.id.textHR);
        textFlavourHRLimit = findViewById(R.id.textFlavourHRLimit);
        textFlavourHRStatus = findViewById(R.id.textFlavourHRStatus);
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
                        textFlavourHRStatus.setText(getString(R.string.stop));
                        textHR.setText("...");
                        GB.toast(getBaseContext(), "STOP", 2000, 0);
                    }else GB.toast(getBaseContext(), "ALREADY STOP", 2000, 0);


                }
            });
            btnStartHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isHRRunning==false){
                        isHRRunning = true;
                        textFlavourHRStatus.setText(getString(R.string.start));
                        textHR.setText("...");
                        GB.toast(getBaseContext(), "START", 2000, 0);
                    }
                    else GB.toast(getBaseContext(), "ALREADY START", 2000, 0);

                }
            });
        }
        else if (getPrefs().getBoolean("pref_emergency_hr_enable",false) == false) {

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
        boolean globalHRMonitor = getPrefs().getBoolean("pref_emergency_hr_enable",false);
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        if(globalHRMonitor){
            if (enable && !isHRRunning) {
                textHR.setText("Starting..");
                textFlavourHRStatus.setText(getString(R.string.start));
                progressHR.setVisibility(View.VISIBLE);
                LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
                getContext().registerReceiver(mReceiver, filter);
                isHRRunning = true;
            } else if (!enable && isHRRunning) {
                textHR.setText("Stopping..");
                textFlavourHRStatus.setText(getString(R.string.stop));
                progressHR.setVisibility(View.VISIBLE);
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
                getContext().unregisterReceiver(mReceiver);
                isHRRunning = false;
            }
        }
        else if(!globalHRMonitor){
            GB.toast(getBaseContext(), (getBaseContext().getString(R.string.error_hr_disabled)), 2000, 0);
            textFlavourHRLimit.setText(getBaseContext().getString(R.string.error_hr_disabled));
            progressHR.setVisibility(View.GONE);
            btnStopHR.setVisibility(View.GONE);
            btnStartHR.setVisibility(View.GONE);
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
                LOG.debug("Hr monitor is ongoing");
                if (sample.getHeartRate() > heart_rate_threshold.get("maxHeartRateValue").intValue()){
                    emergencyWarning("HR HIGH");
                    //GB.toast(getBaseContext(), "EXCEED", 2000, 0);
                }
                if (sample.getHeartRate() < heart_rate_threshold.get("minHeartRateValue").intValue()){
                    emergencyWarning("HR LOW");
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
    //HR VALUES:

    private void emergencyWarning(String HR){
        final Location lastKnownLocation = new CurrentPosition().getLastKnownLocation();
        LOG.debug(HR);
        //LOG.debug (getPrefs().getString("emergency_hr_telno_cc1","00") + getPrefs().getString("emergency_hr_telno1","0000000000000"));
        LOG.debug ("VALIDITY OF TELNO: "+ String.valueOf(isTelNoValid(getPrefs().getString("emergency_hr_telno_cc1","00") , getPrefs().getString("emergency_hr_telno1","0000000000000"))));
        LOG.debug(String.valueOf(lastKnownLocation));


        //this is whatsapp sending method. Set to false to prevent spamming
        sendWAEmergency(getPrefs().getString("emergency_hr_telno_cc1","00"),getPrefs().getString("emergency_hr_telno1","0000000000000"),lastKnownLocation);
        isHRRunning = false;
        enableEmergencyHRTracking(false);

    }
    private boolean isTelNoValid(String countryCode, String telNo){
        Phonenumber.PhoneNumber number = new Phonenumber.PhoneNumber();
        try {
            // the parse method parses the string and
            // returns a PhoneNumber in the format of
            // specified region
            number = phoneNumberUtil.parse("+"+countryCode+telNo,"");

            // this statement prints the type of the phone
            // number
            //LOG.debug(String.valueOf(phoneNumberUtil.getNumberType(number)));
            //LOG.debug(String.valueOf(phoneNumberUtil.getRegionCodeForNumber(number)));
        }
        catch (NumberParseException e) {
            LOG.error("CANNOT PARSE TEL.NO");
            e.printStackTrace();
        }

        return phoneNumberUtil.isValidNumber(number);
        }
        //OVERLOADING IN PRACTICE: IF ARGUMENT OF WARNING ARE GIVEN, IT'LL BE SHOWN.
    private void sendWAEmergency(String countryCode, String telNo, @NonNull Location lastKnownLocation){
        String uri = "http://maps.google.com/maps?saddr=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        //"+393291876000"
        whatsappIntent.putExtra("jid","+"+countryCode +telNo + "@s.whatsapp.net");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, "EMERGENCY WARNING:");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, uri);
        try {
            startActivity(whatsappIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            GB.toast(getBaseContext(), ("Whatsapp have not been installed."), 2000, GB.WARN);
        }
    }
    private void sendWAEmergency(String countryCode, String telNo, @NonNull Location lastKnownLocation, String reasoning){
        String uri = "http://maps.google.com/maps?saddr=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        //"+393291876000"
        whatsappIntent.putExtra("jid","+"+countryCode +telNo + "@s.whatsapp.net");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, "EMERGENCY WARNING:");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, reasoning);
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, uri);
        try {
            startActivity(whatsappIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            GB.toast(getBaseContext(), ("Whatsapp have not been installed."), 2000, GB.WARN);
        }
    }
}
