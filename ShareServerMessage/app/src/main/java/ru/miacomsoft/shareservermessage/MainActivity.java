package ru.miacomsoft.shareservermessage;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.Formatter;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import ru.miacomsoft.shareservermessage.Lib.webserver.HttpSrv;
import ru.miacomsoft.shareservermessage.Terminal.terminal;
import ru.miacomsoft.shareservermessage.Www.Index;
import ru.miacomsoft.shareservermessage.Www.SignalChange;
import ru.miacomsoft.shareservermessage.Www.TestPage;

public class MainActivity extends AppCompatActivity {



    public static String LOG_TAG = "MainActivity";

    private String IP_SERVER = "128.0.24.172";
    private String PORT_SERVER = "8200";

    protected PowerManager.WakeLock mWakeLock;
    private WebView webView;
    private HttpSrv web;
    StringBuffer sb = new StringBuffer("<html>\n" +
            "    <head>\n" +
            "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
            "    </head>\n" +
            "    <body style=\"background-color: RGB(0, 0, 0);color: RGB(100, 100, 100);\">\n" +
            "        <h1>" +
            "            <center>" +
            "              <br/> IP: {%IP%}:{%PORT%}" +
            "              <br/> SSID WIFI:  {%SSID%}" +
            "              <br/> MAC DEVICE: {%MacAddress%}" +
            "            </center>" +
            "            <iframe id=\"testFrm\" src=\"http://" + IP_SERVER + ":" + PORT_SERVER + "/TestPage\" width=\"100%\" height=\"200\"></iframe>" +
            "            <center><button onclick=\"document.getElementById('testFrm').src='http://" + IP_SERVER + ":" + PORT_SERVER + "/TestPage'; \">Reload</button></center>" +
            "        </h1>" +
            "    </body>\n" +
            "</html>");

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        PowerManager pm2 = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm2.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SignalServer::MainActivity");
        mWakeLock.acquire();

        String textHtml = "";
        boolean onConnect = false;
        String ipAddress = "";
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
        wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXX"); // Пароль для полдключения к точки доступа
        while (onConnect == false) {
            WifiInfo info = wifiManager.getConnectionInfo();
            int ip = info.getIpAddress();
            if (ip != 0) {
                ipAddress = Formatter.formatIpAddress(ip);
                textHtml = sb.toString().replace("{%IP%}", ipAddress)
                        .replace("{%SSID%}", info.getSSID())
                        .replace("{%MacAddress%}", info.getMacAddress())
                        .replace("{%PORT%}", PORT_SERVER);
                onConnect = true;
            }
        }
        webView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.loadData(textHtml, "text/html; charset=UTF-8", "UTF-8");
        //webView.loadUrl("https://www.babylonjs-playground.com/#U8MEB0");

        Toast.makeText(getApplicationContext(), "Start Signal Server", Toast.LENGTH_SHORT).show();

        web = new HttpSrv(Integer.valueOf(PORT_SERVER), this);
        web.onTerminal(terminal::onTerminal);
        web.onPage("testpage", TestPage::onPage);
        web.onPage("index.html", Index::onPage);
        web.onPage("signalchange.ru", SignalChange::onPage);
        web.onPage(Index::onPage);
        web.start();
    }
}