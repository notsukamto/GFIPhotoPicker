package com.github.potatodealer.gfiphotopicker.rest.model;


import com.google.gson.annotations.SerializedName;

public class Meta {

    @SerializedName("error_type")
    private String error_type;
    @SerializedName("error_message")
    private String error_message;
    @SerializedName("code")
    private String code;

    public String getError_type ()
    {
        return error_type;
    }

    public void setError_type (String error_type)
    {
        this.error_type = error_type;
    }

    public String getError_message ()
    {
        return error_message;
    }

    public void setError_message (String error_message)
    {
        this.error_message = error_message;
    }

    public String getCode ()
    {
        return code;
    }

    public void setCode (String code)
    {
        this.code = code;
    }

}
