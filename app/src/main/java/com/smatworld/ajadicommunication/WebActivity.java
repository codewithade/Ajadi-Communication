package com.smatworld.ajadicommunication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class WebActivity extends AppCompatActivity {
    private static final String DEBUG_TAG = "AppInfo";
    private static final String URL = "https://dami-communication.web.app/";

    private static ProgressDialog progressDialog;
    private Context context;
    private static String[] errorList;
    private WebView webView;

    @Override
    public void onBackPressed() {
        if (webView.canGoBack())
            webView.goBack();
        else
            super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        context = WebActivity.this;
        errorList = getResources().getStringArray(R.array.error_list);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Data...");
        progressDialog.setCancelable(false);

        webView = findViewById(R.id.webview);
        webView.setWebChromeClient(new myWebChromeClient());
        webView.setWebViewClient(new myWebviewClient(context));


        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        String path = getApplicationContext().getFilesDir().getAbsolutePath() + "/cache";
        webSettings.setAppCachePath(path);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (!isOnline()) {
            Log.i("AppInfo", "I'm offline");
            Log.i("AppInfo", "Path: " + path);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webView.loadUrl(URL);
        checkNetworkConnection(this);
    }

    private void checkNetworkConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiConn = false;
        boolean isMobileConn = false;
        if (connMgr != null) {
            for (Network network : connMgr.getAllNetworks()) {
                NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    isWifiConn |= networkInfo.isConnected();
                }
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    isMobileConn |= networkInfo.isConnected();
                }
            }
        }
        Log.d(DEBUG_TAG, "Wifi connected: " + isWifiConn);
        Log.d(DEBUG_TAG, "Mobile connected: " + isMobileConn);
    }

    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        return (networkInfo != null && networkInfo.isConnected());
    }

    private static class myWebviewClient extends WebViewClient {
        Context context;

        myWebviewClient(Context context) {
            this.context = context;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }


        @Override
        public void onReceivedError(final WebView webView, final WebResourceRequest request, final WebResourceError error) {
            final String originalURL = webView.getOriginalUrl();
            final boolean isOnline = checkInternetTask();

            // resets the webView and release resources making the webView blank
            webView.loadUrl("about:blank");

            // display random error messages
            int errorCode = new Random().nextInt(errorList.length);
            Log.i("AppInfo", "Original URL: " + originalURL);
            Log.i("AppInfo", "isOnline: " + isOnline);

            String buildURL = URL + "content/" + ".html";

            new AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_info_outline_black_24dp)
                    .setTitle("Load Error")
                    .setMessage(errorList[errorCode])
                    .setCancelable(false)
                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i("AppInfo", "Positive Button");
                            //webView.loadUrl(request.getUrl().toString());
                            // checks for internet connectivity
                            if (isOnline)
                                // webView.loadUrl(originalURL);
                                webView.loadUrl(URL);
                            else
                                onReceivedError(webView, request, error);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i("AppInfo", "Negative Button");
                            /*if (webView.canGoBack())
                                webView.loadUrl(URL);
                            else*/
                            context.startActivity(new Intent(context, MainActivity.class));
                        }
                    }).create().show();
            String requestURL = request.getUrl().toString();
            Log.i("AppInfo", requestURL);/*
            Log.i("AppInfo", "Length: " + requestURL.length());
            Log.i("AppInfo", "Length: " + requestURL.substring(39, requestURL.length()-4));*/
            Log.i("AppInfo", "Error: " + error.getDescription());
        }
    }

    private static class myWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (progress < 100) {
                progressDialog.show();
            }
            if (progress == 100) {
                progressDialog.dismiss();
            }
        }
    }

    private static boolean checkInternetTask() {
        try {
            return new InternetAccessAsyncTask().execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class InternetAccessAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean internet) {
            super.onPostExecute(internet);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                int timeoutMs = 1500;
                Socket sock = new Socket();
                SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);
                sock.connect(sockaddr, timeoutMs);
                sock.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}
