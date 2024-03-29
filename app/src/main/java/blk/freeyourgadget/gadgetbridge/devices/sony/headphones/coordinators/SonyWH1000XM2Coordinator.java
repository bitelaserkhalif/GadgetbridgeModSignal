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
package blk.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import blk.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;

public class SonyWH1000XM2Coordinator extends SonyHeadphonesCoordinator {
    @NonNull
    @Override
    public DeviceType getSupportedType(final GBDeviceCandidate candidate) {
        if (candidate.getName().contains("WH-1000XM2")) {
            return DeviceType.SONY_WH_1000XM2;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SONY_WH_1000XM2;
    }

    @Override
    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Arrays.asList(
                SonyHeadphonesCapabilities.BatterySingle,
                SonyHeadphonesCapabilities.AmbientSoundControl,
                SonyHeadphonesCapabilities.WindNoiseReduction,
                SonyHeadphonesCapabilities.AncOptimizer,
                SonyHeadphonesCapabilities.AudioSettingsOnlyOnSbcCodec,
                SonyHeadphonesCapabilities.EqualizerWithCustomBands,
                SonyHeadphonesCapabilities.SoundPosition,
                SonyHeadphonesCapabilities.SurroundMode,
                SonyHeadphonesCapabilities.AudioUpsampling
        );
    }
}
