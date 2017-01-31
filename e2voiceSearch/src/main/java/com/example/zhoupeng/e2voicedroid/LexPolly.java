package com.example.zhoupeng.e2voicedroid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.exceptions.MaxSpeechTimeOutException;
import com.amazonaws.mobileconnectors.lex.interactionkit.exceptions.NoSpeechTimeOutException;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.StringUtils;
import com.example.zhoupeng.e2voicedroid.lex.E2InteractiveVoiceView;
import com.example.zhoupeng.e2voicedroid.lex.E2InteractiveVoiceViewAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class LexPolly extends Activity
        implements E2InteractiveVoiceView.InteractiveVoiceListener {

    private static final String TAG = "VoiceActivity";
    private Context appContext;
    private Context currentContext;
    private E2InteractiveVoiceView voiceView;
    private E2InteractiveVoiceViewAdapter interactiveVoiceViewAdapter;

    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(interactiveVoiceViewAdapter != null)
        {
            interactiveVoiceViewAdapter.cancel();
        }
        setContentView(R.layout.activity_lex_polly);
        mLayout = findViewById(R.id.activity_lex_polly);

        initTitle();

        init();
        StringUtils.isBlank("notempty");

        final ListView messagesListView = (ListView) findViewById(R.id.conversationListView);
        messagesListView.setOnItemClickListener(onListItemClickListener);

        currentContext = this;
    }

    private void initTitle()
    {
        Toolbar toolbar = (Toolbar)findViewById(R.id.main_title);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_action_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit(); // close this activity and return to preview activity (if there is any)
            }
        });
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void exit() {
        if(interactiveVoiceViewAdapter != null)
            interactiveVoiceViewAdapter.doLateCancelConversation();
        Conversation.clear();
        finish();
    }

    private void init() {
        appContext = getApplicationContext();
        voiceView = (E2InteractiveVoiceView) findViewById(R.id.voiceInterface);
        voiceView.setInteractiveVoiceListener(this);
//        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
//                appContext.getResources().getString(R.string.identity_id_test),
//                Regions.fromName(appContext.getResources().getString(R.string.aws_region)));

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                appContext,
                appContext.getResources().getString(R.string.identity_id_test),
                Regions.fromName("us-east-1")
        );

        interactiveVoiceViewAdapter = voiceView.getViewAdapter();
        interactiveVoiceViewAdapter.setCredentialProvider(credentialsProvider);
        InteractionConfig config = new InteractionConfig(appContext.getString(R.string.bot_name),
                appContext.getString(R.string.bot_alias));
        config.setNoSpeechTimeoutInterval(InteractionConfig.DEFAULT_NO_SPEECH_TIMEOUT_INTERVAL*3);
        config.setMaxSpeechTimeoutInterval(InteractionConfig.DEFAULT_MAX_SPEECH_TIMEOUT_INTERVAL*3);
        interactiveVoiceViewAdapter.setInteractionConfig(config);
        interactiveVoiceViewAdapter.setAwsRegion(appContext.getString(R.string.aws_region));

        interactiveVoiceViewAdapter.welcomStartNewConversation();
    }

    @Override
    public void dialogReadyForFulfillment(final Map<String, String> slots, final String intent) {
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));
    }

    private Handler commandDelayHandler = new Handler();
    private VideoCommandProcesser videoCommandProcesser = new VideoCommandProcesser();
    private WeblinkCommandProcesser weblinkCommandProcesser = new WeblinkCommandProcesser();
    @Override
    public void onResponse(Response response) {
        Log.d(TAG, "Bot response: " + response.getTextResponse());

        boolean hasResponseCard = false;
        if(response.getSessionAttributes().containsKey("video"))
        {
            String videoUrl = response.getSessionAttributes().get("video");
            if(videoUrl!=null && !videoUrl.isEmpty())
            {
                try {
                    JSONObject obj = new JSONObject(videoUrl);
//                    displayVideoBar(obj.getString("url"));
//                    addVideoMessage(response.getTextResponse(), obj.getString("message"), obj.getString("url"));
                    addResponseCardMessage(response.getTextResponse(), obj.getString("message"), null, null, obj.getString("url"));
                    hasResponseCard = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(response.getSessionAttributes().containsKey("web"))
        {
            String videoUrl = response.getSessionAttributes().get("web");
            if(videoUrl!=null && !videoUrl.isEmpty())
            {
                try {
                    JSONObject obj = new JSONObject(videoUrl);
                    addResponseCardMessage(response.getTextResponse(), obj.getString("message"), obj.getString("url"), null, null);
                    hasResponseCard = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(response.getSessionAttributes().containsKey("additionInfo"))
        {
            String videoUrl = response.getSessionAttributes().get("additionInfo");
            if(videoUrl!=null && !videoUrl.isEmpty())
            {
                try {
                    JSONObject obj = new JSONObject(videoUrl);
                    addResponseCardMessage(response.getTextResponse(), obj.getString("message"), null, obj.getJSONArray("options"), null);
                    hasResponseCard = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(response.getSessionAttributes().containsKey("command"))
        {
            String command = response.getSessionAttributes().get("command");
            if("openCurrentContent".equalsIgnoreCase(command))
            {
                TextMessage msg = Conversation.getMessage(Conversation.getCount()-1);
                ResponseCard rc = msg.getResponseCard();
                if(rc!=null && rc.getVideoUrl()!=null)
                {
                    videoCommandProcesser.init(rc.getVideoUrl(), currentContext, interactiveVoiceViewAdapter);
                    commandDelayHandler.postDelayed(videoCommandProcesser, 500);                    ;
                    hasResponseCard = true;
                }
                else if(rc!=null && rc.getWebUrl()!=null)
                {
                    weblinkCommandProcesser.init(rc.getWebUrl(), currentContext, interactiveVoiceViewAdapter);
                    commandDelayHandler.postDelayed(weblinkCommandProcesser, 500);
                    hasResponseCard = true;
                    goBackToMain();
                }
            }
        }
        if(!hasResponseCard)
        {
            addMessage(new TextMessage(response.getTextResponse(), "rx", getCurrentTimeStamp()));
        }

//        hideSnackbar();
    }

//    private void addVideoMessage(String msg, String videoMsg, String url)
//    {
//        if(videoMsg== null || videoMsg.isEmpty())
//            videoMsg =  appContext.getResources().getString(R.string.lex_play_video);
//        ResponseCard rc = new ResponseCard();
//        rc.setVideoUrl(url);
//        rc.setAdditionMsg(videoMsg);
//        TextMessage message = new TextMessage(msg, "video", getCurrentTimeStamp());
//        message.setResponseCard(rc);
//        addMessage(message);
//    }

    private void addResponseCardMessage(String msg, String moreMsg, String weburl, JSONArray options, String videoUrl)
    {
        ResponseCard rc = new ResponseCard();
        if(videoUrl != null && !videoUrl.isEmpty())
        {
            rc.setVideoUrl(videoUrl);
            if(moreMsg== null || moreMsg.isEmpty())
                moreMsg =  appContext.getResources().getString(R.string.lex_play_video);
        }
        else if(weburl != null && !weburl.isEmpty())
        {
            rc.setWebUrl(weburl);
        }
        else if(options != null)
        {
            ArrayList<String> list = new ArrayList();
            for(int i=0; i< options.length();i++)
            {
                try {
                    list.add(options.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            moreMsg = "<p>" + moreMsg + "</p>";
            rc.setOptions(list);
        }
        rc.setAdditionMsg(moreMsg);

        TextMessage message = new TextMessage(msg, "moreInfo", getCurrentTimeStamp());
        message.setResponseCard(rc);
        addMessage(message);
    }

    private String getCurrentTimeStamp() {
        return DateFormat.getDateTimeInstance().format(new Date());
    }

    private boolean goodbyeResponse(final String responseText)
    {
        return responseText.contains("Goodbye");
    }

    @Override
    public void onError(final String responseText, final Exception e) {
        addMessage(new TextMessage(responseText, "rx", getCurrentTimeStamp()));
        if(goodbyeResponse(responseText))
        {
            goBackToMain();
        }
        Log.e(TAG, "Error: " + responseText, e);
    }

    @Override
    public void onMicrophoneError(Exception e) {
        if (e instanceof NoSpeechTimeOutException) {
//            addStatusMsg("Sorry, I can not hear you.");
        } else if (e instanceof MaxSpeechTimeOutException) {
            addStatusMsg("Thank you, see you next time.");
            goBackToMain();
        }
        else
        {
            addStatusMsg("MicrophoneError:" + e.getMessage());
        }

    }

    @Override
    public void onStartListening(int state) {
//        addStatusMsg(appContext.getResources().getString(R.string.lex_mic_start));
    }

    @Override
    public boolean onFullFilled(Response response) {
//        addStatusMsg(appContext.getResources().getString(R.string.lex_fulfilled));
        boolean ret = false;
        if(response.getIntentName().equalsIgnoreCase("GoodbyeChase") || goodbyeResponse(response.getTextResponse()))
        {
            ret = true;
        }
        return ret;
    }

    @Override
    public void onAudioPlayBackCompleted(Boolean needTerminateConversation) {
        if(needTerminateConversation)
        {
            onBackPressed();
        }
    }

    private void addMessage(final TextMessage message) {
        Conversation.add(message);
        final MessagesListAdapter listAdapter = new MessagesListAdapter(getApplicationContext());
        final ListView messagesListView = (ListView) findViewById(R.id.conversationListView);
        messagesListView.setDivider(null);
        messagesListView.setAdapter(listAdapter);
        messagesListView.setSelection(listAdapter.getCount() - 1);
    }

    private AdapterView.OnItemClickListener onListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                long arg3) {
            // TODO Auto-generated method stub
            //do your job here, position is the item position in ListView
            TextMessage msg = Conversation.getMessage(position);
            ResponseCard rc = msg.getResponseCard();
            if(rc != null && rc.getVideoUrl() != null)
            {
                videoCommandProcesser.init(rc.getVideoUrl(), currentContext, interactiveVoiceViewAdapter);
                videoCommandProcesser.startPoup();
            }
            else if(rc != null && rc.getWebUrl() != null)
            {
                weblinkCommandProcesser.init(rc.getWebUrl(), currentContext, interactiveVoiceViewAdapter);
                weblinkCommandProcesser.startPoup();
                goBackToMain();
            }
        }
    };


    private void addStatusMsg(String msg)
    {
        addMessage(new TextMessage(msg, "sys", getCurrentTimeStamp()));
    }

    private void goBackToMain()
    {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onBackPressed();
            }
        }, 1000);
    }

//    private Snackbar snackbar;
//    private void displayVideoBar(final String videoUrl)
//    {
//        snackbar = Snackbar.make(mLayout, R.string.lex_play_video,
//                Snackbar.LENGTH_INDEFINITE)
//                .setAction(R.string.ok, new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        interactiveVoiceViewAdapter.cancel();
//                        openVideoPopup(videoUrl);
//                    }
//                });
//        snackbar.show();
//    }
//
//    private void hideSnackbar(){
//        if(snackbar !=null && snackbar.isShown()){
//            snackbar.dismiss();
//        }
//    }



    private void doLateStartNewConversation()
    {
        interactiveVoiceViewAdapter.autoStartNewConversation();
    }



}