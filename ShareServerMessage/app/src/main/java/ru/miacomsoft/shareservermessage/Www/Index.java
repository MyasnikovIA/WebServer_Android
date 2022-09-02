package ru.miacomsoft.shareservermessage.Www;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import ru.miacomsoft.shareservermessage.Lib.webserver.HttpSrv;


public class Index {

    public static void onPage(HttpSrv.HttpResponse Head) throws IOException {
        String timeStamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
        StringBuffer sb = new StringBuffer();
        sb.append("<body style=\"background-color: RGB(100, 100, 100);color: RGB(100, 200, 100);\">");
        // sb.append("<h1><center>Ресурс не найден <br/>");
        sb.append(timeStamp);
        sb.append("</center></h1>");
        sb.append("<script>");
        sb.append("let timerId = setTimeout(function tick(){");
        sb.append("   location.reload();");
        sb.append("   timerId = setTimeout(tick, 1000);");
        sb.append("}, 1000); ");
        sb.append("</script>");
        sb.append("</body>");
        Head.sendHtml(sb.toString());
    }


}
