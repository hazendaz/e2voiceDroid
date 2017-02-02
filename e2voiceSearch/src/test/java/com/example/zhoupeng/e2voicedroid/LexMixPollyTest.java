package com.example.zhoupeng.e2voicedroid;

import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;

public class LexMixPollyTest {

    LexMixPolly polly;

    @Before
    public void init() {
        polly = new LexMixPolly();
    }

    @Test
    public void onCreate() {
        polly.initializeLexSDK();
    }

}
