package blk.freeyourgadget.gadgetbridge.activities;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;
import static blk.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;
import static blk.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_ID_ERROR;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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
import blk.freeyourgadget.gadgetbridge.service.NotificationCollectorMonitorService;
import blk.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import blk.freeyourgadget.gadgetbridge.service.emergencyhrsend.HRMonitor;
import blk.freeyourgadget.gadgetbridge.service.emergencyhrsend.WhatsappAccessibilityService;
import blk.freeyourgadget.gadgetbridge.service.emergencyhrsend.WhatsappSupport;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class EmergencyHRActivity extends AbstractGBActivity  {
    //Activity for heart rate monitoring.

    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private boolean isHRRunning;// this variable clears up everytime...
    final WhatsappSupport whatsappsupport = new WhatsappSupport();

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
    Map<String, Number> heart_rate_threshold = new HRMonitor().updateHeartRateThreshold();

    public static final String P_HR_SMART;
    public static final String P_HR_MANUAL;
    public static final String P_HR_READMAX;


    static {
        Context CONTEXT = GBApplication.getContext();
        P_HR_SMART = CONTEXT.getString(R.string.pref_emergency_hr_value_smart);
        P_HR_MANUAL = CONTEXT.getString(R.string.pref_emergency_hr_value_manual);
        P_HR_READMAX = CONTEXT.getString(R.string.pref_emergency_hr_value_readmax);
    }



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
                    getString(R.string.prefs_heartrate_alert_low_threshold)+": " +String.valueOf(heart_rate_threshold.get("minHeartRateValue")+"\n"+
                            getString(R.string.prefs_heartrate_alert_high_threshold) +": " + String.valueOf(heart_rate_threshold.get("maxHeartRateValue")) )
            );
            //pulseScheduler = startActivityPulse();
            btnStartHR.setText(getBaseContext().getString(R.string.start));
            btnStopHR.setText(getBaseContext().getString(R.string.stop));
            btnStopHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //enable broadcast if not detected, !receiverCheck meaning receiverCheck is false / nonexistent due to exception caused.
                    //receiverCheck meaning it's true / existent.

                        textFlavourHRStatus.setText(getString(R.string.stop));
                        textHR.setText("...");
                        GB.toast(getBaseContext(), "STOP", 2000, 0);


                }
            });
            btnStartHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        textFlavourHRStatus.setText(getString(R.string.start));
                        textHR.setText("...");
                        GB.toast(getBaseContext(), "START", 2000, 0);
                    try {
                        //the following will ensure the notification manager is kept alive
                        if(gbDevice!=null){
                            startService(new Intent(getContext(), HRMonitor.class).putExtra(GBDevice.EXTRA_DEVICE, gbDevice));
                        }
                    } catch (IllegalStateException e) {
                        String message = e.toString();
                        if (message == null) {
                            message = getString(R.string._unknown_);
                        }
                        GB.notify(NOTIFICATION_ID_ERROR,
                                new NotificationCompat.Builder(getContext(), NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                                        .setSmallIcon(R.drawable.gadgetbridge_img)
                                        .setContentTitle(getString(R.string.test))
                                        .setContentText(getString(R.string.test_notification))
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText(getString(R.string.start) + "\"" + message + "\""))
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        .build(), getContext());
                    }
                }
            });
            if((getPrefs().getString("emergency_hr_telno_cc1","")!="" && getPrefs().getString("emergency_hr_telno1","")!="") ||
                    (getPrefs().getString("emergency_hr_telno_cc1","")!="" || getPrefs().getString("emergency_hr_telno1","")!="") ) {
                LOG.debug("ACC ON:" +whatsappsupport.isAccessibilityOn());
                if (!whatsappsupport.isAccessibilityOn()) {
                    accessibilityPopup();
                }
            }
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

    @Override protected void onPause() {
        LOG.debug("HIDE THE WARNING SYSTEM");

        super.onPause();
    }

    @Override protected void onResume() {
        LOG.debug("SHOW THE WARNING SYSTEM");

        super.onResume();
    }


    private void accessibilityPopup(){
        LOG.debug("ACC ON:" +whatsappsupport.isAccessibilityOn());
        GB.toast(getBaseContext(), "Accessibility is not set for whatsapp messaging, please set first!", 5000, 0);
        Intent accessible = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        accessible.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(accessible);
    }

}
