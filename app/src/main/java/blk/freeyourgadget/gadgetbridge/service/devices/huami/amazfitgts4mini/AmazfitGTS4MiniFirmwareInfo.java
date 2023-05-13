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
package blk.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts4mini;

import java.util.HashMap;
import java.util.Map;

import blk.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021FirmwareInfo;

public class AmazfitGTS4MiniFirmwareInfo extends Huami2021FirmwareInfo {
    private static final Map<Integer, String> crcToVersion = new HashMap<Integer, String>() {{
        // firmware
    }};

    public AmazfitGTS4MiniFirmwareInfo(final byte[] bytes) {
        super(bytes);
    }

    @Override
    public String deviceName() {
        return HuamiConst.AMAZFIT_GTS4_MINI_NAME;
    }

    @Override
    public boolean isGenerallyCompatibleWith(final GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITGTS4MINI;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}
