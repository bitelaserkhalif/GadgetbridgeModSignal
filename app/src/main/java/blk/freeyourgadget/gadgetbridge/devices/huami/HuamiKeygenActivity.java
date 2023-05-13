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

package blk.freeyourgadget.gadgetbridge.devices.huami;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import blk.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.util.GB;

public class HuamiKeygenActivity  extends AbstractGBActivity  {
    private TextView message;
    private WebView mWebview ;
    private EditText keygenresult;
    private Button buttonxiaomilogin;
    private Switch huamimodeSwitch;
    private GBDeviceCandidate deviceCandidate;
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryActivity.class);

    boolean isXiaomis = false;
    Map<String, String> url_map = createUrlMap();
    Map<String, Map<String, String>> payload_map = createPayloadMap();
    private boolean isXiaomi = false;
    ActivityResultLauncher<Intent> ActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    //doSomeOperations();
                    String keygen_url = data.getStringExtra("keygen_url");
                    generate_xiaomi_token(keygen_url);
                }
            });
    public static Map<String, String> createUrlMap() {
        Map<String,String> myMap = new HashMap<String,String>();
        myMap.put("login_xiaomi", "https://account.xiaomi.com/oauth2/authorize?skip_confirm=false&client_id=2882303761517383915&pt=0&scope=1+6000+16001+20000&redirect_uri=https%3A%2F%2Fhm.xiaomi.com%2Fwatch.do&_locale=en_US&response_type=code");
        myMap.put("tokens_amazfit", "https://api-user.huami.com/registrations/{user_email}/tokens");
        myMap.put("login_amazfit", "https://account.huami.com/v2/client/login");
        myMap.put("devices", "https://api-mifit-us2.huami.com/users/{user_id}/devices");
        myMap.put("agps", "https://api-mifit-us2.huami.com/apps/com.huami.midong/fileTypes/{pack_name}/files");
        myMap.put("data_short", "https://api-mifit-us2.huami.com/users/{user_id}/deviceTypes/4/data");
        myMap.put("logout", "https://account-us2.huami.com/v1/client/logout");
        myMap.put("fw_updates", "https://api-mifit-us2.huami.com/devices/ALL/hasNewVersion");
        return myMap;
    }

    public static Map<String, Map<String, String>> createPayloadMap() {
        Map<String,Map<String, String>> myMap = new HashMap<String,Map<String, String>>();
        Map<String, String> inner_config1 = new HashMap<>();
        Map<String, String> inner_config2 = new HashMap<>();
        Map<String, String> inner_config3 = new HashMap<>();
        Map<String, String> inner_config4 = new HashMap<>();
        Map<String, String> inner_config5 = new HashMap<>();
        Map<String, String> inner_config6 = new HashMap<>();
        Map<String, String> inner_config7 = new HashMap<>();
        Map<String, String> inner_config8 = new HashMap<>();

        myMap.put("login_xiaomi", null);

        inner_config1.put("state", "REDIRECTION");
        inner_config1.put("client_id", "HuaMi");
        inner_config1.put("password", null);
        inner_config1.put("redirect_uri", "https://s3-us-west-2.amazonws.com/hm-registration/successsignin.html");
        inner_config1.put("token", "access");
        inner_config1.put("country_code", "US");
        myMap.put("tokens_amazfit", inner_config1);


        inner_config2.put("dn", "account.huami.com,api-user.huami.com,app-analytics.huami.com,api-watch.huami.com,api-analytics.huami.com,api-mifit.huami.com");
        inner_config2.put("app_version", "5.9.2-play_100355");
        inner_config2.put("source", "com.huami.watch.hmwatchmanager");
        inner_config2.put("country_code", null);
        inner_config2.put("device_id", null);
        inner_config2.put("third_name", null);
        inner_config2.put("lang", "en");
        inner_config2.put("device_model", "android_phone");
        inner_config2.put("allow_registration", "false");
        inner_config2.put("app_name", "com.huami.midong");
        inner_config2.put("code", null);
        inner_config2.put("grant_type", null);
        myMap.put("login_amazfit", inner_config2);

        inner_config3.put("apptoken", null);
        myMap.put("devices", inner_config3);

        inner_config4.put("apptoken", null);
        myMap.put("agps", inner_config4);

        inner_config5.put("apptoken", null);
        inner_config5.put("startDay", null);
        inner_config5.put("endDay", null);
        myMap.put("data_short", inner_config5);

        inner_config6.put("login_token", null);
        myMap.put("logout", inner_config6);

        inner_config7.put("productionSource", null);
        inner_config7.put("deviceSource", null);
        inner_config7.put("fontVersion", "0");
        inner_config7.put("fontFlag", "0");
        inner_config7.put("appVersion", "5.9.2-play_100355");
        inner_config7.put("firmwareVersion", null);
        inner_config7.put("hardwareVersion", null);
        inner_config7.put("support8Bytes", "true");
        myMap.put("fw_updates", inner_config7);

        return myMap;
    }

    public String  randomDeviceId (){
        Random r = new Random();
        int low = 0;
        int high = 255;
        return        String.format("02:00:00:%02x:%02x:%02x",r.nextInt(high-low) + low,r.nextInt(high-low) + low,r.nextInt(high-low) + low);
//randomizer works as intended
    }
    /*
    'login_xiaomi': "https://account.xiaomi.com/oauth2/authorize?skip_confirm=false&client_id=2882303761517383915&pt=0&scope=1+6000+16001+20000&redirect_uri=https%3A%2F%2Fhm.xiaomi.com%2Fwatch.do&_locale=en_US&response_type=code");
    'tokens_amazfit': 'https://api-user.huami.com/registrations/{user_email}/tokens',
    'login_amazfit': 'https://account.huami.com/v2/client/login',
    'devices': 'https://api-mifit-us2.huami.com/users/{user_id}/devices',
    'agps': 'https://api-mifit-us2.huami.com/apps/com.huami.midong/fileTypes/{pack_name}/files',
    'data_short': 'https://api-mifit-us2.huami.com/users/{user_id}/deviceTypes/4/data',
    'logout': 'https://account-us2.huami.com/v1/client/logout',
    'fw_updates': 'https://api-mifit-us2.huami.com/devices/ALL/hasNewVersion'

    data being sent:
    {'dn': 'account.huami.com,api-user.huami.com,app-analytics.huami.com,api-watch.huami.com,api-analytics.huami.com,api-mifit.huami.com', 'app_version': '5.9.2-play_100355', 'source': 'com.huami.watch.hmwatchmanager', 'country_code': 'US', 'device_id': '02:00:00:3e:51:5b', 'third_name': 'mi-watch', 'lang': 'en', 'device_model': 'android_phone', 'allow_registration': 'false', 'app_name': 'com.huami.midong', 'code': ['ALSG_CLOUDSRV_??????????????'], 'grant_type': 'request_token'}
 */

    protected void onCreate(Bundle savedInstanceState) {
        ActivityResultLauncher<Intent> ActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        // ...
                    }
                }
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huami_token_gen);
        message = findViewById(R.id.miband_keygen_mode);
        keygenresult = findViewById(R.id.miband_keygen_result);
        huamimodeSwitch = findViewById(R.id.switch_mode_amazfit_huami);
        buttonxiaomilogin = findViewById(R.id.button_login_miband);
        //startup value
        if (huamimodeSwitch.isChecked()){
            message.setText(R.string.devicetype_miband);
            isXiaomi = huamimodeSwitch.isChecked();
        }else{
            message.setText(R.string.devicetype_amazfit);
            isXiaomi = huamimodeSwitch.isChecked();
        }



        huamimodeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //if boolean isChecked is true change mode to xiaomi.

                if (isChecked) {
                    if (huamimodeSwitch.isChecked()) {
                        //huamimodeSwitch.setChecked(false);
                        message.setText(R.string.devicetype_miband);
                        isXiaomi = isChecked;
                    }
                }
                   else {
            //do something when unchecked
                    message.setText(R.string.devicetype_amazfit);


                    isXiaomi = isChecked;
                }

                //LOG.debug("Switch State=", "" + isChecked);

            }

        });
        buttonxiaomilogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GB.toast(getBaseContext(), getString(R.string.toast_browser_xiaomi_page), 2000, 0);
                LOG.debug(String.valueOf(Uri.parse(url_map.get("login_xiaomi"))));
                get_access_token(isXiaomi);

            }
        });
/*
this.deviceCandidate = getIntent().getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (deviceCandidate == null && savedInstanceState != null) {
            this.deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
        }
        //LOG.debug(deviceCandidate.toString());
        if (deviceCandidate == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(deviceCandidate);
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        //message.setText(device.getAliasOrName());
        //message.setText(deviceCandidate);
*/

    }
    private void get_access_token(boolean isXiaomi){
        LOG.debug(String.valueOf(isXiaomi));
        if(isXiaomi==true){
            Intent i = new Intent(this, HuamiBrowserActivity.class);
            i.putExtra("url_map", (Serializable) url_map);
            //startActivity(i);
            ActivityResultLauncher.launch(i);
          }
        else{
            //no need bcoz xiaomi focus. soon add the amazfit.
        }
    }

    private void generate_xiaomi_token(String uri){
        if(uri.isEmpty()==false){
            Uri uri_link=Uri.parse(uri);//parse URL
            String getcode = uri_link.getQueryParameter("code");
            if(getcode.isEmpty()==false){
                //ALMOST THERE!
                LOG.debug("attempt to login with the token: "+getcode);
                String login_url = url_map.get("login_amazfit");//normal
                LOG.debug("login url: "+login_url);

                LOG.debug("test "+ payload_map.toString());
                Map<String, String> data = payload_map.get("login_amazfit");
                try{
                    RequestQueue requestQueue = Volley.newRequestQueue(this);
                    String url = login_url;
                    data.put("country_code",data.get("country_code"));
                    data.put("device_id",randomDeviceId());
                    data.put("third_name","mi-watch");//later add "huami" if using "amazfit"
                    data.put("code",getcode);//code add
                    data.put("grant_type","request_token");//later add "access_token" if using "amazfit"
                    //String requestBody = new Gson().toJson(data);
                    String requestBody = data.toString();

                    LOG.debug("Requests " + requestBody);
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            LOG.debug("VOLLEY " + response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            LOG.debug("VOLLEY " + error.toString());
                        }
                    }){
                        @Override
                        public String getBodyContentType() {
                            return "application/json; charset=utf-8";
                        }

                        @Override
                        public byte[] getBody() {
                            try {
                                return requestBody == null ? null : requestBody.getBytes("utf-8");
                            } catch (UnsupportedEncodingException uee) {
                                VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                                return null;
                            }
                        }

                        @Override
                        protected Response<String> parseNetworkResponse(NetworkResponse response) {
                            String responseString = "";
                            if (response != null) {
                                responseString = String.valueOf(response.statusCode);
                                // can get more details such as response.headers
                            }
                            return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                        }
                    };
                    requestQueue.add(stringRequest);

                    }  catch (Exception e) {
                    throw new RuntimeException(e);
                }

                //keygenresult.setText(getcode);
            }
        }
        else{
            //no need bcoz xiaomi focus
        }
    }




    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putParcelable(STATE_DEVICE_CANDIDATE, deviceCandidate);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
    }

}
