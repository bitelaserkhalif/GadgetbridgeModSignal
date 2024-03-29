/*  Copyright (C) 2021 José Rebelo

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
package blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl;

import java.util.List;

import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AudioUpsampling;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerCustomBands;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SoundPosition;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SurroundMode;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.TouchSensor;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.VoiceNotifications;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.BatteryType;
import blk.freeyourgadget.gadgetbridge.util.DeviceHelper;

public abstract class AbstractSonyProtocolImpl {
    private final GBDevice device;

    public AbstractSonyProtocolImpl(final GBDevice device) {
        this.device = device;
    }

    protected GBDevice getDevice() {
        return this.device;
    }

    protected SonyHeadphonesCoordinator getCoordinator() {
        return (SonyHeadphonesCoordinator) DeviceHelper.getInstance().getCoordinator(getDevice());
    }

    public abstract Request getAmbientSoundControl();

    public abstract Request setAmbientSoundControl(final AmbientSoundControl config);

    public abstract Request getNoiseCancellingOptimizerState();

    public abstract Request getAudioCodec();

    public abstract Request getBattery(final BatteryType batteryType);

    public abstract Request getFirmwareVersion();

    public abstract Request getAudioUpsampling();

    public abstract Request setAudioUpsampling(final AudioUpsampling config);

    public abstract Request getAutomaticPowerOff();

    public abstract Request setAutomaticPowerOff(final AutomaticPowerOff config);

    public abstract Request getButtonModes();

    public abstract Request setButtonModes(final ButtonModes config);

    public abstract Request getQuickAccess();

    public abstract Request setQuickAccess(final QuickAccess quickAccess);

    public abstract Request getAmbientSoundControlButtonMode();

    public abstract Request setAmbientSoundControlButtonMode(final AmbientSoundControlButtonMode ambientSoundControlButtonMode);

    public abstract Request getPauseWhenTakenOff();

    public abstract Request setPauseWhenTakenOff(final PauseWhenTakenOff config);

    public abstract Request getEqualizer();

    public abstract Request setEqualizerPreset(final EqualizerPreset config);

    public abstract Request setEqualizerCustomBands(final EqualizerCustomBands config);

    public abstract Request getSoundPosition();

    public abstract Request setSoundPosition(final SoundPosition config);

    public abstract Request getSurroundMode();

    public abstract Request setSurroundMode(final SurroundMode config);

    public abstract Request getTouchSensor();

    public abstract Request setTouchSensor(final TouchSensor config);

    public abstract Request getVoiceNotifications();

    public abstract Request setVoiceNotifications(final VoiceNotifications config);

    public abstract Request startNoiseCancellingOptimizer(final boolean start);

    public abstract Request powerOff();

    public abstract List<? extends GBDeviceEvent> handlePayload(final MessageType messageType, final byte[] payload);
}
