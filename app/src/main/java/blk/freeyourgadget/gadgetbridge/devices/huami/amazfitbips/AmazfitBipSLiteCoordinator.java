/*  Copyright (C) 2017-2021 Andreas Shimokawa, Daniele Gobbetti, João
    Paulo Barraca, José Rebelo, tiparega

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
package blk.freeyourgadget.gadgetbridge.devices.huami.amazfitbips;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blk.freeyourgadget.gadgetbridge.devices.InstallHandler;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;

public class AmazfitBipSLiteCoordinator extends AmazfitBipSCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipSLiteCoordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.AMAZFITBIPS_LITE;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        try {
            BluetoothDevice device = candidate.getDevice();
            String name = device.getName();
            if (name != null && (name.equalsIgnoreCase("Bip S Lite"))) {
                return DeviceType.AMAZFITBIPS_LITE;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        AmazfitBipSLiteFWInstallHandler handler = new AmazfitBipSLiteFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }
}
