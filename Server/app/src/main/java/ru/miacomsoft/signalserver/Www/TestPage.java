package ru.miacomsoft.signalserver.Www;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import ru.miacomsoft.signalserver.Lib.webserver.HttpSrv;


public class TestPage {

    public static void onPage(HttpSrv.HttpResponse Head) throws IOException {
        String timeStamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
        StringBuffer sb = new StringBuffer();
        sb.append("<body style=\"background-color: RGB(100, 100, 100);color: RGB(100, 200, 100);\">");
        sb.append("<h1><center>TEST<br/>");
        sb.append(timeStamp);
        sb.append("</center></h1>");
        sb.append("<script>");
        sb.append( "</script>");
        sb.append("</body>");
        Head.sendHtml(sb.toString());
    }



}
