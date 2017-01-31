package com.example.zhoupeng.e2voicedroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GoogleVoice extends Activity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_CODE = 1234;

    TextView Speech;
    TextView tvAnswer;
    Dialog match_text_dialog;
    ListView textlist;
    private TextToSpeech tts;
    Button Start;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    ArrayList<String> matches_text;

    private Context appContext;

    private String userTextInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_voice);

        appContext = getApplicationContext();

        Start = (Button) findViewById(R.id.start_reg);
        Speech = (TextView) findViewById(R.id.speech);
        tvAnswer = (TextView) findViewById(R.id.answer);

        tts = new TextToSpeech(this, this);

        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Toast.makeText(getApplicationContext(), "Please Connect to Internet", Toast.LENGTH_LONG).show();
                }
            }

        });

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
//                speakOut("Welcome to Chase help! This is an E2 project.");
            }

        } else {
            Log.e("TTS", "Initialization Failed!");
        }

    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }


    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isAvailable() && net.isConnected();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

//            match_text_dialog = new Dialog(MainActivity.this);
//            match_text_dialog.setContentView(R.layout.dialog_matches_frag);
//            match_text_dialog.setTitle("Select Matching Text");
//            textlist = (ListView) match_text_dialog.findViewById(R.id.list);
            matches_text = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches_text != null && matches_text.size() > 0) {
//                if (matches_text.size() == 1) {
                processSpeechText(matches_text.get(0));
//                } else {
//                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                            android.R.layout.simple_list_item_1, matches_text);
//                    textlist.setAdapter(adapter);
//                    textlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                        @Override
//                        public void onItemClick(AdapterView<?> parent, View view,
//                                                int position, long id) {
//                            processSpeechText(matches_text.get(position));
//                            match_text_dialog.hide();
//                        }
//                    });
//                    match_text_dialog.show();
//                }
            } else {
                speakOut("Sorry, we did not get you.");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //    String searchUrl = "http://search-chasing-search-rsldk7myw4hrrerb4azqauoz54.us-east-1.cloudsearch.amazonaws.com/2013-01-01/search?q=";
    String searchUrl = "http://search-imdb-kg6d76awb45nvonriramzbx6si.us-east-1.cloudsearch.amazonaws.com/2013-01-01/search?q=";

    private void processSpeechText(String speechText) {
        userTextInput = speechText;

        Speech.setText(speechText);
        speechText = NLPProcess(speechText);
        doSearch(speechText);
    }

    private String NLPProcess(String speechText){
        return "actors:'" + URLEncoder.encode(speechText) + "'&q.parser=structured&sort=_score+desc";
    }
    protected void doSearch(String speechText) {
        try {
            URL url = new URL(searchUrl + speechText);
            new VoiceSearchTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            tvAnswer.setText(e.getMessage());
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
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.connect();
                int status = urlConnection.getResponseCode();

                switch (status) {
                    case 200:
                    case 201:
                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                                sb.append("\n");
                            }
                        }
                        s = sb.toString();
                        break;
                    default:
                        s = "error status: "+status;
                }
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                s = e.getMessage();
            } finally {
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

            String testToSpeak = "Sorry, we did not found anything.";
            if(result == null || result.isEmpty() || result.contains("error"))
            {

            }
            else
            {
                try {
                    List<String> titles = new ArrayList<>();
                    JSONObject jObject = new JSONObject(result);
                    JSONObject hits = jObject.getJSONObject("hits");
                    if(hits!=null)
                    {
                        int found = hits.getInt("found");
                        JSONArray hit = hits.getJSONArray("hit");

                        for (int i = 0; i < hit.length(); i++) {
                            JSONObject c = hit.getJSONObject(i);
                            JSONObject fields = c.getJSONObject("fields");
                            String title = fields.getString("title");
                            if(title!=null)
                                titles.add(title);
                        }
                    }
                    if(titles.size()>0){
                        testToSpeak = (titles.size() + " movies found for "+ Speech.getText() + "."+ titles.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            tvAnswer.setText(testToSpeak);
            speakOut(testToSpeak);
        }
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void exit() {
        finish();
    }
}
