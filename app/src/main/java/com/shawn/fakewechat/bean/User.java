package com.shawn.fakewechat.bean;

import com.koushikdutta.async.http.WebSocket;

public class User {
    private String uuid;
    private int clientType;
    private WebSocket webSocket;
    private String privateKey;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getClientType() {
        return clientType;
    }

    public void setClientType(int clientType) {
        this.clientType = clientType;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
