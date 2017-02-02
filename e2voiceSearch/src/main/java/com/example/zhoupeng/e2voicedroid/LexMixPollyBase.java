package com.example.zhoupeng.e2voicedroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.AudioPlaybackListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexrts.model.DialogState;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class LexMixPollyBase extends Activity implements TextToSpeech.OnInitListener {

    private static final String TAG = "TextActivity";
    private static final int REQUEST_CODE = 1234;

    ImageButton Start;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient googleClient;

    List<String> matches_text;

    private Context appContext;

    private InteractionClient lexInteractionClient;
    private boolean inConversation;

    private String userTextInput;

    MediaPlayer mediaPlayer;

    private static final String KEY_SELECTED_VOICE_POSITION = "SelectedVoicePosition";
    private static final String KEY_VOICES = "Voices";
    private static final String KEY_SAMPLE_TEXT = "SampleText";

    private LexServiceContinuation convContinuation;

    private GetPollyVoices getPollyVoices;

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lex_mix_polly);

        appContext = getApplicationContext();

        Start = (ImageButton) findViewById(R.id.start_reg_mix_polly);

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

    protected void initializeLexSDK() {
        Log.d(TAG, "Lex Client");

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

        // Create a client that supports generation of presigned URLs.
        this.getPollyVoices = new GetPollyVoices(appContext, new AmazonPollyPresigningClient(credentialsProvider));

        setupNewMediaPlayer();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupVoicesSpinner();
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e("TTS", "Initialization Failed!");
        }
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        super.onDestroy();
    }

    private void speakOut(String text) {
        speakByPolly(text);
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isAvailable() && net.isConnected();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            matches_text = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches_text != null && matches_text.size() > 0) {
                processSpeechText(matches_text.get(0));
            } else {
                speakOut("Sorry, we did not get you.");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processSpeechText(String speechText) {
        userTextInput = speechText;
        textEntered();
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

    //polly speech

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    private void speakByPolly(String textToRead) {

        Voice selectedVoice = (Voice) this.getPollyVoices.getVoicesSpinner().getSelectedItem();

        if (textToRead.trim().isEmpty()) {
            textToRead = "No voice available.";
        }

        // Create speech synthesis request.
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set text to synthesize.
                        .withText(textToRead)
                        // Set voice selected by the user.
                        .withVoiceId(selectedVoice.getId())
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

        // Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                this.getPollyVoices.getClient().getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

        Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

        // Create a media player to play the synthesized audio stream.
        if (mediaPlayer.isPlaying()) {
            setupNewMediaPlayer();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
            Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
        }

        // Start the playback asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();
    }

    private void setupVoicesSpinner() {
        // This is probably better as preExecute but needs the findByViewId
        this.getPollyVoices.setVoicesSpinner((Spinner) findViewById(R.id.voicesSpinner));

        // Asynchronously get available Polly voices.
        this.getPollyVoices.execute();
    }

}
