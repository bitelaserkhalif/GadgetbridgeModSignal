package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message;

import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState;
import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType;
import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;

public class SetSensorRequest extends Message{
    public SetSensorRequest(SensorType sensorType, ReportState reportState) {
        super(
                MessageId.MESSAGE_ID_SET_SENSOR_REQUEST,
                new Parameter[]{
                        new blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.SensorType(sensorType),
                        new blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.ReportStatus(reportState)
                }
        );
    }
}
