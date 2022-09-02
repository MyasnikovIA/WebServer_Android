package ru.miacomsoft.shareservermessage.Www;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import ru.miacomsoft.shareservermessage.Lib.webserver.HttpSrv;
import ru.miacomsoft.shareservermessage.Terminal.terminal;


public class SignalChange {

    public static void onPage(HttpSrv.HttpResponse Head) throws IOException, JSONException {

        JSONObject jsonObject = new JSONObject();
        JSONObject jsonRes = new JSONObject();
        if (Head.POST != null) {
            try {
                jsonObject = new JSONObject(new String(Head.POST));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (jsonObject.has("method") == false) {
            jsonObject.put("method", "push");
        }

        // Отправка сообщения
        if (jsonObject.has("pop") == true) {
            String DeviceNameFrom = jsonObject.getString("pop");
            if (terminal.MESSAGE_LIST.containsKey(DeviceNameFrom) == true) {
                Head.sendJson(terminal.MESSAGE_LIST.get(DeviceNameFrom).toString());
                terminal.MESSAGE_LIST.remove(DeviceNameFrom);
            } else {
                Head.sendJson("{\"ok\":false,\"error\":\"no message for "+DeviceNameFrom+"\"}");
            }
            return;
        }

        if (jsonObject.has("from") == false) {
            jsonObject.put("from", "anonimus");
        }

        // прямая отправка сообщения для устройства, если оно в сети
        if ((jsonObject.has("send") == true) || (Head.lastCommandName.equals("send"))) {
            Head.DeviceNameSendTo = jsonObject.getString("send");
            jsonObject.remove("send");
            if (terminal.DevList.containsKey(Head.DeviceNameSendTo)) {
                try {
                    HttpSrv.HttpResponse devTo = (HttpSrv.HttpResponse) terminal.DevList.get(Head.DeviceNameSendTo);
                    devTo.os.write((jsonObject.toString()).getBytes());
                    devTo.os.write(0);
                    devTo.os.flush();
                    jsonRes.put("ok", true);
                } catch (IOException e) {
                    jsonRes.put("ok", false);
                    jsonRes.put("error", "send '" + Head.DeviceNameSendTo + "' error");
                }
            } else {
                jsonRes.put("ok", false);
                jsonRes.put("error", "send '" + Head.DeviceNameSendTo + "' error");
            }
            Head.sendJson(jsonRes);
            return;
        }

        Head.sendJson(jsonObject);

        /*
        JSONObject json = new JSONObject();
        if (Head.POST != null) {
            try {
                json = new JSONObject(new String(Head.POST));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (json.has("send")){

        }
        Head.sendJson(json);
         */
    }


}
