package blk.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;

public class EmergencyHRActivity extends AbstractGBActivity  {
    //Activity for heart rate monitoring.
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyHRActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GBDevice gbDevice;
        super.onCreate(savedInstanceState);
        final Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_heartrate_emergency);
        }

    }

}
