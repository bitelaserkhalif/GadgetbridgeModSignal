package blk.freeyourgadget.gadgetbridge.service.emergencyhrsend;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;
import static blk.freeyourgadget.gadgetbridge.GBApplication.isRunningOreoOrLater;

import static blk.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CONNECT_FIRST_TIME;
import static blk.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;
import static blk.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import blk.freeyourgadget.gadgetbridge.BR;
import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.EmergencyHRActivity;
import blk.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.ActivityUser;
import blk.freeyourgadget.gadgetbridge.model.DeviceService;
import blk.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class HRMonitor extends Service {
    public static final int NOTIFICATION_ID_HR = 8;
    public static final String NOTIFICATION_ID_HR_STRING = "gadgetbridge hr";
    public static final int NOTIFICATION_ID_HR_DANGER = 10;
    public static final String NOTIFICATION_ID_HR_DANGER_STRING = "gadgetbridge hr danger";
    Map<String, Number> heart_rate_threshold = updateHeartRateThreshold();

    public static final String P_HR_SMART;
    public static Boolean mStarted = false;
    public static final String P_HR_MANUAL;
    public static final String P_HR_READMAX;
    private static boolean IS_ACTIVITY_RUNNING;// this variable for determining is this service running?...
    public static boolean isRunning() {
        return IS_ACTIVITY_RUNNING;
    }


    private static boolean notificationChannelsCreated;
    private static final String EXTRA_REPLY = "reply";
    private static final String ACTION_REPLY
            = "blk.freeyourgadget.gadgetbridge.DebugActivity.action.reply";

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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_REPLY: {
                    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                    CharSequence reply = remoteInput.getCharSequence(EXTRA_REPLY);
                    LOG.info("got wearable reply: " + reply);
                    GB.toast(context, "got wearable reply: " + reply, Toast.LENGTH_SHORT, GB.INFO);
                    break;
                }
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    handleRealtimeSample(intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                default:
                    LOG.info("ignoring intent action " + intent.getAction());
                    break;
            }
        }
    };

    private void handleRealtimeSample(Serializable result) {
        if (result instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) result;
            //GB.toast(this, "Heart Rate measured: " + sample.getHeartRate(), Toast.LENGTH_LONG, GB.INFO);
            LOG.info("Emergency Heart Rate measured: " + sample.getHeartRate());
            LOG.info("Entire sample: " + sample.toString());
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())){
                if (sample.getHeartRate() > heart_rate_threshold.get("maxHeartRateValue").intValue()){
                    updateNotification(getString(R.string.emergencyhr_detail_activity_title), this,NOTIFICATION_ID_HR,getString(R.string.emergency_hr_anomaly_high)+" "+sample.getHeartRate());
                    GB.toast(getBaseContext(), (getBaseContext().getString(R.string.emergency_hr_anomaly_high)), 2000, GB.WARN);
                    emergencyWarning(getBaseContext().getString(R.string.emergency_hr_anomaly_high));
                    LOG.debug("STOPPING");//STOP THE SERVICE
                    this.stopSelf();
                    //HIGH HEART RATE
                }
                else if (sample.getHeartRate() < heart_rate_threshold.get("minHeartRateValue").intValue()){
                    updateNotification(getString(R.string.emergencyhr_detail_activity_title), this,NOTIFICATION_ID_HR,getString(R.string.emergency_hr_anomaly_low)+" "+sample.getHeartRate());
                    GB.toast(getBaseContext(), (getBaseContext().getString(R.string.emergency_hr_anomaly_low)), 2000, GB.WARN);
                    emergencyWarning(getBaseContext().getString(R.string.emergency_hr_anomaly_low));
                    LOG.debug("STOPPING");//STOP THE SERVICE
                    this.stopSelf();
                    //LOW HEART RATE
                }
                else{
                    updateNotification(getString(R.string.emergencyhr_detail_activity_title), this,NOTIFICATION_ID_HR,getString(R.string.emergencyhr_detail_activity_title)+" "+sample.getHeartRate());
                }
            }
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
            }
            else if (whatsappsupport.isAccessibilityOn() && !player.isPlaying()){//stops spam
                whatsappsupport.sendWAEmergency(getPrefs().getString("emergency_hr_telno_cc1", ""), getPrefs().getString("emergency_hr_telno1", ""), lastKnownLocation, HR,true);
                player.start();
                //finish();//prevent double, workaround
            }
        }

        //textHR.setText("!!!");

        //finish();//prevent double, workaround
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        registerReceiver(mReceiver, filter);
        IS_ACTIVITY_RUNNING=true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
        IS_ACTIVITY_RUNNING=false;
    }
    private void start() {
        if(!mStarted){
            createNotificationChannels(getContext());
            startForeground(NOTIFICATION_ID_HR, this.createNotification(getString(R.string.emergencyhr_detail_activity_title), this,getString(R.string.emergencyhr_detail_activity_title)));
            mStarted = true;
        }
    }

    public static void createNotificationChannels(Context context) {
        if (notificationChannelsCreated) return;

        if (isRunningOreoOrLater()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            NotificationChannel channelGeneral = new NotificationChannel(
                    NOTIFICATION_ID_HR_STRING,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channelGeneral);

            NotificationChannel channelHighPriority = new NotificationChannel(
                    NOTIFICATION_ID_HR_DANGER_STRING,
                    context.getString(R.string.notification_channel_high_priority_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channelHighPriority);

        }

        notificationChannelsCreated = true;
    }
    public boolean isStarted() {
        return mStarted;
    }

    public Notification createNotification(List<GBDevice> devices, Context context)
    {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_ID_HR_STRING);


        builder.setDefaults(Notification.DEFAULT_LIGHTS);

        String message = "HR Service Test";
        builder.setSmallIcon(R.drawable.ic_heart_alert)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#0f9595"))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message);

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    public Notification createNotification(String text, Context context, String message)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_ID_HR_STRING);
        builder.setDefaults(Notification.DEFAULT_LIGHTS);


        builder.setSmallIcon(R.drawable.ic_heart_alert)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#0f9595"))
                .setTicker(text)
                .setContentTitle(text)
                .setContentText(message);

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    public void updateNotification(String text, Context context, int notification_id, String message) {
        Notification notification = createNotification(text, context, message);
        notify(notification_id, notification, context);
    }

    public static void notify(int id, @NonNull Notification notification, Context context) {
        createNotificationChannels(context);

        NotificationManagerCompat.from(context).notify(id, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Prefs prefs = getPrefs();
        LOG.debug(action);
        LOG.debug("STARTING EMERGENCY HR!");
        start();

        /*
        GBDevice gbDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
        String btDeviceAddress = null;
        LOG.debug(gbDevice.toString());


        if (action == null) {
            LOG.info("no action");
            return START_NOT_STICKY;
        }
        switch (action) {
            
        }*/
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
