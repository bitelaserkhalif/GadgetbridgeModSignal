/*  Copyright (C) 2022 José Rebelo

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
package blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import blk.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.PayloadTypeV1;
import blk.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v2.SonyProtocolImplV2;
import blk.freeyourgadget.gadgetbridge.util.GB;

public class SonyProtocolImplV3 extends SonyProtocolImplV2 {
    private static final Logger LOG = LoggerFactory.getLogger(SonyProtocolImplV3.class);

    public SonyProtocolImplV3(final GBDevice device) {
        super(device);
    }

    @Override
    public Request getAmbientSoundControl() {
        return new Request(
                PayloadTypeV1.AMBIENT_SOUND_CONTROL_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AMBIENT_SOUND_CONTROL_GET.getCode(),
                        (byte) 0x17
                }
        );
    }

    @Override
    public Request setAmbientSoundControl(final AmbientSoundControl ambientSoundControl) {
        final ByteBuffer buf = ByteBuffer.allocate(8);

        buf.put(PayloadTypeV1.AMBIENT_SOUND_CONTROL_SET.getCode());
        buf.put((byte) 0x17);
        buf.put((byte) 0x01); // 0x00 while dragging the slider?

        if (AmbientSoundControl.Mode.OFF.equals(ambientSoundControl.getMode())) {
            buf.put((byte) 0x00);
        } else {
            buf.put((byte) 0x01);
        }

        if (AmbientSoundControl.Mode.AMBIENT_SOUND.equals(ambientSoundControl.getMode())) {
            buf.put((byte) 0x01);
        } else {
            buf.put((byte) 0x00);
        }

        buf.put((byte) (ambientSoundControl.isFocusOnVoice() ? 0x01 : 0x00));
        buf.put((byte) (ambientSoundControl.getAmbientSound()));

        return new Request(PayloadTypeV1.AMBIENT_SOUND_CONTROL_SET.getMessageType(), buf.array());
    }

    @Override
    public Request getQuickAccess() {
        return new Request(
                PayloadTypeV3.QUICK_ACCESS_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV3.QUICK_ACCESS_GET.getCode(),
                        (byte) 0x0d
                }
        );
    }

    @Override
    public Request setQuickAccess(final QuickAccess quickAccess) {
        return new Request(
                PayloadTypeV3.QUICK_ACCESS_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV3.QUICK_ACCESS_SET.getCode(),
                        (byte) 0x0d,
                        (byte) 0x02,
                        quickAccess.getModeDoubleTap().getCode(),
                        quickAccess.getModeTripleTap().getCode()
                }
        );
    }

    @Override
    public Request getAmbientSoundControlButtonMode() {
        return new Request(
                PayloadTypeV3.AMBIENT_SOUND_CONTROL_BUTTON_MODE_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV3.AMBIENT_SOUND_CONTROL_BUTTON_MODE_GET.getCode(),
                        (byte) 0x03
                }
        );
    }

    @Override
    public Request setAmbientSoundControlButtonMode(final AmbientSoundControlButtonMode ambientSoundControlButtonMode) {
        return new Request(
                PayloadTypeV3.AMBIENT_SOUND_CONTROL_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV3.AMBIENT_SOUND_CONTROL_BUTTON_MODE_SET.getCode(),
                        (byte) 0x03,
                        (byte) 0x01,
                        (byte) 0x35,
                        (byte) 0x01,
                        (byte) 0x00,
                        ambientSoundControlButtonMode.getCode()
                }
        );
    }

    @Override
    public List<? extends GBDeviceEvent> handlePayload(final MessageType messageType, final byte[] payload) {
        final PayloadTypeV3 payloadType = PayloadTypeV3.fromCode(messageType, payload[0]);

        switch (payloadType) {
            case QUICK_ACCESS_RET:
            case QUICK_ACCESS_NOTIFY:
                return handleQuickAccess(payload);
            case AMBIENT_SOUND_CONTROL_BUTTON_MODE_RET:
            case AMBIENT_SOUND_CONTROL_BUTTON_MODE_NOTIFY:
                return handleAmbientSoundControlButtonMode(payload);
        }

        return super.handlePayload(messageType, payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleAmbientSoundControl(final byte[] payload) {
        if (payload.length != 7) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x17) {
            LOG.warn("Not ambient sound control, ignoring {}", payload[1]);
            return Collections.emptyList();
        }

        AmbientSoundControl.Mode mode = null;

        if (payload[3] == (byte) 0x00) {
            mode = AmbientSoundControl.Mode.OFF;
        } else if (payload[3] == (byte) 0x01) {
            // Enabled, determine mode

            if (payload[4] == (byte) 0x00) {
                mode = AmbientSoundControl.Mode.NOISE_CANCELLING;
            } else if (payload[4] == (byte) 0x01) {
                mode = AmbientSoundControl.Mode.AMBIENT_SOUND;
            }
        }

        if (mode == null) {
            LOG.warn("Unable to determine ambient sound control mode from {}", GB.hexdump(payload));
            return Collections.emptyList();
        }

        final Boolean focusOnVoice = booleanFromByte(payload[5]);
        if (focusOnVoice == null) {
            LOG.warn("Unknown focus on voice mode {}", String.format("%02x", payload[5]));
            return Collections.emptyList();
        }

        int ambientSound = payload[6];
        if (ambientSound < 0 || ambientSound > 20) {
            LOG.warn("Ambient sound level {} is out of range", String.format("%02x", payload[6]));
            return Collections.emptyList();
        }

        final AmbientSoundControl ambientSoundControl = new AmbientSoundControl(mode, focusOnVoice, ambientSound);

        LOG.warn("Ambient sound control: {}", ambientSoundControl);

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreferences(ambientSoundControl.toPreferences());

        return Collections.singletonList(eventUpdatePreferences);
    }

    public List<? extends GBDeviceEvent> handleQuickAccess(final byte[] payload) {
        if (payload.length != 5) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x0d || payload[2] != 0x02) {
            LOG.warn("Unexpected quick access payload bytes {}", String.format("%02x %02x", payload[1], payload[2]));
            return Collections.emptyList();
        }

        final QuickAccess.Mode modeDouble = QuickAccess.Mode.fromCode(payload[3]);
        final QuickAccess.Mode modeTriple = QuickAccess.Mode.fromCode(payload[4]);
        if (modeDouble == null || modeTriple == null) {
            LOG.warn("Unknown quick access codes {}", String.format("%02x %02x", payload[3], payload[4]));
            return Collections.emptyList();
        }

        LOG.debug("Quick Access: Double Tap: {}, Triple Tap: {}", modeDouble, modeTriple);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new QuickAccess(modeDouble, modeTriple).toPreferences());

        return Collections.singletonList(event);
    }

    public List<? extends GBDeviceEvent> handleAmbientSoundControlButtonMode(final byte[] payload) {
        if (payload.length != 7) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x03 || payload[2] != 0x01 || payload[3] != 0x35 || payload[4] != 0x01 || payload[5] != 0x00) {
            LOG.warn(
                    "Unexpected ambient sound control button mode payload bytes {}",
                    String.format("%02x %02x %02x %02x %02x", payload[1], payload[2], payload[3], payload[4], payload[5])
            );
            return Collections.emptyList();
        }

        final AmbientSoundControlButtonMode mode = AmbientSoundControlButtonMode.fromCode(payload[6]);
        if (mode == null) {
            LOG.warn("Unknown ambient sound control button mode code {}", String.format("%02x", payload[6]));
            return Collections.emptyList();
        }

        LOG.debug("Ambient Sound Control Buton Mode: {}", mode);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(mode.toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    protected ButtonModes.Mode decodeButtonMode(final byte b) {
        switch (b) {
            case (byte) 0xff:
                return ButtonModes.Mode.OFF;
            case (byte) 0x35:  // Seems to be the only one that differs?
                return ButtonModes.Mode.AMBIENT_SOUND_CONTROL;
            case (byte) 0x20:
                return ButtonModes.Mode.PLAYBACK_CONTROL;
            case (byte) 0x10:
                return ButtonModes.Mode.VOLUME_CONTROL;
        }

        return null;
    }

    @Override
    protected byte encodeButtonMode(final ButtonModes.Mode buttonMode) {
        switch (buttonMode) {
            case OFF:
                return (byte) 0xff;
            case AMBIENT_SOUND_CONTROL:
                return (byte) 0x35; // Seems to be the only one that differs?
            case PLAYBACK_CONTROL:
                return (byte) 0x20;
            case VOLUME_CONTROL:
                return (byte) 0x10;
        }

        throw new IllegalArgumentException("Unknown button mode " + buttonMode);
    }
}
