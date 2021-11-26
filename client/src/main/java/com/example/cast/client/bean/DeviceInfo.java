package com.example.cast.client.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 保存连接目标设备ip信息
 */
public class DeviceInfo {

    public String ip;
    public int port;

    public DeviceInfo() {
    }

    public DeviceInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static DeviceInfo fromJson(String content) {
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

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
