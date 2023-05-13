package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;
import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState;

public class ReportStatus extends Parameter{
    ReportState reportState;
    public ReportStatus(blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState reportState) {
        super(ParameterId.PARAMETER_ID_REPORT_STATUS, reportState.getReportStateByte());
        this.reportState = reportState;
    }

    public static ReportStatus decode(byte[] data){
        return new ReportStatus(
                blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState.fromReportStateByte(data[0])
        );
    }
}
