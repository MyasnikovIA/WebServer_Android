package ru.miacomsoft.shareservermessage.Terminal;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import ru.miacomsoft.shareservermessage.Lib.webserver.HttpSrv;


public class terminal {


    public static HashMap<String, HttpSrv.HttpResponse> DevList = new HashMap<String, HttpSrv.HttpResponse>(10, (float) 0.5);
    public static HashMap<String, String> MESSAGE_LIST = new HashMap<String, String>(10, (float) 0.5);
    public static HashMap<String, ArrayList<String>> BROADCAST_MESSAGE_LIST = new HashMap<String, ArrayList<String>>(10, (float) 0.5);
    private static String ShellUserName = "";
    private static String ShellUserPass = "";

    public static void onTerminal(HttpSrv.HttpResponse Head) throws IOException, JSONException {

        String DeviceName = "";
        String UserName = "";
        String UserPass = "";
        if (Head.countQuery == 1) {
            DeviceName = Head.firstMessage;
            if (DeviceName.indexOf(":") != -1) {
                String[] userTmp = DeviceName.split(":");
                DeviceName = userTmp[0];
                Head.UserName = userTmp[1];
                if (userTmp.length > 2) {
                    Head.UserPass = userTmp[2];
                }
            }
            Head.firstMessage = DeviceName;
            DevList.put(DeviceName, Head);
            ArrayList<String> listDevice = new ArrayList<String>();
            BROADCAST_MESSAGE_LIST.put(DeviceName, listDevice); // Регистрируем канал, для широковещательной передачи сообщений (1 отправитель -> много получателей = видеострим)
            Head.write("{\"register\":\"" + DeviceName + "\"}");
            Head.write("\r\n");
            Head.write(0);
            return;
        } else {
            DeviceName = Head.firstMessage;
            UserName = Head.UserName;
            UserPass = Head.UserPass;
        }

        // Log.d(LOG_TAG, "===========================");
        // Log.d(LOG_TAG, "Head.countQuery:" + Head.countQuery);
        // Log.d(LOG_TAG, "Head.firstMessage:" + Head.firstMessage);
        // Log.d(LOG_TAG, "Head.DeviceNameSendTo:" + Head.DeviceNameSendTo);
        // Log.d(LOG_TAG, Head.message);
        // Log.d(LOG_TAG, "---------------------------");
        // Log.d(LOG_TAG, "UserName:" + UserName);
        // Log.d(LOG_TAG, "UserPass:" + UserPass);
        // Log.d(LOG_TAG, "===========================");

        String cmd = Head.message.replace("\r", "").replace("\n", "");
        if ((cmd.indexOf("exit") != -1) && (cmd.length() == 4)) {
            rebootOneDevice(DeviceName);
            return;
        }
        if ((cmd.indexOf("ping") != -1) && (cmd.length() == 4)) {
            Head.write("ping\r\n");
            Head.write(0);
            return;
        }

        // получить список подключенных устройств
        if ((cmd.indexOf("list") != -1) && (cmd.length() == 4)) {
            Set<String> keys = DevList.keySet();
            StringBuffer sbList = new StringBuffer();
            sbList.append("\r\n");
            sbList.append("[");
            int ind = 0;
            for (String key : keys) {
                try {
                    DevList.get(key).flush();
                } catch (Exception e) {
                    DevList.remove(key);
                    continue;
                }
                ind++;
                if (ind > 1) {
                    sbList.append(",\"" + key + "\"");
                } else {
                    sbList.append("\"" + key + "\"");
                }
            }
            sbList.append("]\r\n");
            Head.write(sbList.toString());
            Head.write(0);
            Head.flush();
            sbList.setLength(0);
            return;
        }

        // Переключение в режим трансляции видеострима
        if ((cmd.indexOf("udp") != -1) && (cmd.length() == 3)) {
            boolean isWor = true;
            int charInt;
            while (isWor) {
                while ((charInt = Head.is.read()) != -1) {
                    if (charInt == -1) {
                        isWor = false;
                        rebootOneDevice(DeviceName);
                        break;
                    }
                    for (int ind = 0; ind < BROADCAST_MESSAGE_LIST.get(DeviceName).size(); ind++) {
                        try {
                            DevList.get(BROADCAST_MESSAGE_LIST.get(DeviceName).get(ind)).write(charInt);
                        } catch (Exception e) {
                            DevList.remove(BROADCAST_MESSAGE_LIST.get(DeviceName).get(ind));
                            BROADCAST_MESSAGE_LIST.get(DeviceName).remove(ind);
                        }
                    }
                }
            }
            return;
        }

        // получение сообщения для устройства
        if ((cmd.indexOf("pop") != -1) && (cmd.trim().length() == 3)) {
            if (MESSAGE_LIST.containsKey(DeviceName) == true) {
                Head.write(MESSAGE_LIST.get(DeviceName).toString() + "\r\n");
                MESSAGE_LIST.remove(DeviceName);
            } else {
                Head.write("{\"ok\":false,\"error\":\"no message\"}\r\n");
            }
            Head.write(0);
            Head.flush();
            return;
        }

        if ((UserName.length() > 0) && (UserPass.length() > 0) && UserName.equals(ShellUserName) && UserPass.equals(ShellUserPass)) {
            if ((Head.message.trim().split(" ")[0].equals("shell")) || (Head.lastCommandName.equals("shell"))) {
                Head.lastCommandName = "shell";
                String cmdShell = Head.message;
                if (Head.message.indexOf("shell") != -1) {
                    cmdShell = Head.message.split("shell")[1].trim();
                }
                if (cmdShell.length() == 0) return;
                shellExecute(cmdShell, Head);
                return;
            }
        }

        JSONObject jsonObject = null;
        if ((cmd.indexOf("{") != -1) && (cmd.indexOf("}") != -1)) {
            try {
                jsonObject = new JSONObject(cmd);
            } catch (JSONException e) {
                jsonObject = new JSONObject();
                jsonObject.put("message", cmd);
            }
        } else {
            jsonObject = new JSONObject();
            int nimLine = 0;
            for (String TitleLine : Head.message.split("\r")) {
                String value = TitleLine;
                String tmpstr = TitleLine.trim();
                if (tmpstr.indexOf(":") != -1) {
                    TitleLine = tmpstr.substring(0, TitleLine.lastIndexOf(":")).trim();
                    value = tmpstr.substring(tmpstr.lastIndexOf(":") + 1);
                    Head.lastAttribetName = TitleLine;
                    if (Head.lastAttribetName.indexOf(":") != -1) {
                        Head.lastAttribetName = Head.lastAttribetName.split(":")[0];
                    }
                }
                if (jsonObject.has(Head.lastAttribetName)) {
                    jsonObject.put(Head.lastAttribetName, jsonObject.getString(Head.lastAttribetName) + "\r" + value);
                } else {
                    jsonObject.put(Head.lastAttribetName, value);
                }
            }
        }
        //  Log.d(LOG_TAG, "==========" + jsonObject.toString() + "=================");
        if (jsonObject.toString().equals("{}")) return;


        if ((jsonObject.has("stream") == true)) {
            Head.lastCommandName = "stream";
            jsonObject.put("input_stream", DeviceName);
            Head.DeviceNameSendTo = jsonObject.getString("stream").trim().replace("\r", "").replace("\n", "");
            jsonObject.remove("stream");
            if (DevList.containsKey(Head.DeviceNameSendTo)) {
                HttpSrv.HttpResponse devTo = (HttpSrv.HttpResponse) DevList.get(Head.DeviceNameSendTo);
                devTo.write(jsonObject.toString() + "\r\r\r");
                int subcharIntIn = 0;
                while ((subcharIntIn = Head.is.read()) != -1) {
                    if (subcharIntIn == -1) break;
                    try {
                        devTo.write(subcharIntIn);
                        devTo.flush();
                    } catch (Exception e) {
                        Head.write("{\"error\":\"client disconnect\"}");
                        if (DevList.containsKey(Head.DeviceNameSendTo)) {
                            DevList.remove(Head.DeviceNameSendTo);
                            BROADCAST_MESSAGE_LIST.remove(Head.DeviceNameSendTo);
                        }
                        break;
                    }
                }
                return;
            } else {
                Head.write("{\"ok\":false,\"error\":\"device '" + Head.DeviceNameSendTo + "' not found\"}\r\n");
            }
            return;
        }

        // регистрация нового устройства для слушателя видеопотока
        if ((jsonObject.has("udp") == true) || (Head.lastCommandName.equals("udp"))) {
            Head.lastCommandName = "udp";
            if (jsonObject.has("udp") == true) {
                Head.DeviceNameSendTo = jsonObject.getString("udp").trim().replace("\n", "").replace("\r", "");
                jsonObject.remove("udp");
            }
            jsonObject.put("from", DeviceName);
            if (!BROADCAST_MESSAGE_LIST.containsKey(Head.DeviceNameSendTo)) {
                Head.write("{\"ok\":false,\"error\":\"device '" + Head.DeviceNameSendTo + "' not found\"}\r\n");
                Head.write(0);
                Head.flush();
                return;
            }
            ArrayList<String> listDevice = BROADCAST_MESSAGE_LIST.get(Head.DeviceNameSendTo);
            if (!listDevice.contains(DeviceName)) {
                listDevice.add(DeviceName);
                BROADCAST_MESSAGE_LIST.put(Head.DeviceNameSendTo, listDevice);
            }
            Head.write("{\"ok\":true}\r\n");
            Head.write(0);
            Head.flush();
            return;
        }

        // отправка сообщения для устройства
        if ((jsonObject.has("push") == true) || (Head.lastCommandName.equals("push"))) {
            Head.lastCommandName = "push";
            if (jsonObject.has("push") == true) {
                Head.DeviceNameSendTo = jsonObject.getString("push");
                jsonObject.remove("push");
            }
            jsonObject.put("from", DeviceName);
            if (MESSAGE_LIST.containsKey(Head.DeviceNameSendTo)) {
                JSONArray arr = new JSONArray((String) MESSAGE_LIST.get(Head.DeviceNameSendTo));
                arr.put(jsonObject);
                MESSAGE_LIST.put(Head.DeviceNameSendTo, arr.toString());
            } else {
                MESSAGE_LIST.put(Head.DeviceNameSendTo, "[" + jsonObject.toString() + "]");
            }
            Head.write("{\"ok\":true}\r\n");
            Head.write(0);
            Head.flush();
            return;
        }

        // прямая отправка сообщения для устройства, если оно в сети
        if ((jsonObject.has("send") == true) || (Head.lastCommandName.equals("send"))) {
            Head.lastCommandName = "send";
            if (jsonObject.has("send") == true) {
                Head.DeviceNameSendTo = jsonObject.getString("send");
            }
            jsonObject.remove("send");
            jsonObject.put("from", DeviceName);
            if (DevList.containsKey(Head.DeviceNameSendTo)) {
                try {
                    HttpSrv.HttpResponse devTo = (HttpSrv.HttpResponse) DevList.get(Head.DeviceNameSendTo);
                    devTo.os.write((jsonObject.toString()).getBytes());
                    devTo.os.write(0);
                    devTo.os.flush();
                    Head.write("{\"ok\":true}");
                } catch (IOException e) {
                    Head.write("{\"ok\":false,\"error\":\"send '" + Head.DeviceNameSendTo + "' error\"}\r\n");
                }
            } else {
                Head.write("{\"ok\":false,\"error\":\"send '" + Head.DeviceNameSendTo + "' error\"}\r\n");
            }
            Head.write(0);
            Head.flush();
            return;
        }

        // Если получатель определен, тогда все остальные сообщения отпр
        if (Head.DeviceNameSendTo.length() > 0) {
            jsonObject.put("from", DeviceName);
            if (DevList.containsKey(Head.DeviceNameSendTo)) {
                HttpSrv.HttpResponse devTo = (HttpSrv.HttpResponse) DevList.get(Head.DeviceNameSendTo);
                devTo.write(jsonObject.toString().getBytes());
                devTo.write(0);
                devTo.flush();
            } else {
                Head.write("{\"ok\":false,\"error\":\"device '" + Head.DeviceNameSendTo + "' not found\"}\r\n");
            }
            Head.write(0);
            return;
        }
    }

    public static void rebootOneDevice(String DevName) {
        if (DevName.length() == 0) return;
        Set<String> keys = DevList.keySet();
        if (keys != null) {
            for (String key : keys) {
                if (DevName.length() > 0) {
                    if (key.equals(DevName)) {
                        HttpSrv.HttpResponse dev = (HttpSrv.HttpResponse) DevList.get(key);
                        OutputStream osDst = dev.os;
                        Socket soc = dev.socket;
                        if (soc.isConnected()) {
                            try {
                                osDst.write(" Kill connect \r\n".getBytes());
                                osDst.write(0);
                                soc.shutdownInput();
                                soc.shutdownOutput();
                                soc.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        DevList.remove(key);
                        BROADCAST_MESSAGE_LIST.remove(key);
                    }
                }
            }
        }
    }

    public static void shellExecute(String cmd, HttpSrv.HttpResponse Head) {
        Process proc;
        try {
            proc = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                // Log.d(LOG_TAG, "cmdShell: " + line);
                if (Head != null) Head.write(line + "\r\n");
                proc.waitFor();
            }
            if (Head != null) Head.write(0);
        } catch (Exception e) {
            if (Head != null) {
                try {
                    Head.write(e.getMessage() + "\r\n");
                    Head.write(0);
                    //Log.d(LOG_TAG, "shellError: " + e.getMessage());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            // e.printStackTrace();
        }
    }

    // public void rebootDevice() {
    //     // https://answacode.com/questions/24693682/programmno-vyklyuchit-ustrojstvo
    //     Intent i = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
    //     i.putExtra("android.intent.extra.KEY_CONFIRM", false);
    //     i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //     startActivity(i);
    // }

}
