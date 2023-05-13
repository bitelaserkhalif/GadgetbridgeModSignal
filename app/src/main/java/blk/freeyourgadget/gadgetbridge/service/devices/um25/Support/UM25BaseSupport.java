package blk.freeyourgadget.gadgetbridge.service.devices.um25.Support;

import org.slf4j.Logger;

import blk.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;

public class UM25BaseSupport extends AbstractBTLEDeviceSupport {
    public UM25BaseSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
