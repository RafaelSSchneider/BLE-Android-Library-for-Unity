package com.rafael.playblowplugin;

import org.json.JSONObject;
public class UnityObject {
    public String command;
    public String device;
    public String deviceName;
    public String service;
    public String characteristic;
    public boolean hasError;
    public String errorMessage;
    public String data;
    public String dataType;

    public UnityObject(String command){
        this.command = command;
    }

    public void setError(String errorMessage) {
        hasError = true;
        this.errorMessage = errorMessage;
    }
    public String toJson(){
        JSONObject obj = new JSONObject();
        try{
            obj.put("command", command);
            obj.put("device", device);
            obj.put("deviceName", deviceName);
            obj.put("service", service);
            obj.put("characteristic", characteristic);
            obj.put("data", data);
            obj.put("dataType", dataType);
            if(hasError) {
                obj.put("hasError", hasError);
                obj.put("errorMessage", errorMessage);
            }
            return obj.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj.toString();
    }
}
