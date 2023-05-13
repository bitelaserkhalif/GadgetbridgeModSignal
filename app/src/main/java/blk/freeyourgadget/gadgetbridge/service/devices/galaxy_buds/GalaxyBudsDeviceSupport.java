package blk.freeyourgadget.gadgetbridge.service.devices.galaxy_buds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blk.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import blk.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class GalaxyBudsDeviceSupport extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyBudsDeviceSupport.class);


    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        super.onTestNewFunction();
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public synchronized GalaxyBudsIOThread getDeviceIOThread() {
        return (GalaxyBudsIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected GBDeviceProtocol createDeviceProtocol() {
        return new GalaxyBudsProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new GalaxyBudsIOThread(getDevice(), getContext(), (GalaxyBudsProtocol) getDeviceProtocol(),
                GalaxyBudsDeviceSupport.this, getBluetoothAdapter());
    }
}
