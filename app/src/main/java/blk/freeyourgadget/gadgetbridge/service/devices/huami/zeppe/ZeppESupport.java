/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Dmytro
    Bielik, pangwalla, randnv20

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
package blk.freeyourgadget.gadgetbridge.service.devices.huami.zeppe;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import blk.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import blk.freeyourgadget.gadgetbridge.devices.huami.zeppe.ZeppEFWHelper;
import blk.freeyourgadget.gadgetbridge.model.CallSpec;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgtr.AmazfitGTRSupport;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation2020;

public class ZeppESupport extends AmazfitGTRSupport {

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new ZeppEFWHelper(uri, context);
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        onSetCallStateNew(callSpec);
    }

    @Override
    public UpdateFirmwareOperation createUpdateFirmwareOperation(Uri uri) {
        return new UpdateFirmwareOperation2020(uri, this);
    }

    @Override
    public int getActivitySampleSize() {
        return 8;
    }
}
