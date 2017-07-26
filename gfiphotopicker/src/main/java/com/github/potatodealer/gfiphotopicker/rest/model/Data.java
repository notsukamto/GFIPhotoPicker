package com.github.potatodealer.gfiphotopicker.rest.model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Data {

    @SerializedName("comments")
    @Expose
    private Comments comments;
    @SerializedName("caption")
    @Expose
    private Caption caption;
    @SerializedName("likes")
    @Expose
    private Likes likes;
    @SerializedName("link")
    @Expose
    private String link;
    @SerializedName("user")
    @Expose
    private User user;
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
    private List<Object> usersInPhoto = null;
    @SerializedName("filter")
    @Expose
    private String filter;
    @SerializedName("tags")
    @Expose
    private List<String> tags = null;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("location")
    @Expose
    private Location location;
    @SerializedName("videos")
    @Expose
    private Videos videos;

    public Comments getComments() {
        return comments;
    }

    public void setComments(Comments comments) {
        this.comments = comments;
    }

    public Caption getCaption() {
        return caption;
    }

    public void setCaption(Caption caption) {
        this.caption = caption;
    }

    public Likes getLikes() {
        return likes;
    }

    public void setLikes(Likes likes) {
        this.likes = likes;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Object> getUsersInPhoto() {
        return usersInPhoto;
    }

    public void setUsersInPhoto(List<Object> usersInPhoto) {
        this.usersInPhoto = usersInPhoto;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Videos getVideos() {
        return videos;
    }

    public void setVideos(Videos videos) {
        this.videos = videos;
    }
}
