package com.example.cast.service.bean;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceInfo {

    public String ip;
    public int port;

    public static DeviceInfo fromJson(String content){
        DeviceInfo info = new DeviceInfo();
        try {
            JSONObject jsonObject = new JSONObject(content);
            info.ip = jsonObject.optString("ip");
            info.port = jsonObject.optInt("port");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return info;
    }

    public static String toJson(String ip, int port){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ip", ip);
            jsonObject.put("port",port);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

}
