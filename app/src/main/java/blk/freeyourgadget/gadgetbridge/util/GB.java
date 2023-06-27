/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniel Dakhno,
    Daniele Gobbetti, Felix Konstantin Maurer, Pauli Salmenrinne, Taavi Eomäe,
    Uwe Hermann, Yar

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package blk.freeyourgadget.gadgetbridge.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.GBEnvironment;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import blk.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.ActivityKind;
import blk.freeyourgadget.gadgetbridge.model.DeviceService;
import blk.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

import static blk.freeyourgadget.gadgetbridge.GBApplication.isRunningOreoOrLater;
import static blk.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_RECORDED_DATA_TYPES;

public class GB {

    public static final String NOTIFICATION_CHANNEL_ID = "gadgetbridge";
    public static final String NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID = "gadgetbridge_high_priority";
    public static final String NOTIFICATION_CHANNEL_ID_TRANSFER = "gadgetbridge transfer";
    public static final String NOTIFICATION_CHANNEL_ID_LOW_BATTERY = "low_battery";
    public static final String NOTIFICATION_CHANNEL_ID_GPS = "gps";

    public static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_ID_INSTALL = 2;
    public static final int NOTIFICATION_ID_LOW_BATTERY = 3;
    public static final int NOTIFICATION_ID_TRANSFER = 4;
    public static final int NOTIFICATION_ID_EXPORT_FAILED = 5;
    public static final int NOTIFICATION_ID_PHONE_FIND = 6;
    public static final int NOTIFICATION_ID_GPS = 7;
    public static final int NOTIFICATION_ID_ERROR = 42;

    private static final Logger LOG = LoggerFactory.getLogger(GB.class);
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    public static final String ACTION_DISPLAY_MESSAGE = "GB_Display_Message";
    public static final String DISPLAY_MESSAGE_MESSAGE = "message";
    public static final String DISPLAY_MESSAGE_DURATION = "duration";
    public static final String DISPLAY_MESSAGE_SEVERITY = "severity";

    /** Commands related to the progress (bar) on the screen */
    public static final String ACTION_SET_PROGRESS_BAR = "GB_Set_Progress_Bar";
    public static final String PROGRESS_BAR_INDETERMINATE = "indeterminate";
    public static final String PROGRESS_BAR_MAX = "max";
    public static final String PROGRESS_BAR_PROGRESS = "progress";
    public static final String ACTION_SET_PROGRESS_TEXT = "GB_Set_Progress_Text";
    public static final String ACTION_SET_INFO_TEXT = "GB_Set_Info_Text";

    private static boolean notificationChannelsCreated;

    private static final String TAG = "GB";

    public static void createNotificationChannels(Context context) {
        if (notificationChannelsCreated) return;

        if (isRunningOreoOrLater()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            NotificationChannel channelGeneral = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channelGeneral);

            NotificationChannel channelHighPriority = new NotificationChannel(
                    NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID,
                    context.getString(R.string.notification_channel_high_priority_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channelHighPriority);

            NotificationChannel channelTransfer = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_TRANSFER,
                    context.getString(R.string.notification_channel_transfer_name),
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channelTransfer);

            NotificationChannel channelLowBattery = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_LOW_BATTERY,
                    context.getString(R.string.notification_channel_low_battery_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channelLowBattery);

            NotificationChannel channelGps = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_GPS,
                    context.getString(R.string.notification_channel_gps),
                    NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(channelGps);
        }

        notificationChannelsCreated = true;
    }

    private static PendingIntent getContentIntent(Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        return pendingIntent;
    }

    public static Notification createNotification(List<GBDevice> devices, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        if(devices.size() == 0){
            builder.setContentTitle(context.getString(R.string.info_no_devices_connected))
                    .setSmallIcon(R.drawable.ic_notification_disconnected)
                    .setContentIntent(getContentIntent(context))
                    .setShowWhen(false)
                    .setOngoing(true);

            if (!GBApplication.isRunningTwelveOrLater()) {
                builder.setColor(context.getResources().getColor(R.color.accent));
            }
        }else if(devices.size() == 1) {
            GBDevice device = devices.get(0);
            String deviceName = device.getAliasOrName();
            String text = device.getStateString();
            if (device.getBatteryLevel() != GBDevice.BATTERY_UNKNOWN) {
                text += ": " + context.getString(R.string.battery) + " " + device.getBatteryLevel() + "%";
            }

            boolean connected = device.isInitialized();
            builder.setContentTitle(deviceName)
                    .setTicker(deviceName + " - " + text)
                    .setContentText(text)
                    .setSmallIcon(connected ? device.getNotificationIconConnected() : device.getNotificationIconDisconnected())
                    .setContentIntent(getContentIntent(context))
                    .setShowWhen(false)
                    .setOngoing(true);

            if (!GBApplication.isRunningTwelveOrLater()) {
                builder.setColor(context.getResources().getColor(R.color.accent));
            }

            Intent deviceCommunicationServiceIntent = new Intent(context, DeviceCommunicationService.class);
            if (connected) {
                deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_DISCONNECT);
                PendingIntent disconnectPendingIntent = PendingIntent.getService(context, 0, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
                builder.addAction(R.drawable.ic_notification_disconnected, context.getString(R.string.controlcenter_disconnect), disconnectPendingIntent);
                if (DeviceHelper.getInstance().getCoordinator(device).supportsActivityDataFetching()) {
                    deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_FETCH_RECORDED_DATA);
                    deviceCommunicationServiceIntent.putExtra(EXTRA_RECORDED_DATA_TYPES, ActivityKind.TYPE_ACTIVITY);
                    PendingIntent fetchPendingIntent = PendingIntent.getService(context, 1, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
                    builder.addAction(R.drawable.ic_refresh, context.getString(R.string.controlcenter_fetch_activity_data), fetchPendingIntent);
                }
            } else if (device.getState().equals(GBDevice.State.WAITING_FOR_RECONNECT) || device.getState().equals(GBDevice.State.NOT_CONNECTED)) {
                deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_CONNECT);
                deviceCommunicationServiceIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                PendingIntent reconnectPendingIntent = PendingIntent.getService(context, 2, deviceCommunicationServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_notification, context.getString(R.string.controlcenter_connect), reconnectPendingIntent);
            }
        }else{
            StringBuilder contentText = new StringBuilder();
            boolean isConnected = true;
            boolean anyDeviceSupportesActivityDataFetching = false;
            for(GBDevice device : devices){
                if(!device.isInitialized()){
                    isConnected = false;
                }

                anyDeviceSupportesActivityDataFetching |= DeviceHelper.getInstance().getCoordinator(device).supportsActivityDataFetching();

                String deviceName = device.getAliasOrName();
                String text = device.getStateString();
                if (device.getBatteryLevel() != GBDevice.BATTERY_UNKNOWN) {
                    text += ": " + context.getString(R.string.battery) + " " + device.getBatteryLevel() + "%";
                }
                contentText.append(deviceName).append(" (").append(text).append(")<br>");
            }

            SpannableString formated = new SpannableString(
                    Html.fromHtml(contentText.substring(0, contentText.length() - 4)) // cut away last <br>
            );

            String title = context.getString(R.string.info_connected_count, devices.size());

            builder.setContentTitle(title)
                    .setContentText(formated)
                    .setSmallIcon(isConnected ? R.drawable.ic_notification : R.drawable.ic_notification_disconnected)
                    .setContentIntent(getContentIntent(context))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(formated).setBigContentTitle(title))
                    .setShowWhen(false)
                    .setOngoing(true);

            if (!GBApplication.isRunningTwelveOrLater()) {
                builder.setColor(context.getResources().getColor(R.color.accent));
            }

            if (anyDeviceSupportesActivityDataFetching) {
                Intent deviceCommunicationServiceIntent = new Intent(context, DeviceCommunicationService.class);
                deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_FETCH_RECORDED_DATA);
                deviceCommunicationServiceIntent.putExtra(EXTRA_RECORDED_DATA_TYPES, ActivityKind.TYPE_ACTIVITY);
                PendingIntent fetchPendingIntent = PendingIntent.getService(context, 1, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
                builder.addAction(R.drawable.ic_refresh, context.getString(R.string.controlcenter_fetch_activity_data), fetchPendingIntent);
            }
        }

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (GBApplication.minimizeNotification()) {
            builder.setPriority(Notification.PRIORITY_MIN);
        }
        return builder.build();
    }

    public static Notification createNotification(String text, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setTicker(text)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification_disconnected)
                .setContentIntent(getContentIntent(context))
                .setShowWhen(false)
                .setOngoing(true);

        if (!GBApplication.isRunningTwelveOrLater()) {
            builder.setColor(context.getResources().getColor(R.color.accent));
        }

        if (GBApplication.getPrefs().getString("last_device_address", null) != null) {
            Intent deviceCommunicationServiceIntent = new Intent(context, DeviceCommunicationService.class);
            deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_CONNECT);
            PendingIntent reconnectPendingIntent = PendingIntent.getService(context, 2, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.drawable.ic_notification, context.getString(R.string.controlcenter_connect), reconnectPendingIntent);
        }

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (GBApplication.minimizeNotification()) {
            builder.setPriority(Notification.PRIORITY_MIN);
        }
        return builder.build();
    }

    public static void updateNotification(List<GBDevice> devices, Context context) {
        Notification notification = createNotification(devices, context);
        notify(NOTIFICATION_ID, notification, context);
    }

    public static void notify(int id, @NonNull Notification notification, Context context) {
        createNotificationChannels(context);

        NotificationManagerCompat.from(context).notify(id, notification);
    }

    public static void removeNotification(int id, Context context) {
        NotificationManagerCompat.from(context).cancel(id);
    }

    public static boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public static boolean supportsBluetoothLE() {
        return GBApplication.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    public static String hexdump(byte[] buffer, int offset, int length) {
        if (length == -1) {
            length = buffer.length - offset;
        }

        char[] hexChars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = buffer[i + offset] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String hexdump(byte[] buffer) {
        return hexdump(buffer, 0, buffer.length);
    }

    /**
     * https://stackoverflow.com/a/140861/4636860
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String formatRssi(short rssi) {
        return String.valueOf(rssi);
    }

    public static String writeScreenshot(GBDeviceEventScreenshot screenshot, String filename) throws IOException {

        LOG.info("Will write screenshot: " + screenshot.width + "x" + screenshot.height + "x" + screenshot.bpp + "bpp");
        final int FILE_HEADER_SIZE = 14;
        final int INFO_HEADER_SIZE = 40;

        File dir = FileUtils.getExternalFilesDir();
        File outputFile = new File(dir, filename);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            ByteBuffer headerbuf = ByteBuffer.allocate(FILE_HEADER_SIZE + INFO_HEADER_SIZE + screenshot.clut.length);
            headerbuf.order(ByteOrder.LITTLE_ENDIAN);

            // file header
            headerbuf.put((byte) 'B');
            headerbuf.put((byte) 'M');
            headerbuf.putInt(0); // size in bytes (uncompressed = 0)
            headerbuf.putInt(0); // reserved
            headerbuf.putInt(FILE_HEADER_SIZE + INFO_HEADER_SIZE + screenshot.clut.length);

            // info header
            headerbuf.putInt(INFO_HEADER_SIZE);
            headerbuf.putInt(screenshot.width);
            headerbuf.putInt(-screenshot.height);
            headerbuf.putShort((short) 1); // planes
            headerbuf.putShort((short) screenshot.bpp);
            headerbuf.putInt(0); // compression
            headerbuf.putInt(0); // length of pixeldata in bytes (uncompressed=0)
            headerbuf.putInt(0); // pixels per meter (x)
            headerbuf.putInt(0); // pixels per meter (y)
            headerbuf.putInt(screenshot.clut.length / 4); // number of colors in CLUT
            headerbuf.putInt(0); // numbers of used colors
            headerbuf.put(screenshot.clut);
            fos.write(headerbuf.array());
            int rowbytes = (screenshot.width * screenshot.bpp) / 8;
            byte[] pad = new byte[rowbytes % 4];
            for (int i = 0; i < screenshot.height; i++) {
                fos.write(screenshot.data, rowbytes * i, rowbytes);
                fos.write(pad);
            }
        }
        return outputFile.getAbsolutePath();
    }

    /**
     * Creates and display a Toast message using the application context.
     * Additionally the toast is logged using the provided severity.
     * Can be called from any thread.
     *
     * @param message     the message to display.
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     */
    public static void toast(String message, int displayTime, int severity) {
        toast(GBApplication.getContext(), message, displayTime, severity, null);
    }

    /**
     * Creates and display a Toast message using the application context.
     * Additionally the toast is logged using the provided severity.
     * Can be called from any thread.
     *
     * @param message     the message to display.
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     */
    public static void toast(String message, int displayTime, int severity, Throwable ex) {
        toast(GBApplication.getContext(), message, displayTime, severity, ex);
    }

    /**
     * Creates and display a Toast message using the application context
     * Can be called from any thread.
     *
     * @param context     the context to use
     * @param message     the message to display
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     */
    public static void toast(final Context context, final String message, final int displayTime, final int severity) {
        toast(context, message, displayTime, severity, null);
    }

    /**
     * Creates and display a Toast message using the application context
     * Can be called from any thread.
     *
     * @param context     the context to use
     * @param message     the message to display
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     * @param ex          optional exception to be logged
     */
    public static void toast(final Context context, final String message, final int displayTime, final int severity, final Throwable ex) {
        log(message, severity, ex); // log immediately, not delayed
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }
        Looper mainLooper = Looper.getMainLooper();
        if (Thread.currentThread() == mainLooper.getThread()) {
            Toast.makeText(context, message, displayTime).show();
        } else {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, displayTime).show();
                }
            };

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(runnable);
            } else {
                new Handler(mainLooper).post(runnable);
            }
        }
    }

    public static void log(String message, int severity, Throwable ex) {
        switch (severity) {
            case INFO:
                LOG.info(message, ex);
                break;
            case WARN:
                LOG.warn(message, ex);
                break;
            case ERROR:
                LOG.error(message, ex);
                break;
        }
    }

    private static Notification createTransferNotification(String title, String text, boolean ongoing,
                                                           int percentage, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_TRANSFER)
                .setTicker((title == null) ? context.getString(R.string.app_name) : title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle((title == null) ? context.getString(R.string.app_name) : title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);

        if (ongoing) {
            nb.setProgress(100, percentage, percentage == 0);
            nb.setSmallIcon(android.R.drawable.stat_sys_download);
        } else {
            nb.setProgress(0, 0, false);
            nb.setSmallIcon(android.R.drawable.stat_sys_download_done);
        }

        return nb.build();
    }

    public static void updateTransferNotification(String title, String text, boolean ongoing, int percentage, Context context) {
        if (percentage == 100) {
            removeNotification(NOTIFICATION_ID_TRANSFER, context);
        } else {
            Notification notification = createTransferNotification(title, text, ongoing, percentage, context);
            notify(NOTIFICATION_ID_TRANSFER, notification, context);
        }
    }

    public static void createGpsNotification(Context context, int numDevices) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_GPS)
                .setTicker(context.getString(R.string.notification_gps_title))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(context.getString(R.string.notification_gps_title))
                .setContentText(context.getString(R.string.notification_gps_text, numDevices))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_gps_location)
                .setOngoing(true);

        notify(NOTIFICATION_ID_GPS, nb.build(), context);
    }

    public static void removeGpsNotification(Context context) {
        removeNotification(NOTIFICATION_ID_GPS, context);
    }

    private static Notification createInstallNotification(String text, boolean ongoing,
                                                          int percentage, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setTicker(text)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);

        if (ongoing) {
            nb.setProgress(100, percentage, percentage == 0);
            nb.setSmallIcon(android.R.drawable.stat_sys_upload);

        } else {
            nb.setSmallIcon(android.R.drawable.stat_sys_upload_done);
        }

        return nb.build();
    }

    public static void updateInstallNotification(String text, boolean ongoing, int percentage, Context context) {
        Notification notification = createInstallNotification(text, ongoing, percentage, context);
        notify(NOTIFICATION_ID_INSTALL, notification, context);
    }

    private static Notification createBatteryNotification(String text, String bigText, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_LOW_BATTERY)
                .setContentTitle(context.getString(R.string.notif_battery_low_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_low_battery)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(false);

        if (bigText != null) {
            nb.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        }

        return nb.build();
    }

    public static void updateBatteryNotification(String text, String bigText, Context context) {
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }
        Notification notification = createBatteryNotification(text, bigText, context);
        notify(NOTIFICATION_ID_LOW_BATTERY, notification, context);
    }

    public static void removeBatteryNotification(Context context) {
        removeNotification(NOTIFICATION_ID_LOW_BATTERY, context);
    }

    public static Notification createExportFailedNotification(String text, Context context) {
        Intent notificationIntent = new Intent(context, SettingsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notif_export_failed_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(false);

        return nb.build();
    }

    public static void updateExportFailedNotification(String text, Context context) {
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }
        Notification notification = createExportFailedNotification(text, context);
        notify(NOTIFICATION_ID_EXPORT_FAILED, notification, context);
    }

    public static void assertThat(boolean condition, String errorMessage) {
        if (!condition) {
            throw new AssertionError(errorMessage);
        }
    }

    public static void signalActivityDataFinish() {
        Intent intent = new Intent(GBApplication.ACTION_NEW_DATA);
        LocalBroadcastManager.getInstance(GBApplication.getContext()).sendBroadcast(intent);
    }

    public static boolean checkPermission(final Context context, final String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}