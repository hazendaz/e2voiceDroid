
package com.example.zhoupeng.e2voicedroid;

public class TextMessage {
    private String message;
    private String from;
    private String timeStamp;

    private ResponseCard responseCard;

    public TextMessage(final String message, final String from, final String timeStamp) {
        this.message = message;
        this.from = from;
        this.timeStamp = timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public ResponseCard getResponseCard() {
        return responseCard;
    }

    public void setResponseCard(ResponseCard responseCard) {
        this.responseCard = responseCard;
    }
}