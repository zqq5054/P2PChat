package com.shawn.fakewechat.app;


import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.socketio.StringCallback;
import com.shawn.fakewechat.bean.User;
import com.shawn.fakewechat.utils.HelpUtils;
import com.shawn.fakewechat.utils.HttpUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class App extends Application implements Runnable{

    AsyncHttpServer httpServer = new AsyncHttpServer();
    private String ipAddress = "192.168.1.153";
    private int port = 9998;
    private String host = ipAddress+":"+9998;
    private String httpHost = "http://" + host + "/";
    private String wsHost = "ws://" + host + "/live";
    private Map<String, User> userMap = new HashMap<>();
    private String pubKey;
    private String priKey;
    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(this).start();

    }

    private void initHttpServer() {

        httpServer.get("/file", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                String fileName = request.getQuery().getString("name");

                if (fileName.equals("client.html")) {
                    try {
                        String content = readFileFromAssets(App.this, "client.html");
                        response.setContentType("text/html");
                        response.send(content);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else if (fileName.equals("zepto.min.js")) {
                    try {
                        String content = readFileFromAssets(App.this, "zepto.min.js");
                        response.setContentType("text/javascript");
                        response.send(content);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else if (fileName.equals("client.js")) {
                    try {
                        String content = readFileFromAssets(App.this, "client.js");
                        response.setContentType("text/javascript");
                        response.send(content);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else if (fileName.equals("client.css")) {
                    try {
                        String content = readFileFromAssets(App.this, "client.css");
                        response.setContentType("text/css");
                        response.send(content);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });

    }

    private void initWebSocket() {

        httpServer.websocket("/live", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, final AsyncHttpServerRequest request) {
//                socketList.add(webSocket);

                //Use this to clean up any references to your websocket
                webSocket.send("pub-"+pubKey);
                final String uuid = request.getQuery().get("uuid").get(0);
                User user = new User();
                user.setUuid(uuid);
                user.setWebSocket(webSocket);
                userMap.put(uuid,user);
                Intent intent = new Intent(getPackageName()+".online");
                intent.putExtra("uuid",uuid);
                sendBroadcast(intent);
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                Log.e("WebSocket", "An error occurred", ex);
                        } finally {
//                            socketList.remove(webSocket);
                            userMap.remove(uuid);
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onStringAvailable(String s) {
                        s = HelpUtils.decrypt(s,priKey);
                        Intent intent = new Intent(getPackageName()+".chat");
                        intent.putExtra("uuid",uuid);
                        intent.putExtra("data",s);
                        sendBroadcast(intent);
                    }
                });

            }
        });

    }

    public String readFileFromAssets(Context context, String fileName) throws IOException {

        if (null == context || null == fileName) return null;
        AssetManager am = context.getAssets();
        InputStream input = am.open(fileName);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = input.read(buffer)) != -1) {
            output.write(buffer, 0, len);
        }
        output.close();
        input.close();
        return output.toString();
    }
    public String createNewClient(String uuid){
        if(TextUtils.isEmpty(uuid)) {
            uuid = java.util.UUID.randomUUID().toString().replaceAll("-", "");
        }
        String url = httpHost+"file?name=client.html&uuid="+uuid;
        return url;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendMessage(String uuid, String message){
        WebSocket webSocket = userMap.get(uuid).getWebSocket();
//        message = HelpUtils.encrypt(message,priKey);
//        System.out.println("encode message ==== "+message);
//        System.out.println("decode ==== "+HelpUtils.decrypt(message,priKey));
        webSocket.send(message);
    }
    public Map<String,User> getUserMap(){


        return userMap;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {

        httpServer.listen(AsyncServer.getDefault(), 9998);
        SharedPreferences data = getSharedPreferences("data",0);
        pubKey = data.getString("pubKey","");
        if(TextUtils.isEmpty(pubKey)){

            try {
                Map<Integer,String> keyMap = HelpUtils.genKeyPair();
                data.edit()
                        .putString("pubKey",keyMap.get(0))
                        .putString("priKey",keyMap.get(1))
                        .commit();
                pubKey = keyMap.get(0);
                priKey = keyMap.get(1);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        }else{
            pubKey = data.getString("pubKey","");
            priKey = data.getString("priKey","");
        }


        String ipV6Str = HttpUtil.getRequset("https://ipv6.lookup.test-ipv6.com/ip/?asn=1&testdomain=test-ipv6.com&testname=test_asn6",new HashMap<>());

        try {
            JSONObject jb = new JSONObject(ipV6Str);
            ipAddress = jb.getString("ip");
            host = "["+ipAddress+"]:"+9998;
            httpHost = "http://" + host + "/";
            wsHost = "ws://" + host + "/live";
        }catch (Exception e){
            e.printStackTrace();
        }
        initWebSocket();
        initHttpServer();
//        createHole();
        System.out.println("createHole====>");
    }
    public void createHole(){

        AsyncHttpClient.getDefaultInstance().websocket("ws://[240e:464:1b24:3ce7::1]:9998", "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                webSocket.send("a string");
                webSocket.send(new byte[10]);
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        System.out.println("I got a string: " + s);
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                        System.out.println("I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
            }
        });

    }
}