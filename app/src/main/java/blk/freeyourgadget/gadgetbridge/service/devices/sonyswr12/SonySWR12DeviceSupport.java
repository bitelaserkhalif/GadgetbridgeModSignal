/*  Copyright (C) 2020-2021 opavlov

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
package blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import blk.freeyourgadget.gadgetbridge.database.DBHelper;
import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.Alarm;
import blk.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.btle.GattService;
import blk.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import blk.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import blk.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import blk.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventBase;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventFactory;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm.BandAlarm;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm.BandAlarms;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control.CommandCode;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control.ControlPointLowVibration;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control.ControlPointWithValue;
import blk.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.time.BandTime;
import blk.freeyourgadget.gadgetbridge.util.GB;

// done:
// - time sync
// - alarms (also smart)
// - fetching activity(walking, sleep)
// - stamina mode
// - vibration intensity
// - realtime heart rate
// todo options:
// - "get moving"
// - get notified: -call, -notification, -notification from, -do not disturb
// - media control: media/find phone(tap once for play pause, tap twice for next, tap triple for previous)

public class SonySWR12DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SonySWR12DeviceSupport.class);
    private SonySWR12HandlerThread processor = null;

    private final BatteryInfoProfile<SonySWR12DeviceSupport> batteryInfoProfile;
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            if (intent.getAction().equals(BatteryInfoProfile.ACTION_BATTERY_INFO)) {
                BatteryInfo info = intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO);
                GBDeviceEventBatteryInfo gbInfo = new GBDeviceEventBatteryInfo();
                gbInfo.level = (short) info.getPercentCharged();
                handleGBDeviceEvent(gbInfo);
            }
        }
    };

    public SonySWR12DeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(SonySWR12Constants.UUID_SERVICE_AHS);
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        initialize();
        setTime(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        return builder;
    }

    private SonySWR12HandlerThread getProcessor() {
        if (processor == null) {
            processor = new SonySWR12HandlerThread(getDevice(), getContext());
            processor.start();
        }
        return processor;
    }

    private void initialize() {
        if (gbDevice.getState() != GBDevice.State.INITIALIZED) {
            gbDevice.setFirmwareVersion("N/A");
            gbDevice.setFirmwareVersion2("N/A");
            gbDevice.setState(GBDevice.State.INITIALIZED);
            gbDevice.sendDeviceUpdateIntent(getContext());
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            setTime(builder);
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void setTime(TransactionBuilder builder) {
        BluetoothGattCharacteristic timeCharacteristic = getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_TIME);
        builder.write(timeCharacteristic, new BandTime(Calendar.getInstance()).toByteArray());
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            BluetoothGattCharacteristic alarmCharacteristic = getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_ALARM);
            TransactionBuilder builder = performInitialized("alarm");
            int prefInterval = Integer.valueOf(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                    .getString(DeviceSettingsPreferenceConst.PREF_SONYSWR12_SMART_INTERVAL, "0"));
            ArrayList<BandAlarm> bandAlarmList = new ArrayList<>();
            for (Alarm alarm : alarms) {
                BandAlarm bandAlarm = BandAlarm.fromAppAlarm(alarm, bandAlarmList.size(), alarm.getSmartWakeup() ? prefInterval : 0);
                if (bandAlarm != null)
                    bandAlarmList.add(bandAlarm);
            }
            builder.write(alarmCharacteristic, new BandAlarms(bandAlarmList).toByteArray());
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting alarms: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        return super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic))
            return true;
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(SonySWR12Constants.UUID_CHARACTERISTIC_EVENT)) {
            try {
                EventBase event = EventFactory.readEventFromByteArray(characteristic.getValue());
                getProcessor().process(event);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            TransactionBuilder builder = performInitialized("fetchActivity");
            builder.notify(getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_EVENT), true);
            ControlPointWithValue flushControl = new ControlPointWithValue(CommandCode.FLUSH_ACTIVITY, 0);
            builder.write(getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT), flushControl.toByteArray());
            builder.queue(getQueue());
        } catch (Exception e) {
            LOG.error("failed to fetch activity data", e);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.notify(getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_EVENT), enable);
            ControlPointWithValue controlPointHeart = new ControlPointWithValue(CommandCode.HEARTRATE_REALTIME, enable ? 1 : 0);
            builder.write(getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT), controlPointHeart.toByteArray());
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read heart rate from Sony device", ex);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        try {
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_SONYSWR12_STAMINA: {
                    //stamina can be:
                    //disabled = 0, enabled = 1 or todo auto on low battery = 2
                    int status = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(config, false) ? 1 : 0;
                    TransactionBuilder builder = performInitialized(config);
                    ControlPointWithValue vibrationControl = new ControlPointWithValue(CommandCode.STAMINA_MODE, status);
                    builder.write(getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT), vibrationControl.toByteArray());
                    builder.queue(getQueue());
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_SONYSWR12_LOW_VIBRATION: {
                    boolean isEnabled = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(config, false);
                    TransactionBuilder builder = performInitialized(config);
                    ControlPointLowVibration vibrationControl = new ControlPointLowVibration(isEnabled);
                    builder.write(getCharacteristic(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT), vibrationControl.toByteArray());
                    builder.queue(getQueue());
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_SONYSWR12_SMART_INTERVAL: {
                    onSetAlarms(new ArrayList(DBHelper.getAlarms(gbDevice)));
                }
            }
        } catch (Exception exc) {
            LOG.error("failed to send config " + config, exc);
        }
    }
}
