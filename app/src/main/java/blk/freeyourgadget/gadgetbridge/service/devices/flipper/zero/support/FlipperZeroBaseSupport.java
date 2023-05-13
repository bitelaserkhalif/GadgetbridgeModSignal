package blk.freeyourgadget.gadgetbridge.service.devices.flipper.zero.support;

import org.slf4j.LoggerFactory;

import blk.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;

public class FlipperZeroBaseSupport extends AbstractBTLEDeviceSupport {
    public FlipperZeroBaseSupport() {
        super(LoggerFactory.getLogger(FlipperZeroBaseSupport.class));
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
