/*  Copyright (C) 2016-2021 Andreas Shimokawa, Daniele Gobbetti, Sebastian
    Kranz

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
package blk.freeyourgadget.gadgetbridge.service.devices.liveview;

import blk.freeyourgadget.gadgetbridge.model.NotificationSpec;
import blk.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import blk.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import blk.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class LiveviewSupport extends AbstractSerialDeviceSupport {

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new LiveviewProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new LiveviewIoThread(getDevice(), getContext(), getDeviceProtocol(), LiveviewSupport.this, getBluetoothAdapter());
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public synchronized LiveviewIoThread getDeviceIOThread() {
        return (LiveviewIoThread) super.getDeviceIOThread();
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        super.onNotification(notificationSpec);
    }
}
