package blk.freeyourgadget.gadgetbridge.activities;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;
import static blk.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;
import static blk.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_ID_ERROR;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;

import blk.freeyourgadget.gadgetbridge.databinding.ActivityHeartrateEmergencyBinding;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.ActivityUser;
import blk.freeyourgadget.gadgetbridge.model.DeviceService;
import blk.freeyourgadget.gadgetbridge.service.emergencyhrsend.HRMonitor;
import blk.freeyourgadget.gadgetbridge.service.emergencyhrsend.WhatsappSupport;
import blk.freeyourgadget.gadgetbridge.util.GB;

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
    Map<String, Number> heart_rate_threshold = new HRMonitor().updateHeartRateThreshold();
    boolean IS_ACTIVITY_RUNNING = new HRMonitor().isRunning();

//TODO: WHY IT CANNOT UPDATE?
    String IS_SERVICE_RUNNING()
    {
        LOG.debug("RUNNING "+String.valueOf(IS_ACTIVITY_RUNNING));
        if (IS_ACTIVITY_RUNNING == true) {
            return getString(R.string.start);
        } else {
            return getString(R.string.stop);
        }
    }
    ActivityHeartrateEmergencyBinding binding;
    IntentFilter filter = new IntentFilter();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        getContext().registerReceiver(mReceiver, filter);//background update alongside check the service status

        final Context appContext = this.getApplicationContext();

        if (appContext instanceof GBApplication) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_heartrate_emergency);

            /*
            binding =ActivityHeartrateEmergencyBinding.inflate(getLayoutInflater());
            setContentView(R.layout.activity_heartrate_emergency);
            */
        }
        textHR = findViewById(R.id.textHR);
        textFlavourHRLimit = findViewById(R.id.textFlavourHRLimit);
        textFlavourHRStatus = findViewById(R.id.textFlavourHRStatus);
        btnStartHR = findViewById(R.id.btnStartHR);
        btnStopHR = findViewById(R.id.btnStopHR);
        progressHR = findViewById(R.id.progressHR);
        //enable broadcast
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

            btnStartHR.setText(getBaseContext().getString(R.string.start));
            btnStopHR.setText(getBaseContext().getString(R.string.stop));
            btnStartHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    GB.toast(getBaseContext(), getString(R.string.start) + " " +getString(R.string.pref_title_emergency_hr), 2000, 0);
                    try {
                        //the following will start activity
                        if(gbDevice!=null){
                            startService(new Intent(getContext(), HRMonitor.class).putExtra(GBDevice.EXTRA_DEVICE, gbDevice));
                        }
                    } catch (IllegalStateException e) {
                        String message = e.toString();
                        if (message == null) {
                            message = (getString(R.string.error_notification));
                        }
                        GB.notify(NOTIFICATION_ID_ERROR,
                                new NotificationCompat.Builder(getContext(), NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                                        .setSmallIcon(R.drawable.gadgetbridge_img)
                                        .setContentTitle(getString(R.string.error))
                                        .setContentText(message)
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText(getString(R.string.start) + "\"" + message + "\""))
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        .build(), getContext());
                    }
                }
            });
            btnStopHR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //enable broadcast if not detected, !receiverCheck meaning receiverCheck is false / nonexistent due to exception caused.
                    //receiverCheck meaning it's true / existent.
                        GB.toast(getBaseContext(), getString(R.string.stop) + " " +getString(R.string.pref_title_emergency_hr), 2000, 0);
                    try {
                        //the following will stops activity
                        if(gbDevice!=null){
                            stopService(new Intent(getContext(), HRMonitor.class));
                        }
                    } catch (IllegalStateException e) {
                        String message = e.toString();
                        if (message == null) {
                            message = (getString(R.string.error_notification));
                        }
                        GB.notify(NOTIFICATION_ID_ERROR,
                                new NotificationCompat.Builder(getContext(), NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                                        .setSmallIcon(R.drawable.gadgetbridge_img)
                                        .setContentTitle(getString(R.string.error))
                                        .setContentText(message)
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
                    DialogFragment dialog = new AccesibilityDialogFragment();
                    dialog.show(getSupportFragmentManager(), "AccesibilityDialogFragment");
                }
            }
        }
        else if (getPrefs().getBoolean("pref_emergency_hr_enable",false) == false) {

        }

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
        if (result instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) result;
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())){
                progressHR.setVisibility(View.GONE);
                textHR.setText(String.valueOf(sample.getHeartRate()));
                textFlavourHRStatus.setText(IS_SERVICE_RUNNING());
            }
        }
    }

        @Override
    protected void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
            getContext().unregisterReceiver(mReceiver);
        }catch(Exception e){LOG.debug (e.toString());}
        super.onDestroy();
    }


    public static class AccesibilityDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_notification_accesibility,
                            context.getString(R.string.app_name),
                            context.getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            accessibilityPopup();
                        }
                    }).setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {}
                    });
            return builder.create();
        }
    }

    @Override protected void onPause() {
        LOG.debug("HIDE THE WARNING SYSTEM");try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
            getContext().unregisterReceiver(mReceiver);
        }catch(Exception e){LOG.debug (e.toString());}
        super.onPause();
    }

    @Override protected void onResume() {
        LOG.debug("SHOW THE WARNING SYSTEM");
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        getContext().registerReceiver(mReceiver, filter);//background update alongside check the service status
        super.onResume();
    }


    private static void accessibilityPopup(){
        //LOG.debug("ACC ON:" +whatsappsupport.isAccessibilityOn());
        Intent accessible = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        accessible.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(accessible);
    }

}
