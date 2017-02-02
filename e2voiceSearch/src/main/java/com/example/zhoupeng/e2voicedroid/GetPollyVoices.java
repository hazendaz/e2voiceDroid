package com.example.zhoupeng.e2voicedroid;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.Voice;

import java.util.ArrayList;
import java.util.List;

public class GetPollyVoices extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "VoicesActivity";

    private List<Voice> voices;

    private int selectedPosition;

    private Spinner voicesSpinner;

    private AmazonPollyPresigningClient client;

    private Context appContext;

    public GetPollyVoices(Context appContext, AmazonPollyPresigningClient client) {
        this.appContext = appContext;
        this.client = client;
    }

    public AmazonPollyPresigningClient getClient() {
        return client;
    }

    public void setVoicesSpinner(Spinner voicesSpinner) {
        this.voicesSpinner = voicesSpinner;
    }

    public Spinner getVoicesSpinner() {
        return voicesSpinner;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (voices != null) {
            return null;
        }

        // Create describe voices request.
        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

        DescribeVoicesResult describeVoicesResult;
        try {
            // Synchronously ask the Polly Service to describe available TTS voices.
            describeVoicesResult = client.describeVoices(describeVoicesRequest);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to get available voices. " + e.getMessage());
            return null;
        }

        // Get list of voices from the result.
        List<Voice> fullVoices = describeVoicesResult.getVoices();
        voices = new ArrayList<>();
        for(Voice item : fullVoices) {
            if("en-US".equalsIgnoreCase(item.getLanguageCode()) ) {
                voices.add(item);
            }
        }

        // Log a message with a list of available TTS voices.
        Log.i(TAG, "Available Polly voices: " + voices);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (voices == null) {
            return;
        }

        voicesSpinner.setAdapter(new SpinnerVoiceAdapter(appContext, voices));

        voicesSpinner.setVisibility(View.VISIBLE);

        voicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view == null) {
                    return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing!
            }
        });

        // Restore previously selected voice (e.g. after screen orientation change).
        voicesSpinner.setSelection(selectedPosition);
    }
}
