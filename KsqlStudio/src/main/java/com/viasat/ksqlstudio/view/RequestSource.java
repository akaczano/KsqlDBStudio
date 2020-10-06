package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.model.query.StreamProperties;

public interface RequestSource {
    void onError(String message);
    void onComplete();
    StreamProperties getProperties();
}
