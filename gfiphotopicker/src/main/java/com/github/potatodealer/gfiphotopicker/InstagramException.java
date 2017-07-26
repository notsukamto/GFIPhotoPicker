package com.github.potatodealer.gfiphotopicker;


public class InstagramException extends Exception {

    public static final int CODE_GENERIC_NETWORK_EXCEPTION = 0;
    public static final int CODE_INVALID_ACCESS_TOKEN = 1;

    private final int code;

    public InstagramException(int code, String detailsMessage) {
        super(detailsMessage);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
