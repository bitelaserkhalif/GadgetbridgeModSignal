/*  Copyright (C) 2018-2021 Andreas Shimokawa, Sebastian Kranz

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
package blk.freeyourgadget.gadgetbridge.devices.zetime;

import android.os.Bundle;
import android.preference.Preference;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

public class ZeTimePreferenceActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zetime_preferences);

        GBApplication.deviceService().onReadConfiguration("do_it");

        final Preference heartrateMeasurementInterval = findPreference(ZeTimeConstants.PREF_ZETIME_HEARTRATE_INTERVAL);
        heartrateMeasurementInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));
                return true;
            }
        });

        addPreferenceHandlerFor(ZeTimeConstants.PREF_SCREENTIME);

        addPreferenceHandlerFor(ZeTimeConstants.PREF_ANALOG_MODE);

        addPreferenceHandlerFor(ZeTimeConstants.PREF_ACTIVITY_TRACKING);

        addPreferenceHandlerFor(ZeTimeConstants.PREF_HANDMOVE_DISPLAY);

        addPreferenceHandlerFor(ZeTimeConstants.PREF_CALORIES_TYPE);

        addPreferenceHandlerFor(ZeTimeConstants.PREF_DATE_FORMAT);

        addPreferenceHandlerFor(ZeTimeConstants.PREF_SMS_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_CALENDAR_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_CALL_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_MISSED_CALL_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_EMAIL_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_INACTIVITY_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_LOW_POWER_SIGNALING);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_SOCIAL_SIGNALING);


        addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_HEARTRATE_ALARM);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_MAX_HEARTRATE);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_MIN_HEARTRATE);


        addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_SLEEP_GOAL);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_CALORIES_GOAL);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_DISTANCE_GOAL);
        addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_ACTIVETIME_GOAL);
    }

    /**
     * delayed execution so that the preferences are applied first
     */
//    private void invokeLater(Runnable runnable) {
//        getListView().post(runnable);
//    }

//    private void addTryListeners() {
//        for (final NotificationType type : NotificationType.values()) {
//            String prefKey = "zetime_try_" + type.getGenericType();
//            final Preference tryPref = findPreference(prefKey);
//            if (tryPref != null) {
//                tryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                    @Override
//                    public boolean onPreferenceClick(Preference preference) {
//                        tryVibration(type);
//                        return true;
//                    }
//                });
//            } else {
//                GB.toast(getBaseContext(), "Unable to find preference key: " + prefKey + ", trying the vibration won't work", Toast.LENGTH_LONG, GB.WARN);
//            }
//        }
//    }
//
//    private void tryVibration(NotificationType type) {
//        NotificationSpec spec = new NotificationSpec();
//        spec.type = type;
//        GBApplication.deviceService().onNotification(spec);
//    }
}
