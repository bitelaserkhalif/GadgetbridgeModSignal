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

import static blk.freeyourgadget.gadgetbridge.util.BondingUtil.STATE_DEVICE_CANDIDATE;
import static blk.freeyourgadget.gadgetbridge.util.GB.ERROR;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import blk.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import blk.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.util.GB;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HuamiKeygenActivity  extends AbstractGBActivity  {
    private TextView message;
    private WebView mWebview ;

    private EditText keygenresult;
    private EditText guiusername;
    private EditText guipassword;
    private TextView flavortextusername;
    private TextView flavortextpassword;
    private Button buttonxiaomilogin;
    private Switch huamimodeSwitch;
    private GBDeviceCandidate deviceCandidate;
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryActivity.class);

    boolean isXiaomis = false;
    Map<String, String> url_map = createUrlMap();
    Map<String, Map<String, String>> payload_map = createPayloadMap();
    Map<String, String> error_map = createErrorMap();
    private boolean isXiaomi = false;
    ActivityResultLauncher<Intent> ActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    //doSomeOperations();
                    String keygen_url = data.getStringExtra("keygen_url");
                    fetch_xiaomi_token(keygen_url);
                }
            });
    public static Map<String, String> createUrlMap() {
        Map<String,String> myMap = new HashMap<String,String>();
        myMap.put("login_xiaomi", "https://account.xiaomi.com/oauth2/authorize?skip_confirm=false&client_id=2882303761517383915&pt=0&scope=1+6000+16001+20000&redirect_uri=https%3A%2F%2Fhm.xiaomi.com%2Fwatch.do&_locale=en_US&response_type=code");
        myMap.put("tokens_amazfit", "https://api-user.huami.com/registrations/{user_email}/tokens");
        myMap.put("login_amazfit", "https://account.huami.com/v2/client/login");
        myMap.put("devices", "https://api-mifit-us2.huami.com/users/%s/devices?enableMultiDevice=true"); //{user_id}
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

    public static Map<String,String> createErrorMap(){
        Map<String,String> myMap = new HashMap<String,String>();
        myMap.put("0106", "106 = Verification failed, wrong token.");
        myMap.put("0113", "113 = Wrong region");
        myMap.put("0115", "115 = Account disabled.");
        myMap.put("0117", "117 = Account not registered");
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
            guiusername = findViewById(R.id.amazfit_username);
            guipassword = findViewById(R.id.amazfit_password);
            flavortextusername = findViewById(R.id.amazfit_username_message);
            flavortextpassword = findViewById(R.id.amazfit_password_message);
            guiusername.setVisibility(View.GONE);
            guipassword.setVisibility(View.GONE);
            flavortextusername.setVisibility(View.GONE);
            flavortextpassword.setVisibility(View.GONE);
            buttonxiaomilogin.setVisibility(View.VISIBLE);
        }else{
            message.setText(R.string.devicetype_amazfit);
            isXiaomi = huamimodeSwitch.isChecked();
            guiusername = findViewById(R.id.amazfit_username);
            guipassword = findViewById(R.id.amazfit_password);
            flavortextusername = findViewById(R.id.amazfit_username_message);
            flavortextpassword = findViewById(R.id.amazfit_password_message);
            guiusername.setVisibility(View.VISIBLE);
            guipassword.setVisibility(View.VISIBLE);
            flavortextusername.setVisibility(View.VISIBLE);
            flavortextpassword.setVisibility(View.VISIBLE);
            buttonxiaomilogin.setVisibility(View.GONE);
        }
        Intent intent = getIntent();

        deviceCandidate = intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (deviceCandidate == null && savedInstanceState != null) {
            deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
        }
        if (deviceCandidate == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
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
                        guiusername = findViewById(R.id.amazfit_username);
                        guipassword = findViewById(R.id.amazfit_password);
                        flavortextusername = findViewById(R.id.amazfit_username_message);
                        flavortextpassword = findViewById(R.id.amazfit_password_message);
                        guiusername.setVisibility(View.GONE);
                        guipassword.setVisibility(View.GONE);
                        flavortextusername.setVisibility(View.GONE);
                        flavortextpassword.setVisibility(View.GONE);
                        buttonxiaomilogin.setVisibility(View.VISIBLE);
                    }
                }
                   else {
            //do something when unchecked
                    message.setText(R.string.devicetype_amazfit);


                    isXiaomi = isChecked;
                    guiusername = findViewById(R.id.amazfit_username);
                    guipassword = findViewById(R.id.amazfit_password);
                    flavortextusername = findViewById(R.id.amazfit_username_message);
                    flavortextpassword = findViewById(R.id.amazfit_password_message);
                    guiusername.setVisibility(View.VISIBLE);
                    guipassword.setVisibility(View.VISIBLE);
                    flavortextusername.setVisibility(View.VISIBLE);
                    flavortextpassword.setVisibility(View.VISIBLE);
                    buttonxiaomilogin.setVisibility(View.GONE);
                }


            }

        });
        buttonxiaomilogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GB.toast(getBaseContext(), getString(R.string.toast_browser_xiaomi_page), 2000, 0);
                get_access_token(isXiaomi);

            }
        });


    }
    private void get_access_token(boolean isXiaomi){
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

    private void fetch_xiaomi_token(@NonNull String uri){
        if(uri.isEmpty()==false){
            Uri uri_link=Uri.parse(uri);//parse URL
            String getcode = uri_link.getQueryParameter("code");
            if(getcode.isEmpty()==false){
                //ALMOST THERE!
                String login_url = url_map.get("login_amazfit");//normal
                String devices = url_map.get("devices");//normal

                Map<String, String> login_amazfit = payload_map.get("login_amazfit");
                try{
                    //RequestQueue requestQueue = Volley.newRequestQueue(this);
                    String url = login_url;

                    //String requestBody = data.toString();
                    // to post, we are using x-www-form-urlencoded

                    /** This is phase 1: post procedure, generated using postman **/
                    //region Fetch user id and app id

                    String requests = String.format("country_code=%s&app_name=%s&code=%s&app_version=%s&device_id=%s&device_model=%s&grant_type=%s&allow_registration=%s&dn=%s&source=%s&third_name=%s&lang=%s",
                            "US",
                            login_amazfit.get("app_name"),
                            getcode,
                            login_amazfit.get("app_version"),
                            randomDeviceId(),
                            login_amazfit.get("device_model"),
                            "request_token",
                            login_amazfit.get("allow_registration"),
                            login_amazfit.get("dn"),
                            login_amazfit.get("source"),
                            "mi-watch",
                            login_amazfit.get("lang"));

                    post(url, requests, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            LOG.debug("failure error: "+ e.toString());
                            // Something went wrong
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseStr = response.body().string();
                                try {
                                    JSONObject jobj = new JSONObject(responseStr);
                                    if (jobj.has("error_code")){
                                        GB.toast(getBaseContext(), "LOGIN ERROR: "+ error_map.get(jobj.get("error_code").toString()), 2000, ERROR);
                                        LOG.debug("error logging in");
                                    }

                                    if (!jobj.has("token_info")){
                                        GB.toast(getBaseContext(), "LOGIN ERROR: token_info is missing", 2000, ERROR);
                                        LOG.debug("missing: token_info");
                                    }

                                    JSONObject token_info = jobj.getJSONObject("token_info");
                                    if(!token_info.has("app_token")){
                                        GB.toast(getBaseContext(), "LOGIN ERROR: app_token is missing", 2000, ERROR);
                                        LOG.debug("missing: app_token");

                                    }
                                    if(!token_info.has("login_token")){
                                        GB.toast(getBaseContext(), "LOGIN ERROR: login_token is missing", 2000, ERROR);
                                        LOG.debug("missing: login_token");
                                    }
                                    if(!token_info.has("user_id")){
                                        GB.toast(getBaseContext(), "LOGIN ERROR: user_id is missing", 2000, ERROR);
                                        LOG.debug("missing: user_id");
                                    }

                                    generate_token(token_info.getString("app_token"),token_info.getString("user_id"),devices);

                                } catch (JSONException e) {
                                    LOG.debug("failure error: "+ e.toString());
                                }

                                // Do what you want to do with the response.
                            } else {
                                LOG.debug("failed but response has been sent: "+ response.toString());
                                // Request not successful
                            }
                        }
                    });
                    //endregion


                    }

                catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }
        else{
            //no need bcoz xiaomi focus
        }
    }
    private void generate_token(String apptoken, String userid, String url_device){
        String url_device_ = String.format(url_device,userid);
        LOG.debug(url_device_);
        getToken(url_device_, apptoken, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LOG.debug("failure error: "+ e.toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    LOG.debug("response: "+ responseStr.toString());
                    try {
                        JSONObject jobj = new JSONObject(responseStr);
                        JSONArray items = jobj.getJSONArray("items");
                        Map<String, String> data = new HashMap<>();
                        for(int i = 0; i < items.length(); i++){
                            JSONObject device_info = new JSONObject(items.getJSONObject(i).getString("additionalInfo"));
                            data.put("active_status",items.getJSONObject(i).getString("activeStatus"));
                            data.put("auth_key",String.format("0x%s",device_info.getString("auth_key")));
                            data.put("mac_address",items.getJSONObject(i).getString("macAddress"));
                            data.put("device_source",items.getJSONObject(i).getString("deviceSource"));
                        }
                        //!!Only last key provided by the server is displayed!!
                        LOG.debug("response: "+ data.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                keygenresult.setText(data.get("auth_key"));
                            }
                        });
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    LOG.debug("failed but response has been sent: "+ response.toString());
                }

            }
        });
    }


    public static final MediaType URLENCODED = MediaType.parse("application/x-www-form-urlencoded");

    OkHttpClient client = new OkHttpClient();

    Call post(String url, String requests, Callback callback) {
        RequestBody body = RequestBody.create(requests,URLENCODED);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    Call getToken(String url,  String apptoken, Callback callback ) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("apptoken", apptoken)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
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
