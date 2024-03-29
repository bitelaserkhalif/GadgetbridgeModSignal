/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public class FileCloseAndPutRequest extends FileCloseRequest {
    FossilWatchAdapter adapter;
    byte[] data;

    public FileCloseAndPutRequest(FileHandle fileHandle, byte[] data, FossilWatchAdapter adapter) {
        super(fileHandle);
        this.adapter = adapter;
        this.data = data;
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        adapter.queueWrite(new FilePutRequest(getHandle(), this.data, adapter) {
            @Override
            public void onFilePut(boolean success) {
                super.onFilePut(success);
                FileCloseAndPutRequest.this.onFilePut(success);
            }
        }, false);
    }

    public void onFilePut(boolean success){

    }
}
