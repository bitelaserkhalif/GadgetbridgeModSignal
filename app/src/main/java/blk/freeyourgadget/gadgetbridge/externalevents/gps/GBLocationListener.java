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

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blk.freeyourgadget.gadgetbridge.devices.EventHandler;

/**
 * An implementation of a {@link LocationListener} that forwards the location updates to the
 * provided {@link EventHandler}.
 */
public class GBLocationListener implements LocationListener {
    private static final Logger LOG = LoggerFactory.getLogger(GBLocationListener.class);

    private final EventHandler eventHandler;

    private Location previousLocation;

    public GBLocationListener(final EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void onLocationChanged(final Location location) {
        LOG.info("Location changed: {}", location);

        // Correct the location time
        location.setTime(getLocationTimestamp(location));

        // The location usually doesn't contain speed, compute it from the previous location
        if (previousLocation != null && !location.hasSpeed()) {
            long timeInterval = (location.getTime() - previousLocation.getTime()) / 1000L;
            float distanceInMeters = previousLocation.distanceTo(location);
            location.setSpeed(distanceInMeters / timeInterval);
        }

        previousLocation = location;

        eventHandler.onSetGpsLocation(location);
    }

    @Override
    public void onProviderDisabled(final String provider) {
        LOG.info("onProviderDisabled: {}", provider);
    }

    @Override
    public void onProviderEnabled(final String provider) {
        LOG.info("onProviderDisabled: {}", provider);
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {
        LOG.info("onStatusChanged: {}, {}", provider, status);
    }

    private static long getLocationTimestamp(final Location location) {
        long nanosSinceLocation = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
        return System.currentTimeMillis() - (nanosSinceLocation / 100_000L);
    }
}
