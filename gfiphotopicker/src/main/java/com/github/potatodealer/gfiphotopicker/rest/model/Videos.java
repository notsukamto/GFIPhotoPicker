package com.github.potatodealer.gfiphotopicker.rest.model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Videos {

    @SerializedName("low_resolution")
    @Expose
    private Image lowResolution;
    @SerializedName("standard_resolution")
    @Expose
    private Image standardResolution;
    @SerializedName("comments")
    @Expose
    private Comments comments;
    @SerializedName("caption")
    @Expose
    private Object caption;
    @SerializedName("likes")
    @Expose
    private Likes likes;
    @SerializedName("link")
    @Expose
    private String link;
    @SerializedName("created_time")
    @Expose
    private String createdTime;
    @SerializedName("images")
    @Expose
    private Images images;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("users_in_photo")
    @Expose
    private Object usersInPhoto;
    @SerializedName("filter")
    @Expose
    private String filter;
    @SerializedName("tags")
    @Expose
    private List<Object> tags = null;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("user")
    @Expose
    private User user;
    @SerializedName("location")
    @Expose
    private Object location;

}
