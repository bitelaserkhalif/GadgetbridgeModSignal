/*  Copyright (C) 2020-2021 Yukai Li

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
package blk.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import blk.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import blk.freeyourgadget.gadgetbridge.devices.lefun.commands.GetBatteryLevelCommand;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import blk.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class GetBatteryLevelRequest extends Request {
    public GetBatteryLevelRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    @Override
    public byte[] createRequest() {
        GetBatteryLevelCommand cmd = new GetBatteryLevelCommand();
        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        GetBatteryLevelCommand cmd = new GetBatteryLevelCommand();
        cmd.deserialize(data);

        GBDevice device = getSupport().getDevice();
        device.setBatteryThresholdPercent((short)15);

        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.level = (short)((int)cmd.getBatteryLevel() & 0xff);
        getSupport().evaluateGBDeviceEvent(batteryInfo);

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_BATTERY_LEVEL;
    }
}
