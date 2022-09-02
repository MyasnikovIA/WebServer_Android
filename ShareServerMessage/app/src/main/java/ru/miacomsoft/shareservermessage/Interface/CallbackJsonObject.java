package ru.miacomsoft.shareservermessage.Interface;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public interface CallbackJsonObject {
    public void call(JSONObject response) throws JSONException, IOException;
}
