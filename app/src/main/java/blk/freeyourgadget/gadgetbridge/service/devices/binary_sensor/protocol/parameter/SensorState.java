package blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;

public class SensorState extends Parameter{
    blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState sensorState;
    int count;

    public SensorState(blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState sensorState, int count) {
        super(ParameterId.PARAMETER_ID_SENSOR_STATUS, sensorState.getSensorStateByte());
        this.sensorState = sensorState;
        this.count = count;
    }

    public blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState getSensorState() {
        return sensorState;
    }

    public int getCount() {
        return count;
    }

    public static SensorState decode(byte[] data){
        int dataInt = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
        byte stateByte = (byte)((dataInt >> 11) & 0x01);
        int count = dataInt & 0b11111111111;
        return new SensorState(
                blk.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState.fromSensorStateByte(stateByte),
                count
        );
    }
}
