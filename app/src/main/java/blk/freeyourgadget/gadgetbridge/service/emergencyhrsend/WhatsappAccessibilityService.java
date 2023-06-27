/*  Copyright (C) 20223 bitelaserkhalif
    ------------------------------
    MODDED Bitelaserkhalif
    TU9EREVEIEJpdGVsYXNlcmtoYWxpZg
    ------------------------------
    This file is part of MODDED Gadgetbridge.

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

package blk.freeyourgadget.gadgetbridge.service.emergencyhrsend;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import blk.freeyourgadget.gadgetbridge.R;

public class WhatsappAccessibilityService extends AccessibilityService {
    private static final Logger LOG = LoggerFactory.getLogger(WhatsappAccessibilityService.class);
    @Override
    //https://stackoverflow.com/questions/49654674/send-message-via-whatsapp-programmatically
    public void onAccessibilityEvent (AccessibilityEvent event) {
        if (getRootInActiveWindow () == null) {
            return;
        }

        AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap (getRootInActiveWindow ());

        // Whatsapp Message EditText id
        List<AccessibilityNodeInfoCompat> messageNodeList = rootInActiveWindow.findAccessibilityNodeInfosByViewId ("com.whatsapp:id/entry");
        if (messageNodeList == null || messageNodeList.isEmpty ()) {
            return;
        }

        // check if the whatsapp message EditText field is filled with text and ending with  suffix (R.string.accessibility_fingerprint)
        AccessibilityNodeInfoCompat messageField = messageNodeList.get (0);

        if (messageField.getText () == null || messageField.getText ().length () == 0 || !messageField.getText ().toString ().endsWith (getApplicationContext ().getString (R.string.accessibility_fingerprint))) { // So your service doesn't process any message, but the ones ending your apps suffix

            return;
        }

        // Whatsapp send button id
        List<AccessibilityNodeInfoCompat> sendMessageNodeInfoList = rootInActiveWindow.findAccessibilityNodeInfosByViewId ("com.whatsapp:id/send");

        if (sendMessageNodeInfoList == null || sendMessageNodeInfoList.isEmpty ()) {
            return;
        }

        AccessibilityNodeInfoCompat sendMessageButton = sendMessageNodeInfoList.get (0);

        if (!sendMessageButton.isVisibleToUser ()) {
            return;
        }

        // Now fire a click on the send button
        sendMessageButton.performAction (AccessibilityNodeInfo.ACTION_CLICK);
        LOG.debug("Phase 6 ACC");

        // Now go back to your app by clicking on the Android back button twice:
        // First one to leave the conversation screen
        // Second one to leave whatsapp
        LOG.debug("Phase FINALE ACC");

        try {
            Thread.sleep (500); // hack for certain devices in which the immediate back click is too fast to handle
            performGlobalAction (GLOBAL_ACTION_BACK);
            Thread.sleep (500);  // same hack as above
        } catch (InterruptedException ignored) {}
        performGlobalAction (GLOBAL_ACTION_BACK);
    }

    @Override
    public void onInterrupt() {

    }
}
