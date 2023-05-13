package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message;

import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType;
import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;

public class GetSensorRequest extends Message{
    public GetSensorRequest(SensorType sensorType) {
        super(
                MessageId.MESSAGE_ID_GET_SENSOR_REQUEST,
                new Parameter[]{
                        new blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.SensorType(sensorType)
                }
        );
    }
}
