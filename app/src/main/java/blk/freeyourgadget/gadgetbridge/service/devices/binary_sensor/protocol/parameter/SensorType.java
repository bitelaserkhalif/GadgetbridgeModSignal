package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;

public class SensorType extends Parameter{
    blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType sensorType;
    public SensorType(blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType sensorType) {
        super(ParameterId.PARAMETER_ID_SENSOR_TYPE, sensorType.getSensorTypeByte());
        this.sensorType = sensorType;
    }

    public static SensorType decode(byte[] data){
        return new SensorType(
                blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType.fromSensorTypeByte(data[0])
        );
    }

}
