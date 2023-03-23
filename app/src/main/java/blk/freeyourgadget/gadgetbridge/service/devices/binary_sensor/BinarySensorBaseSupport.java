package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor;

import org.slf4j.Logger;

import blk.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;

public class BinarySensorBaseSupport extends AbstractBTLEDeviceSupport {
    public BinarySensorBaseSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
