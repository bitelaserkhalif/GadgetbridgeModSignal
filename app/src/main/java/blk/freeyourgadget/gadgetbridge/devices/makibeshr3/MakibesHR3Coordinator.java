/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Cre3per,
    Daniele Gobbetti, José Rebelo, Petr Kadlec, protomors

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
package blk.freeyourgadget.gadgetbridge.devices.makibeshr3;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.dao.query.QueryBuilder;
import blk.freeyourgadget.gadgetbridge.GBException;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import blk.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import blk.freeyourgadget.gadgetbridge.devices.InstallHandler;
import blk.freeyourgadget.gadgetbridge.devices.SampleProvider;
import blk.freeyourgadget.gadgetbridge.entities.DaoSession;
import blk.freeyourgadget.gadgetbridge.entities.Device;
import blk.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySampleDao;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.model.ActivitySample;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;
import blk.freeyourgadget.gadgetbridge.util.GBPrefs;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;


public class MakibesHR3Coordinator extends AbstractBLEDeviceCoordinator {

    public static final int FindPhone_ON = -1;
    public static final int FindPhone_OFF = 0;

    private static final Logger LOG = LoggerFactory.getLogger(MakibesHR3Coordinator.class);


    public static boolean shouldEnableHeadsUpScreen(SharedPreferences sharedPrefs) {
        String liftMode = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_on));

        // Makibes HR3 doesn't support scheduled intervals. Treat it as "on".
        return !liftMode.equals(getContext().getString(R.string.p_off));
    }

    public static boolean shouldEnableLostReminder(SharedPreferences sharedPrefs) {
        String lostReminder = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DISCONNECT_NOTIFICATION, getContext().getString(R.string.p_on));

        // Makibes HR3 doesn't support scheduled intervals. Treat it as "on".
        return !lostReminder.equals(getContext().getString(R.string.p_off));
    }

    public static byte getTimeMode(SharedPreferences sharedPrefs) {
        GBPrefs gbPrefs = new GBPrefs(new Prefs(sharedPrefs));

        String timeMode = gbPrefs.getTimeFormat();

        if (timeMode.equals(getContext().getString(R.string.p_timeformat_24h))) {
            return MakibesHR3Constants.ARG_SET_TIMEMODE_24H;
        } else {
            return MakibesHR3Constants.ARG_SET_TIMEMODE_12H;
        }
    }

    /**
     * @param startOut out Only hour/minute are used.
     * @param endOut   out Only hour/minute are used.
     * @return True if quite hours are enabled.
     */
    public static boolean getQuiteHours(SharedPreferences sharedPrefs, Calendar startOut, Calendar endOut) {
        String doNotDisturb = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO, getContext().getString(R.string.p_off));

        if (doNotDisturb.equals(getContext().getString(R.string.p_off))) {
            return false;
        } else {
            String start = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START, "00:00");
            String end = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END, "00:00");

            DateFormat df = new SimpleDateFormat("HH:mm");

            try {
                startOut.setTime(df.parse(start));
                endOut.setTime(df.parse(end));

                return true;
            } catch (Exception e) {
                LOG.error("Unexpected exception in MiBand2Coordinator.getTime: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * @return {@link #FindPhone_OFF}, {@link #FindPhone_ON}, or the duration
     */
    public static int getFindPhone(SharedPreferences sharedPrefs) {
        String findPhone = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_FIND_PHONE, getContext().getString(R.string.p_off));

        if (findPhone.equals(getContext().getString(R.string.p_off))) {
            return FindPhone_OFF;
        } else if (findPhone.equals(getContext().getString(R.string.p_on))) {
            return FindPhone_ON;
        } else { // Duration
            String duration = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION, "0");

            try {
                int iDuration;

                try {
                    iDuration = Integer.valueOf(duration);
                } catch (Exception ex) {
                    LOG.warn(ex.getMessage());
                    iDuration = 60;
                }

                return iDuration;
            } catch (Exception e) {
                LOG.error("Unexpected exception in MiBand2Coordinator.getTime: " + e.getMessage());
                return FindPhone_ON;
            }
        }
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();

        List<String> deviceNames = new ArrayList<String>(){{
            add("Y808"); // Chinese version
            add("MAKIBES HR3"); // English version
        }};

        if (name != null) {
            if (deviceNames.contains(name)) {
                return DeviceType.MAKIBESHR3;
            }
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getMakibesHR3ActivitySampleDao().queryBuilder();
        qb.where(MakibesHR3ActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MAKIBESHR3;
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
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new MakibesHR3SampleProvider(device, session);
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
        return 8;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Makibes";
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
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_disconnectnotification,
                R.xml.devicesettings_donotdisturb_no_auto,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_transliteration
        };
    }
}
