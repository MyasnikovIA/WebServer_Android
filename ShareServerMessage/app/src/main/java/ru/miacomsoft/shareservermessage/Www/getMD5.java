package ru.miacomsoft.shareservermessage.Www;

import static ru.miacomsoft.shareservermessage.Terminal.terminal.LIST_MD5;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ru.miacomsoft.shareservermessage.Lib.webserver.HttpSrv;
import ru.miacomsoft.shareservermessage.Terminal.terminal;

public class getMD5 {
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

        if (Head.requestParam.has("getmd5") == true) {
            String md5Text = getMD5(Head.requestParam.getString("getmd5"));
            LIST_MD5.put(md5Text, Head.requestParam.getString("getmd5"));
            Head.sendJson("{\"md5\":\""+md5Text+"\"}");
            return;
        }
        if (Head.requestParam.has("x-getmd5") == true) {
            String md5Text = getMD5(Head.requestParam.getString("x-getmd5"));
            LIST_MD5.put(md5Text, Head.requestParam.getString("x-getmd5"));
            Head.sendJson("{\"md5\":\""+md5Text+"\"}");
            return;
        }
        Head.sendText(Head, "OK");
        return;
    }
    public static String getMD5(String plaintext) {
        String hashtext = "";
        try {
            MessageDigest m = null;
            m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plaintext.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            hashtext = bigInt.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashtext;
    }
}
