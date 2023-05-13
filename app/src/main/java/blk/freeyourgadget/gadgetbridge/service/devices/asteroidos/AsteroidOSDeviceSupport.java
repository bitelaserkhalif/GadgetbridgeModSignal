package blk.freeyourgadget.gadgetbridge.service.devices.asteroidos;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import blk.freeyourgadget.gadgetbridge.devices.asteroidos.AsteroidOSConstants;
import blk.freeyourgadget.gadgetbridge.devices.asteroidos.AsteroidOSMediaCommand;
import blk.freeyourgadget.gadgetbridge.devices.asteroidos.AsteroidOSNotification;
import blk.freeyourgadget.gadgetbridge.devices.asteroidos.AsteroidOSWeather;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.CallSpec;
import blk.freeyourgadget.gadgetbridge.model.MusicSpec;
import blk.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import blk.freeyourgadget.gadgetbridge.model.NotificationSpec;
import blk.freeyourgadget.gadgetbridge.model.WeatherSpec;
import blk.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import blk.freeyourgadget.gadgetbridge.service.btle.GattService;
import blk.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import blk.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import blk.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import blk.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;

public class AsteroidOSDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AsteroidOSDeviceSupport.class);
    private final BatteryInfoProfile<AsteroidOSDeviceSupport> batteryInfoProfile;
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public AsteroidOSDeviceSupport() {
        super(LOG);
        addSupportedService(AsteroidOSConstants.SERVICE_UUID);
        addSupportedService(AsteroidOSConstants.TIME_SERVICE_UUID);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(AsteroidOSConstants.WEATHER_SERVICE_UUID);
        addSupportedService(AsteroidOSConstants.NOTIFICATION_SERVICE_UUID);
        addSupportedService(AsteroidOSConstants.MEDIA_SERVICE_UUID);

        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                    handleBatteryInfo((blk.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));

                }
            }
        };

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
    }

    private void handleBatteryInfo(blk.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        batteryCmd.level = info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        byte[] value = characteristic.getValue();

        if (characteristicUUID.equals(AsteroidOSConstants.MEDIA_COMMANDS_CHAR)) {
            handleMediaCommand(gatt, characteristic);
            return true;
        }

        LOG.info("Characteristic changed UUID: " + characteristicUUID);
        LOG.info("Characteristic changed value: " + characteristic.getValue());
        return false;
    }


    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        builder.notify(getCharacteristic(AsteroidOSConstants.MEDIA_COMMANDS_CHAR), true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.requestMtu(256);
        }
        return builder;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        AsteroidOSNotification notif = new AsteroidOSNotification(notificationSpec);
        TransactionBuilder builder = new TransactionBuilder("send notification");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.NOTIFICATION_UPDATE_CHAR, notif.toString().getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onDeleteNotification(int id) {
        AsteroidOSNotification notif = new AsteroidOSNotification(id);
        TransactionBuilder builder = new TransactionBuilder("delete notification");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.NOTIFICATION_UPDATE_CHAR, notif.toString().getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onSetTime() {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        Date nowTime = now.getTime();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) nowTime.getYear());
        baos.write((byte) nowTime.getMonth());
        baos.write((byte) nowTime.getDay() + 1);
        baos.write((byte) nowTime.getHours());
        baos.write((byte) nowTime.getMinutes());
        baos.write((byte) nowTime.getSeconds());
        TransactionBuilder builder = new TransactionBuilder("set time");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.TIME_SET_CHAR, baos.toByteArray());
        builder.queue(getQueue());
    }


    @Override
    public void onSetCallState(CallSpec callSpec) {
        AsteroidOSNotification call = new AsteroidOSNotification(callSpec);
        TransactionBuilder builder = new TransactionBuilder("send call");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.NOTIFICATION_UPDATE_CHAR, call.toString().getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        TransactionBuilder builder = new TransactionBuilder("set music state");
        if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_PLAYING_CHAR, new byte[]{1});
        } else {
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_PLAYING_CHAR, new byte[]{0});
        }
        builder.queue(getQueue());
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        TransactionBuilder builder = new TransactionBuilder("send music information");
        // Send title
        safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_TITLE_CHAR, musicSpec.track.getBytes(StandardCharsets.UTF_8));
        // Send album
        safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_ALBUM_CHAR, musicSpec.album.getBytes(StandardCharsets.UTF_8));
        // Send artist
        safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_ARTIST_CHAR, musicSpec.artist.getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onFindDevice(boolean start) {
        final CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Gadgetbridge";
        onSetCallState(callSpec);
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        AsteroidOSWeather asteroidOSWeather = new AsteroidOSWeather(weatherSpec);
        TransactionBuilder builder = new TransactionBuilder("send weather info");
        // Send city name
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_CITY_CHAR, asteroidOSWeather.getCityName());
        // Send conditions
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_IDS_CHAR, asteroidOSWeather.getWeatherConditions());
        // Send min temps
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_MIN_TEMPS_CHAR, asteroidOSWeather.getMinTemps());
        // Send max temps
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_MAX_TEMPS_CHAR, asteroidOSWeather.getMaxTemps());
        // Flush queue
        builder.queue(getQueue());
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    /**
     * This will check if the characteristic exists and can be written
     * <p>
     * Keeps backwards compatibility with firmware that can't take all the information
     */
    private void safeWriteToCharacteristic(TransactionBuilder builder, UUID uuid, byte[] data) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);
        if (characteristic != null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            builder.write(characteristic, data);
        } else {
            LOG.warn("Tried to write to a characteristic that did not exist or was not writable!");
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        if (super.onCharacteristicRead(gatt, characteristic, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }


    /**
     * Handles a media command sent from the AsteroidOS device
     * @param gatt The bluetooth device's GATT info
     * @param characteristic The Characteristic information
     */
    public void handleMediaCommand (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        LOG.info("handle media command");
        AsteroidOSMediaCommand command = new AsteroidOSMediaCommand(characteristic.getValue()[0]);
        GBDeviceEventMusicControl event = command.toMusicControlEvent();
        evaluateGBDeviceEvent(event);
    }

}
