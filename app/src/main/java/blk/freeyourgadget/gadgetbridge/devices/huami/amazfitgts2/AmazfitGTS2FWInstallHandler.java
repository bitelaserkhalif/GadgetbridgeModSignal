/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, pangwalla

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
package blk.freeyourgadget.gadgetbridge.devices.huami.amazfitgts2;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import blk.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWInstallHandler;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;

class AmazfitGTS2FWInstallHandler extends AbstractMiBandFWInstallHandler {
    AmazfitGTS2FWInstallHandler(Uri uri, Context context) {
        super(uri, context);
    }

    @Override
    protected String getFwUpgradeNotice() {
        return mContext.getString(R.string.fw_upgrade_notice_amazfitgtr, helper.getHumanFirmwareVersion());
    }

    @Override
    protected AbstractMiBandFWHelper createHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTS2FWHelper(uri, context);
    }

    @Override
    protected boolean isSupportedDeviceType(GBDevice device) {
        return device.getType() == DeviceType.AMAZFITGTS2;
    }
}
