/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, José Rebelo

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
package blk.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.GBException;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import blk.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import blk.freeyourgadget.gadgetbridge.devices.InstallHandler;
import blk.freeyourgadget.gadgetbridge.devices.SampleProvider;
import blk.freeyourgadget.gadgetbridge.entities.DaoSession;
import blk.freeyourgadget.gadgetbridge.entities.Device;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;
import blk.freeyourgadget.gadgetbridge.util.FileUtils;
import blk.freeyourgadget.gadgetbridge.util.Version;

public class QHybridCoordinator extends AbstractBLEDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(QHybridCoordinator.class);

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        for(ParcelUuid uuid : candidate.getServiceUuids()){
            if(uuid.getUuid().toString().equals("3dda0001-957f-7d4a-34a6-74696673696d")){
                return DeviceType.FOSSILQHYBRID;
            }
        }
        return DeviceType.UNKNOWN;
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return Collections.singletonList(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("3dda0001-957f-7d4a-34a6-74696673696d")).build());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.FOSSILQHYBRID;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }



    @Override
    public boolean supportsActivityDataFetching() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();
        for(GBDevice device : devices){
            if(isFossilHybrid(device) && device.getState() == GBDevice.State.INITIALIZED){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new HybridHRActivitySampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        if (isHybridHR()) {
            FossilHRInstallHandler installHandler = new FossilHRInstallHandler(uri, context);
            if (!installHandler.isValid()) {
                LOG.warn("Not a Fossil Hybrid firmware or app!");
                return null;
            } else {
                return installHandler;
            }
        }
        FossilInstallHandler installHandler = new FossilInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }

    @Override
    public boolean supportsFlashing() { return false; }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    private boolean supportsAlarmConfiguration() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();
        LOG.debug("devices count: " + devices.size());
        for(GBDevice device : devices){
            if(isFossilHybrid(device) && device.getState() == GBDevice.State.INITIALIZED){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return this.supportsAlarmConfiguration() ? 5 : 0;
    }

    @Override
    public boolean supportsAlarmDescription(GBDevice device) {
        return isHybridHR();
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return this.isHybridHR();
    }

    @Override
    public String getManufacturer() {
        return "Fossil";
    }

    @Override
    public boolean supportsAppsManagement() {
        return true;
    }

    @Override
    public boolean supportsAppListFetching() {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return isHybridHR() ? AppManagerActivity.class : ConfigActivity.class;
    }

    @Override
    public Class<? extends Activity> getWatchfaceDesignerActivity() {
        return isHybridHR() ? HybridHRWatchfaceDesignerActivity.class : null;
    }

    /**
     * Returns the directory containing the watch app cache.
     * @throws IOException when the external files directory cannot be accessed
     */
    public File getAppCacheDir() throws IOException {
        return new File(FileUtils.getExternalFilesDir(), "qhybrid-app-cache");
    }

    /**
     * Returns a String containing the device app sort order filename.
     */
    @Override
    public String getAppCacheSortFilename() {
        return "wappcacheorder.txt";
    }

    /**
     * Returns a String containing the file extension for watch apps.
     */
    @Override
    public String getAppFileExtension() {
        return ".wapp";
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
    public boolean supportsWeather() {
        return isHybridHR();
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        if (!isHybridHR()) {
            return new int[0];
        }
        //Settings applicable to all firmware versions
        int[] supportedSettings = new int[]{
                R.xml.devicesettings_fossilhybridhr,
                R.xml.devicesettings_inactivity,
                R.xml.devicesettings_emergency_hr,
                R.xml.devicesettings_autoremove_notifications,
                R.xml.devicesettings_canned_dismisscall_16,
                R.xml.devicesettings_transliteration
        };
        //Firmware specific settings
        if (getFirmwareVersion() != null && getFirmwareVersion().smallerThan(new Version("3.0"))) {
            supportedSettings = ArrayUtils.insert(0, supportedSettings, R.xml.devicesettings_fossilhybridhr_buttonconfiguration_pre_fw30);
        } else {
            supportedSettings = ArrayUtils.insert(0, supportedSettings, R.xml.devicesettings_fossilhybridhr_buttonconfiguration);
        }
        if (getFirmwareVersion() != null && getFirmwareVersion().smallerThan(new Version("2.20"))) {
            supportedSettings = ArrayUtils.insert(1, supportedSettings, R.xml.devicesettings_fossilhybridhr_pre_fw20);
        }
        return supportedSettings;
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_pairingkey
        };
    }

    @Override
    public int[] getSupportedDeviceSpecificApplicationSettings() {
        return new int[]{
                R.xml.devicesettings_custom_deviceicon,
        };
    }

    private boolean isHybridHR() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();
        for(GBDevice device : devices){
            if(isHybridHR(device)){
                return true;
            }
        }
        return false;
    }

    private boolean isHybridHR(GBDevice device){
        if(!isFossilHybrid(device)) return false;
        return device.getName().startsWith("Hybrid HR") || device.getName().equals("Fossil Gen. 6 Hybrid");
    }

    private Version getFirmwareVersion() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();
        for (GBDevice device : devices) {
            if (isFossilHybrid(device)) {
                return new Version(device.getFirmwareVersion2());
            }
        }

        return null;
    }

    private boolean isFossilHybrid(GBDevice device){
        return device.getType() == DeviceType.FOSSILQHYBRID;
    }
}