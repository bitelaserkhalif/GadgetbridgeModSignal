package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;

public class ResultCode extends Parameter{
    blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ResultCode resultCode;
    public ResultCode(blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ResultCode resultCode) {
        super(ParameterId.PARAMETER_ID_RESULT_CODE, resultCode.getResultCodeByte());
        this.resultCode = resultCode;
    }

    public static ResultCode decode(byte[] data){
        return new ResultCode(
                blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ResultCode.fromResultCodeByte(data[0])
        );
    }
}
