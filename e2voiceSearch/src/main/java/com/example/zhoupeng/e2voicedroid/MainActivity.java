package com.example.zhoupeng.e2voicedroid;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {



    private static final String TAG = "MainActivity";
//    private Button googleDemoButton;
//    private Button textDemoButton;
//    private Button textPollyDemoButton;
    private Button speechLexButton;
    private TextView versionText;

    private View mLayout;
    private Context appContext;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.main_layout);
        appContext = getApplicationContext();

//        googleDemoButton = (Button) findViewById(R.id.goto_reg_google);
//        textDemoButton = (Button) findViewById(R.id.goto_reg_lex_mix);
//        textPollyDemoButton = (Button) findViewById(R.id.goto_reg_lex_mix_polly);
        speechLexButton = (Button) findViewById(R.id.goto_reg_lex_polly);
        versionText = (TextView)findViewById(R.id.answer);

//        initTitle();

//        textDemoButton.setOnClickListener(this);
        speechLexButton.setOnClickListener(this);
//        textPollyDemoButton.setOnClickListener(this);
//        googleDemoButton.setOnClickListener(this);

        requestMicroPermission();

        checkNewVersion();

        Intent voiceIntent = new Intent(this, LexPolly.class);
    }

    private void initTitle()
    {
        TextView textView = (TextView)findViewById(R.id.main_title);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("CHASE ").append(" ");
        builder.setSpan(new ImageSpan(this, R.drawable.chase_icon_32), builder.length() - 1, builder.length(), 0);
        builder.append(" Assist");
        textView.setText(builder);
    }
    @Override
    public void onClick(final View v) {
        switch ((v.getId())) {
//            case R.id.goto_reg_lex_mix:
//                Intent textIntent = new Intent(this, lexMix.class);
//                startActivity(textIntent);
//                break;
//            case R.id.goto_reg_lex_mix_polly:
//                Intent textPollyIntent = new Intent(this, LexMixPolly.class);
//                startActivity(textPollyIntent);
//                break;
//            case R.id.goto_reg_google:
//                Intent googleIntent = new Intent(this, GoogleVoice.class);
//                startActivity(googleIntent);
//                break;
            case R.id.goto_reg_lex_polly:
                Intent voiceIntent = new Intent(this, LexPolly.class);
                startActivity(voiceIntent);
                break;
        }
    }


    private static final int REQUEST_RECORD_AUDIO = 0;
    private void requestMicroPermission() {
        Log.i(TAG, "Micro permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            Snackbar.make(mLayout, R.string.permission_mp_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    REQUEST_RECORD_AUDIO);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
        }
    }

    String searchUrl = "https://s3.amazonaws.com/aws-website-aws-poster-hmu15/e2voice/androidAppVersion.txt";
    private void checkNewVersion()
    {
        try {
            URL url = new URL(searchUrl);
            new MainActivity.VoiceSearchTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    class VoiceSearchTask extends AsyncTask<URL,Void,String> {
        protected String doInBackground(URL... urls) {
            return doSearch(urls[0]);
        }

        private String doSearch(URL url)
        {
            String s = "";
            HttpURLConnection urlConnection = null;
            BufferedReader reader=null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.connect();
                int status = urlConnection.getResponseCode();

                switch (status) {
                    case 200:
                    case 201:
                        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line+"\n");
                        }
                        br.close();
                        s = sb.toString();
                        break;
                    default:
                        s = "error status: "+status;
                }
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                s = e.getMessage();
            } finally {
                if(reader != null){
                    try
                    {
                        reader.close();
                    }catch(Exception ex) {}
                }

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return s;
        }
        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
        }

        protected void onPostExecute(String result) {

            if(!(result == null || result.isEmpty() || result.contains("error")))
            {
                if(!result.startsWith(appContext.getResources().getString(R.string.app_version)))
                {
                    //new version detected.
                    versionText.setClickable(true);
                    versionText.setMovementMethod(LinkMovementMethod.getInstance());
                    String text = "New version is available for download from <a href='https://s3.amazonaws.com/aws-website-aws-poster-hmu15/e2voice/e2voiceSearch.apk'>here</a>";
                    versionText.setText(Html.fromHtml(text));
                    return;
                }
            }
            versionText.setText("");
        }
    }
}
