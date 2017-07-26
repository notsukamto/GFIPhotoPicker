package com.github.potatodealer.gfiphotopicker.rest.model;


import com.google.gson.annotations.SerializedName;

public class Location {

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("longitude")
    private Double longitude;
    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("street_address")
    private String street_address;

    public String getId ()
    {
        return id;
    }

    public void setId (String id)
    {
        this.id = id;
    }

    public String getName ()
    {
        return name;
    }

    public void setName (String name)
    {
        this.name = name;
    }

    public Double getLongitude ()
    {
        return longitude;
    }

    public void setLongitude (Double longitude)
    {
        this.longitude = longitude;
    }

    public Double getLatitude ()
    {
        return latitude;
    }

    public void setLatitude (Double latitude)
    {
        this.latitude = latitude;
    }

    public String getStreet_address ()
    {
        return street_address;
    }

    public void setStreet_address (String street_address)
    {
        this.street_address = street_address;
    }

}
