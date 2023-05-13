/*  Copyright (C) 2022 José Rebelo

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
package blk.freeyourgadget.gadgetbridge.externalevents.gps;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import blk.freeyourgadget.gadgetbridge.devices.EventHandler;
import blk.freeyourgadget.gadgetbridge.util.GB;


/**
 * A static location manager, which keeps track of what providers are currently running. A notification is kept
 * while there is at least one provider runnin.
 */
public class GBLocationManager {
    private static final Logger LOG = LoggerFactory.getLogger(GBLocationManager.class);

    /**
     * The current number of running listeners.
     */
    private static Map<EventHandler, Map<LocationProviderType, AbstractLocationProvider>> providers = new HashMap<>();

    public static void start(final Context context, final EventHandler eventHandler) {
        GBLocationManager.start(context, eventHandler, LocationProviderType.GPS, null);
    }

    public static void start(final Context context, final EventHandler eventHandler, final LocationProviderType providerType, Integer updateInterval) {
        System.out.println("Starting");
        if (providers.containsKey(eventHandler) && providers.get(eventHandler).containsKey(providerType)) {
            LOG.warn("EventHandler already registered");
            return;
        }

        GB.createGpsNotification(context, providers.size());

        final GBLocationListener locationListener = new GBLocationListener(eventHandler);
        final AbstractLocationProvider locationProvider;
        switch (providerType) {
            case GPS:
                LOG.info("Using gps location provider");
                locationProvider = new PhoneGpsLocationProvider(locationListener);
                break;
            case NETWORK:
                LOG.info("Using network location provider");
                locationProvider = new PhoneNetworkLocationProvider(locationListener);
                break;
            default:
                LOG.info("Using default location provider: GPS");
                locationProvider = new PhoneGpsLocationProvider(locationListener);
        }

        if (updateInterval != null) {
            locationProvider.start(context, updateInterval);
        } else {
            locationProvider.start(context);
        }

        if (providers.containsKey(eventHandler)) {
            providers.get(eventHandler).put(providerType, locationProvider);
        } else {
            Map<LocationProviderType, AbstractLocationProvider> providerMap = new HashMap<>();
            providerMap.put(providerType, locationProvider);
            providers.put(eventHandler, providerMap);
        }
    }

    public static void stop(final Context context, final EventHandler eventHandler) {
        GBLocationManager.stop(context, eventHandler, null);
    }

    public static void stop(final Context context, final EventHandler eventHandler, final LocationProviderType gpsType) {
        if (!providers.containsKey(eventHandler)) return;
        Map<LocationProviderType, AbstractLocationProvider> providerMap = providers.get(eventHandler);
        if (gpsType == null) {
            Set<LocationProviderType> toBeRemoved = new HashSet<>();
            for (LocationProviderType providerType: providerMap.keySet()) {
                stopProvider(context, providerMap.get(providerType));
                toBeRemoved.add(providerType);
            }
            toBeRemoved.forEach(c->{providerMap.remove(c);});
        } else {
            stopProvider(context, providerMap.get(gpsType));
            providerMap.remove(gpsType);
        }
        LOG.debug("Remaining providers: " + providers.size());
        if (providers.get(eventHandler).size() == 0)
            providers.remove(eventHandler);
        updateNotification(context);
    }

    private static void updateNotification(final Context context){
        if (!providers.isEmpty()) {
            GB.createGpsNotification(context, providers.size());
        } else {
            GB.removeGpsNotification(context);
        }
    }

    private static void stopProvider(final Context context, AbstractLocationProvider locationProvider) {
        if (locationProvider != null) {
            locationProvider.stop(context);
        }
    }

    public static void stopAll(final Context context) {
        for (EventHandler eventHandler : providers.keySet()) {
            stop(context, eventHandler);
        }
    }
}
