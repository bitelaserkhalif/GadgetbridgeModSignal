/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Taavi Eomäe

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
package blk.freeyourgadget.gadgetbridge.devices.huami.miband2;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.InstallActivity;
import blk.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWHelper;
import blk.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWInstallHandler;
import blk.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import blk.freeyourgadget.gadgetbridge.util.Version;

public class MiBand2FWInstallHandler extends AbstractMiBandFWInstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand2FWInstallHandler.class);

    MiBand2FWInstallHandler(Uri uri, Context context) {
        super(uri, context);
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        super.validateInstallation(installActivity, device);
        maybeAddFw53Hint(installActivity, device);
        maybeAddFontHint(installActivity);
    }

    private void maybeAddFontHint(InstallActivity installActivity) {
        HuamiFirmwareType type = getFirmwareType();
        if (type == HuamiFirmwareType.FIRMWARE) {
            String newInfoText = installActivity.getInfoText() + "\n\n" + "Note: you may install Mili_pro.ft or Mili_pro.ft.en to enable text notifications.";
            installActivity.setInfoText(newInfoText);
        }
    }

    private void maybeAddFw53Hint(InstallActivity installActivity, GBDevice device) {
        HuamiFirmwareType type = getFirmwareType();
        if (type != HuamiFirmwareType.FIRMWARE) {
            return;
        }

        Version deviceVersion = getFirmwareVersionOf(device);
        if (deviceVersion != null) {
            Version v53 = MiBandConst.MI2_FW_VERSION_INTERMEDIATE_UPGRADE_53;
            if (deviceVersion.compareTo(v53) < 0) {
                String vInstall = getHelper().format(getHelper().getFirmwareVersion());
                try {
                    if (vInstall == null || new Version(vInstall).compareTo(v53) > 0) {
                        String newInfoText = getContext().getString(R.string.mi2_fw_installhandler_fw53_hint, v53.get()) +
                                "\n\n" +
                                installActivity.getInfoText();
                        installActivity.setInfoText(newInfoText);
                    }
                } catch (IllegalArgumentException e) {
                    String newInfoText = getContext().getString(R.string.mi2_fw_installhandler_fw53_hint, v53.get()) +
                            "\n\n" +
                            installActivity.getInfoText() +
                            "\n\n" +
                            getContext().getString(R.string.error_version_check_extreme_caution, vInstall);
                    installActivity.setInfoText(newInfoText);
                }
            }
        }
    }

    private Version getFirmwareVersionOf(GBDevice device) {
        String version = device.getFirmwareVersion();
        if (version == null || version.length() == 0) {
            return null;
        }
        if (version.charAt(0) == 'V') {
            version = version.substring(1);
        }
        try {
            return new Version(version);
        } catch (Exception ex) {
            LOG.error("Unable to parse version: " + version);
            return null;
        }
    }

    private HuamiFirmwareType getFirmwareType() {
        AbstractMiBandFWHelper helper = getHelper();
        if (helper instanceof MiBand2FWHelper) {
            return ((MiBand2FWHelper) helper).getFirmwareInfo().getFirmwareType();
        }
        return HuamiFirmwareType.INVALID;
    }

    @Override
    protected AbstractMiBandFWHelper createHelper(Uri uri, Context context) throws IOException {
        return new MiBand2FWHelper(uri, context);
    }

    @Override
    protected boolean isSupportedDeviceType(GBDevice device) {
        return device.getType() == DeviceType.MIBAND2;
    }
}
