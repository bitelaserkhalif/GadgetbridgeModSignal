package blk.freeyourgadget.gadgetbridge.devices.flipper.zero;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import blk.freeyourgadget.gadgetbridge.GBException;
import blk.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import blk.freeyourgadget.gadgetbridge.devices.InstallHandler;
import blk.freeyourgadget.gadgetbridge.devices.SampleProvider;
import blk.freeyourgadget.gadgetbridge.entities.DaoSession;
import blk.freeyourgadget.gadgetbridge.entities.Device;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;

public class FlipperZeroCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        if(candidate.supportsService(UUID.fromString("00003082-0000-1000-8000-00805f9b34fb"))){
            return DeviceType.FLIPPER_ZERO; // need to filter for flipper here
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.FLIPPER_ZERO;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return 0;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "Flipper devices";
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }
}
