package com.viasat.ksqlstudio.model.query;

import com.google.gson.annotations.SerializedName;

public class StreamProperties {
    @SerializedName("auto.offset.reset")
    private String offsetRest;

    public String getOffsetRest() {
        return offsetRest;
    }

    public void setOffsetRest(String offsetRest) {
        this.offsetRest = offsetRest;
    }
}
