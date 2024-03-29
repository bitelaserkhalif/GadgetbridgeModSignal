package blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupAndGetRequest;
import blk.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public class ApplicationsListRequest extends FileLookupAndGetRequest{
    public ApplicationsListRequest(FossilHRWatchAdapter adapter) {
        super(FileHandle.APP_CODE, adapter);
    }

    public void handleFileData(byte[] fileData){
        ArrayList<ApplicationInformation> applicationInfos = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(12);
        while(buffer.remaining() > 4){
            short packetLength = buffer.getShort();
            buffer.get();
            int nameLength = buffer.get() - 1; // cutting off null byte
            byte[] nameBuffer = new byte[nameLength];
            buffer.get(nameBuffer);
            String name = new String(nameBuffer);
            buffer.get(); // null byte
            byte handle = buffer.get();
            int hash = buffer.getInt();
            String version = String.format("%d.%d", buffer.get(), buffer.get());
            buffer.get();  // unknown
            buffer.get();  // unknown

            applicationInfos.add(new ApplicationInformation(
                    name,
                    version,
                    hash,
                    handle
            ));
        }
        Collections.sort(applicationInfos);
        this.handleApplicationsList(applicationInfos);
    }

    public void handleApplicationsList(List<ApplicationInformation> installedApplications){

    }

    public void handleFileLookupError(FILE_LOOKUP_ERROR error){

    }
}
