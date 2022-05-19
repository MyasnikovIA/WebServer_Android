package ru.miacomsoft.signalserver.Www;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import ru.miacomsoft.signalserver.Lib.webserver.HttpSrv;
import ru.miacomsoft.signalserver.Terminal.terminal;

public class SignalChange {

    public static void onPage(HttpSrv.HttpResponse Head) throws IOException, JSONException {

        //  http://128.0.24.172:8200/signalchange.ru?send=vr&pos=1&from=test
        //  http://128.0.24.172:8200/signalchange.ru?pop=1&from=test
        String sendDeviseName = "";
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonRes = new JSONObject();
        if (Head.POST != null) {
            try {
                jsonObject = new JSONObject(new String(Head.POST));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            jsonObject = Head.requestParam;
        }

        if (jsonObject.has("from") == false) {
            jsonObject.put("from","anonimus");
            sendDeviseName = "anonimus";
        } else{
            sendDeviseName = jsonObject.getString("from");
        }

        if (jsonObject.has("method") == false) {
            jsonObject.put("method","push");
        }


        if ((jsonObject.has("send") == true) || (Head.lastCommandName.equals("send"))) {
            Head.DeviceNameSendTo = jsonObject.getString("send");
            jsonObject.remove("send");
            if (terminal.DevList.containsKey(Head.DeviceNameSendTo)) {
                try {
                    HttpSrv.HttpResponse devTo = (HttpSrv.HttpResponse) terminal.DevList.get(Head.DeviceNameSendTo);
                    devTo.os.write((jsonObject.toString()).getBytes());
                    devTo.os.write(0);
                    devTo.os.flush();
                    jsonRes.put("ok",true);
                    Head.sendJson(jsonRes);
                } catch (IOException e) {
                    jsonRes.put("ok",false);
                    jsonRes.put("error","send '" + Head.DeviceNameSendTo + "' error");
                    Head.sendJson(jsonRes);
                }
            } else {
                jsonRes.put("ok",false);
                jsonRes.put("error","send '" + Head.DeviceNameSendTo + "' error");
                Head.sendJson(jsonRes);
            }
            return;
        }

        if (jsonObject.has("pop") == true){
            if (terminal.MESSAGE_LIST.containsKey(sendDeviseName) == true) {
                Head.sendJson(terminal.MESSAGE_LIST.get(sendDeviseName));
                terminal.MESSAGE_LIST.remove(sendDeviseName);
            } else {
                Head.sendJson("[]");
            }
            return;
        }
        Head.sendJson(jsonObject);
    }


}
