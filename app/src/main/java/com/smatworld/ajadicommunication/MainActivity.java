package com.smatworld.ajadicommunication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private boolean isOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isOnline = checkInternetTask();

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        if(preferences.getInt(getString(R.string.is_first_run), -1) == -1){
            editor.putInt(getString(R.string.is_first_run), 1);
            editor.apply();
        }
        Log.i("AppInfo", "isFirstRun: " + preferences.getInt(getString(R.string.is_first_run), -1));

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int isFirstRun = preferences.getInt(getString(R.string.is_first_run), -1);
                if (isFirstRun == 1) {
                    if (isOnline) {
                        editor.putInt(getString(R.string.is_first_run), 0);
                        editor.apply();
                        startActivity(new Intent(MainActivity.this, WebActivity.class));
                    } else {
                        Toast.makeText(MainActivity.this, R.string.internet_error, Toast.LENGTH_LONG).show();
                        isOnline = checkInternetTask();
                    }
                } else
                    startActivity(new Intent(MainActivity.this, WebActivity.class));
            }
        });
    }

    private boolean checkInternetTask(){
        try {
            return new InternetAccessAsyncTask().execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class InternetAccessAsyncTask extends AsyncTask<Void, Void, Boolean>{

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
            } catch (IOException e) { return false; }
        }
    }
}
