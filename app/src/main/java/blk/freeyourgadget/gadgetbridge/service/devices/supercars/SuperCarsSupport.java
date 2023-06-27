package blk.freeyourgadget.gadgetbridge.service.devices.supercars;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import blk.freeyourgadget.gadgetbridge.devices.supercars.SuperCarsConstants;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.BatteryState;
import blk.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import blk.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import blk.freeyourgadget.gadgetbridge.util.CryptoUtils;

public class SuperCarsSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SuperCarsSupport.class);
    public static final String COMMAND_DRIVE_CONTROL = "blk.freeyourgadget.gadgetbridge.supercars.command.DRIVE_CONTROL";
    public static final String EXTRA_DIRECTION = "EXTRA_DIRECTION";
    public static final String EXTRA_MOVEMENT = "EXTRA_MOVEMENT";
    public static final String EXTRA_SPEED = "EXTRA_SPEED";
    public static final String EXTRA_LIGHT = "EXTRA_LIGHT";

    public SuperCarsSupport() {
        super(LOG);
        addSupportedService(SuperCarsConstants.SERVICE_UUID_FFF);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(SuperCarsConstants.CHARACTERISTIC_UUID_FFF4), true); //for battery
        builder.setGattCallback(this);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(COMMAND_DRIVE_CONTROL);
        broadcastManager.registerReceiver(commandReceiver, filter);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        LOG.debug("Connected to: " + gbDevice.getName());
        return builder;
    }

    BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(COMMAND_DRIVE_CONTROL)) {
                queueDataToBLE(
                        (SuperCarsConstants.Speed) intent.getSerializableExtra(EXTRA_SPEED),
                        (SuperCarsConstants.Movement) intent.getSerializableExtra(EXTRA_MOVEMENT),
                        (SuperCarsConstants.Light) intent.getSerializableExtra(EXTRA_LIGHT),
                        (SuperCarsConstants.Direction) intent.getSerializableExtra(EXTRA_DIRECTION)
                );
            }
        }
    };

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] byte_data = characteristic.getValue();
        LOG.debug("got characteristics: " + characteristicUUID);
        /*
        //keep here for now. This requires descriptor reading
        if (getDevice().getFirmwareVersion() == "N/A") {
            // this is probably not the best way to retrieve UUID descriptor
            // but i have not found a better way at this moment
            BluetoothGattCharacteristic characteristic1 = getCharacteristic(SuperCarsConstants.CHARACTERISTIC_UUID_FFF1);
            BluetoothGattDescriptor descriptor1 = characteristic1.getDescriptor(GattDescriptor.UUID_DESCRIPTOR_GATT_CHARACTERISTIC_USER_DESCRIPTION);
            gatt.readDescriptor(descriptor1);
            getDevice().setFirmwareVersion(new String(descriptor1.getValue()));
            getDevice().sendDeviceUpdateIntent(getContext());
        } else if (getDevice().getFirmwareVersion2() == "N/A") {
            BluetoothGattCharacteristic characteristic2 = getCharacteristic(SuperCarsConstants.CHARACTERISTIC_UUID_FFF2);
            BluetoothGattDescriptor descriptor2 = characteristic2.getDescriptor(GattDescriptor.UUID_DESCRIPTOR_GATT_CHARACTERISTIC_USER_DESCRIPTION);
            gatt.readDescriptor(descriptor2);
            //getDevice().setFirmwareVersion2(new String(descriptor2.getValue()));
            //getDevice().sendDeviceUpdateIntent(getContext());
        }
        */

        if (SuperCarsConstants.CHARACTERISTIC_UUID_FFF4.equals(characteristicUUID)) {
            byte[] decoded_data = decryptData(byte_data);

            if (decoded_data.length == 16) {
                GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
                batteryEvent.state = BatteryState.BATTERY_NORMAL;
                int level = decoded_data[4];
                batteryEvent.level = level;
                evaluateGBDeviceEvent(batteryEvent);
            }
        }
        return true;
    }

    private byte[] decryptData(byte[] data) {
        byte[] decodedResult = new byte[0];
        try {
            decodedResult = CryptoUtils.decryptAES(data, SuperCarsConstants.aes_key);
        } catch (Exception e) {
            LOG.error("Error while decoding received data: " + e);
        }
        return decodedResult;
    }

    private byte[] encryptData(byte[] data) {
        byte[] encodedResult = new byte[0];
        try {
            encodedResult = CryptoUtils.encryptAES(data, SuperCarsConstants.aes_key);
        } catch (Exception e) {
            LOG.error("Error while encoding data to be sent: " + e);
        }
        return encodedResult;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void queueDataToBLE(SuperCarsConstants.Speed speed,
                                SuperCarsConstants.Movement movement,
                                SuperCarsConstants.Light light,
                                SuperCarsConstants.Direction direction) {

        byte[] command = craft_packet(speed, direction, movement, light);
        TransactionBuilder builder = new TransactionBuilder("send data");
        BluetoothGattCharacteristic writeCharacteristic = getCharacteristic(SuperCarsConstants.CHARACTERISTIC_UUID_FFF1);
        builder.write(writeCharacteristic, encryptData(command));
        builder.queue(getQueue());
    }

    private byte[] craft_packet(SuperCarsConstants.Speed speed,
                                SuperCarsConstants.Direction direction,
                                SuperCarsConstants.Movement movement,
                                SuperCarsConstants.Light light) {

        byte[] packet = new byte[]{0x0, 0x43, 0x54, 0x4c, 0x0, 0x0, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};

        switch (movement) {
            case UP:
                packet[4] = 0x1;
                break;
            case DOWN:
                packet[5] = 0x1;
                break;
        }
        switch (direction) {
            case LEFT:
                packet[6] = 0x1;
                break;
            case RIGHT:
                packet[7] = 0x1;
                break;
        }
        switch (light) {
            case ON:
                packet[8] = 0x0;
                break;
            case OFF:
                packet[8] = 0x1;
                break;
        }
        switch (speed) {
            case NORMAL:
                packet[9] = 0x50;
                break;
            case TURBO:
                packet[9] = 0x64;
                break;
        }
        return packet;
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
    }

}
