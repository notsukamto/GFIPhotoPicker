package com.github.potatodealer.gfiphotopicker.rest.model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Images {

    @SerializedName("thumbnail")
    @Expose
    private Image thumbnail;
    @SerializedName("low_resolution")
    @Expose
    private Image low_resolution;
    @SerializedName("standard_reslution")
    @Expose
    private Image standard_resolution;

    public Image getThumbnail ()
    {
        return thumbnail;
    }

    public void setThumbnail (Image thumbnail)
    {
        this.thumbnail = thumbnail;
    }

    public Image getLow_resolution ()
    {
        return low_resolution;
    }

    public void setLow_resolution (Image low_resolution)
    {
        this.low_resolution = low_resolution;
    }

    public Image getStandard_resolution ()
    {
        return standard_resolution;
    }

    public void setStandard_resolution (Image standard_resolution)
    {
        this.standard_resolution = standard_resolution;
    }

}
