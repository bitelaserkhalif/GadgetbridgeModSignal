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

import blk.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import blk.freeyourgadget.gadgetbridge.devices.lefun.commands.BaseCommand;
import blk.freeyourgadget.gadgetbridge.devices.lefun.commands.HydrationReminderIntervalCommand;
import blk.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import blk.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class SetHydrationReminderIntervalRequest extends Request {
    private int interval;

    public SetHydrationReminderIntervalRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public byte[] createRequest() {
        HydrationReminderIntervalCommand cmd = new HydrationReminderIntervalCommand();

        cmd.setOp(BaseCommand.OP_SET);
        cmd.setHydrationReminderInterval((byte) interval);

        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        HydrationReminderIntervalCommand cmd = new HydrationReminderIntervalCommand();
        cmd.deserialize(data);
        if (cmd.getOp() == BaseCommand.OP_SET && !cmd.isSetSuccess())
            reportFailure("Could not set hydration reminder interval");

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_HYDRATION_REMINDER_INTERVAL;
    }
}
