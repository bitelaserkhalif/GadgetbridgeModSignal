package blk.freeyourgadget.gadgetbridge.devices.huami;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import blk.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import blk.freeyourgadget.gadgetbridge.devices.DataWrapper;
import blk.freeyourgadget.gadgetbridge.util.GB;

public class HuamiBrowserActivity extends AbstractGBActivity {
    private WebView mWebview ;    ProgressBar bar;

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryActivity.class);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huami_web_browser_popup);
        Intent intent = getIntent();
        bar =(ProgressBar) findViewById(R.id.progressBar2);

        mWebview  = new WebView(this);
        mWebview.getSettings().setJavaScriptEnabled(true); // enable javascript
        HashMap<String, String> url_map = (HashMap<String, String>) intent.getSerializableExtra("url_map");

        final Activity activity = this;

        mWebview.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
            }
            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                bar.setVisibility(View.GONE);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }

            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            // MAGIC HAPPEN HERE. https://stackoverflow.com/questions/35569047/how-can-load-https-url-without-use-of-ssl-in-android-webview
            // SO WHAT HAPPENED HERE IS THAT XIAOMI USES SSL ERROR, HOSTNAME MISMATCH.
            // WE CAN EXPLOIT THIS TO HARVEST THE URL.
                String message = "SSL Certificate error."; //GENERIC SSL ERROR PROBLEM
                switch (error.getPrimaryError()) {//LIST OF ERRORS
                    case SslError.SSL_UNTRUSTED:
                        message = "The certificate authority is not trusted.";
                        break;
                    case SslError.SSL_EXPIRED:
                        message = "The certificate has expired.";
                        break;
                    case SslError.SSL_IDMISMATCH:
                        message = "The certificate Hostname mismatch.";//this is it!
                        break;
                    case SslError.SSL_NOTYETVALID:
                        message = "The certificate is not yet valid.";
                        break;
                }
                LOG.debug (message);//just message debug.
                LOG.debug (view.getUrl());//at this point the url is harvested.
                GB.toast(getBaseContext(), getString(R.string.toast_browser_keygen_xiaomi_acquire), 2000, 0);
                intent.putExtra("keygen_url", view.getUrl());
                intent.putExtra("ssl_message", message);
                setResult(Activity.RESULT_OK, intent);
                finish();

                /* unused for obvious reasons.
                AlertDialog.Builder builder = new AlertDialog.Builder(activity); //INIT THE ALERT DIALOG PT.1
                AlertDialog alertDialog = builder.create(); //INIT THE ALERT DIALOG PT.2
                message += " Do you want to continue anyway?";
                alertDialog.setTitle("SSL Certificate Error");
                alertDialog.setMessage(message);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Ignore SSL certificate errors
                        handler.proceed();
                    }
                });

                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        handler.cancel();
                    }
                });
                alertDialog.show();//SHOW THE ALERT. DOES NOT MATTER ANYWAY, SO WE SHUT THIS OFF.
                */
            }

        });
        LOG.debug(String.valueOf(url_map.toString()));

        mWebview.loadUrl(String.valueOf(Uri.parse(url_map.get("login_xiaomi"))));
        setContentView(mWebview);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebview.canGoBack()) {
            mWebview.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    }
