package com.example.zhoupeng.e2voicedroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.AudioPlaybackListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexrts.model.DialogState;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class lexMix extends Activity implements TextToSpeech.OnInitListener{

    private static final String TAG = "TextActivity";
    private static final int REQUEST_CODE = 1234;

    private TextToSpeech tts;
    ImageButton Start;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient googleClient;

    ArrayList<String> matches_text;

    private Context appContext;

    private InteractionClient lexInteractionClient;
    private boolean inConversation;

    private String userTextInput;

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lex_mix2);

        appContext = getApplicationContext();

        Start = (ImageButton) findViewById(R.id.start_reg_mix);

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

        googleClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        initializeLexSDK();
    }

    private void initializeLexSDK() {
        Log.d(TAG, "Lex Client");
        // Cognito Identity Broker is the credentials provider.
//        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
//                appContext.getResources().getString(R.string.identity_id_test),
//                Regions.fromName("us-east-1"));

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                appContext,
                appContext.getResources().getString(R.string.identity_id_test),
                Regions.fromName("us-east-1")
        );

        // Create Lex interaction client.
        lexInteractionClient = new InteractionClient(getApplicationContext(),
                credentialsProvider,
                Regions.US_EAST_1,
                appContext.getResources().getString(R.string.bot_name),
                appContext.getResources().getString(R.string.bot_alias));
        lexInteractionClient.setAudioPlaybackListener(audioPlaybackListener);
        lexInteractionClient.setInteractionListener(interactionListener);
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
        if (net != null && net.isAvailable() && net.isConnected()) {
            return true;
        } else {
            return false;
        }
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

    private void processSpeechText(String speechText) {
        userTextInput = speechText;
        textEntered();
//        Speech.setText(speechText);
//        speechText = NLPProcess(speechText);
//        doSearch(speechText);
    }

    final AudioPlaybackListener audioPlaybackListener = new AudioPlaybackListener() {
        @Override
        public void onAudioPlaybackStarted() {
            Log.d(TAG, " -- Audio playback started");
        }

        @Override
        public void onAudioPlayBackCompleted() {
            Log.d(TAG, " -- Audio playback ended");
        }

        @Override
        public void onAudioPlaybackError(Exception e) {
            Log.d(TAG, " -- Audio playback error", e);
        }
    };

    private void showToast(final String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_LONG).show();
        Log.d(TAG, message);
    }

    final InteractionListener interactionListener = new InteractionListener() {
        @Override
        public void onReadyForFulfillment(final Response response) {
            Log.d(TAG, "Transaction completed successfully");
            addMessage(new TextMessage(response.getTextResponse(), "rx", getCurrentTimeStamp()));
            inConversation = false;
        }

        @Override
        public void promptUserToRespond(final Response response,
                                        final LexServiceContinuation continuation) {
            String responseText = response.getTextResponse();
            addMessage(new TextMessage(response.getTextResponse(), "rx", getCurrentTimeStamp()));
            readUserText(continuation);

            speakOut(responseText);
        }

        @Override
        public void onInteractionError(final Response response, final Exception e) {
            if (response != null) {
                if (DialogState.Failed.toString().equals(response.getDialogState())) {
                    addMessage(new TextMessage(response.getTextResponse(), "rx",
                            getCurrentTimeStamp()));
                    inConversation = false;
                } else {
                    addMessage(new TextMessage("Please retry", "rx", getCurrentTimeStamp()));
                }
            } else {
                showToast("Error: " + e.getMessage());
                Log.e(TAG, "Interaction error", e);
                inConversation = false;
            }
        }
    };

    private LexServiceContinuation convContinuation;
    private void readUserText(final LexServiceContinuation continuation) {
        convContinuation = continuation;
        inConversation = true;
    }

    private void textEntered() {
        // showToast("Text input not implemented");
        String text = userTextInput;
        if (!inConversation) {
            Log.d(TAG, " -- New conversation started");
            startNewConversation();
            addMessage(new TextMessage(text, "tx", getCurrentTimeStamp()));
            lexInteractionClient.textInForTextOut(text, null);
            inConversation = true;
        } else {
            Log.d(TAG, " -- Responding with text: " + text);
            addMessage(new TextMessage(text, "tx", getCurrentTimeStamp()));
            convContinuation.continueWithTextInForTextOut(text);
        }
        clearTextInput();
    }

    private void startNewConversation() {
        Log.d(TAG, "Starting new conversation");
        Conversation.clear();
        inConversation = false;
        clearTextInput();
    }

    private void addMessage(final TextMessage message) {
        Conversation.add(message);
    }

    private void clearTextInput() {
        userTextInput = ("");
    }

    private String getCurrentTimeStamp() {
        return DateFormat.getDateTimeInstance().format(new Date());
    }

}
