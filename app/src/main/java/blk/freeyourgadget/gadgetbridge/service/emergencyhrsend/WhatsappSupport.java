package blk.freeyourgadget.gadgetbridge.service.emergencyhrsend;

import static blk.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static blk.freeyourgadget.gadgetbridge.GBApplication.getPrefs;

import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.location.Location;

import androidx.annotation.NonNull;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.EmergencyHRActivity;
import blk.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import blk.freeyourgadget.gadgetbridge.util.GB;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class WhatsappSupport {
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyHRActivity.class);
    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public boolean isAccessibilityOn () {
        int accessibilityEnabled = 0;
        Context context = getContext();
        Class<? extends AccessibilityService> clazz = WhatsappAccessibilityService.class;
        final String service = context.getPackageName () + "/" + clazz.getCanonicalName ();
        try {
            accessibilityEnabled = Settings.Secure.getInt (context.getApplicationContext ().getContentResolver (), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException ignored) {  }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter (':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString (context.getApplicationContext ().getContentResolver (), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString (settingValue);
                while (colonSplitter.hasNext ()) {
                    String accessibilityService = colonSplitter.next ();

                    if (accessibilityService.equalsIgnoreCase (service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isTelNoValid(String countryCode, String telNo){
        Phonenumber.PhoneNumber number = new Phonenumber.PhoneNumber();
        try {
            // the parse method parses the string and
            // returns a PhoneNumber in the format of
            // specified region
            number = phoneNumberUtil.parse("+"+countryCode+telNo,"");

            // this statement prints the type of the phone
            // number
            //LOG.debug(String.valueOf(phoneNumberUtil.getNumberType(number)));
            //LOG.debug(String.valueOf(phoneNumberUtil.getRegionCodeForNumber(number)));
        }
        catch (NumberParseException e) {
            LOG.error("CANNOT PARSE TEL.NO");
            e.printStackTrace();
        }

        return phoneNumberUtil.isValidNumber(number);
    }
    //OVERLOADING IN PRACTICE: IF ARGUMENT OF WARNING ARE GIVEN, IT'LL BE SHOWN.
    public void sendWAEmergency(String countryCode, String telNo, @NonNull Location lastKnownLocation,boolean debugmode){
        if (debugmode==true)         LOG.info("TELNO COUNTRY CODE : "+ countryCode + " TELNO : "+ telNo +" Location : "+ lastKnownLocation.toString());
        if(isTelNoValid(countryCode,telNo)==true){
            String uri = "http://maps.google.com/maps?saddr=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            //"+393291876000"
            whatsappIntent.putExtra("jid",countryCode +telNo + "@s.whatsapp.net");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, "EMERGENCY WARNING:" + uri+"\n"+GBApplication.getContext().getString(R.string.accessibility_fingerprint));

            try {
                getContext().startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                LOG.error("Whatsapp have not been installed");
                //GB.toast(getBaseContext(), ("Whatsapp have not been installed."), 2000, GB.WARN);
            }
        }
    }
    public void sendWAEmergency(String countryCode, String telNo, @NonNull Location lastKnownLocation, String reasoning,boolean debugmode) {
        if (debugmode == true)
            LOG.info("TELNO COUNTRY CODE : " + countryCode + " TELNO : " + telNo + " Location : " + lastKnownLocation.toString() + " Reasoning : " + reasoning);
        if (isTelNoValid(countryCode, telNo) == true) {
            String uri = "http://maps.google.com/maps?saddr=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            //"+393291876000"
            whatsappIntent.putExtra("jid", countryCode + telNo + "@s.whatsapp.net");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, "EMERGENCY WARNING: " + reasoning + " " + uri+"\n"+GBApplication.getContext().getString(R.string.accessibility_fingerprint));
            try {
                getContext().startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                LOG.error("Whatsapp have not been installed");
                //GB.toast(getBaseContext(), ("Whatsapp have not been installed."), 2000, GB.WARN);
            }
        }

    }
    public void sendWAEmergencyTesting() {
        String countryCode = getPrefs().getString("emergency_hr_telno_cc1", "");
        String telNo= getPrefs().getString("emergency_hr_telno1", "");
          if (isTelNoValid(countryCode, telNo) == true) {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            //"+393291876000"
            whatsappIntent.putExtra("jid", countryCode + telNo + "@s.whatsapp.net");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, "TESTING THE SENDING OF WHATSAPP MESSAGE"+ "\n"+GBApplication.getContext().getString(R.string.accessibility_fingerprint));
            try {
                getContext().startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                LOG.error("Whatsapp have not been installed");
                //GB.toast(getBaseContext(), ("Whatsapp have not been installed."), 2000, GB.WARN);
            }
        }
    }

}
