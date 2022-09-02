package ru.miacomsoft.shareservermessage.Lib.webserver;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author MyasnikovIA (20-01-2022)
 * <p>
 * Состав:
 * HttpSrv.java - Вэб сервер
 * HttpResponse.java - объект с описанием запроса и вспомогательными функциями
 * org.json.jar (05-12-2021) - библиотека для работы с JSON структурами  (JSONObject JSONArray)
 * <p>
 * Применение:
 * <p>
 * HttpSrv srv;
 * srv = new HttpSrv(9090);
 * <p>
 * // Обработка запросов из терминала
 * srv.onTerminal((HttpResponse Head) -> {
 * System.out.println(Head.message);
 * Head.write("Сообщение обработано на сервере:" + Head.message);
 * });
 * <p>
 * // Обработка запросов браузера URL пути
 * // страница не найдена
 * srv.onPage404((HttpResponse Head) -> {
 * Head.Head();
 * Head.Body("<h1><center>Ресурс не найден</center></h1>");
 * Head.End();
 * });
 * <p>
 * // Создаем страницу в коде
 * srv.onPage("index.html", (HttpResponse Head) -> {
 * Head.Head();
 * Head.Body("Текст HTML страницы");
 * Head.Body("fff<h1>Обработка тэгов</h1>ffffffffff");
 * Head.Body("--------------------------");
 * Head.End();
 * });
 * <p>
 * // Отправляем JSON объект в коде
 * srv.onPage("json.html", (HttpResponse Head) -> {
 * Head.sendJson("{'ok':14234234}");
 * });
 * <p>
 * // Отправка файла при указании ресурса
 * srv.onPage("test.html","F:\\javaProject\\HtmlServer013\\www\\index.html");
 * <p>
 * //  Указываем директорию в которой распложен контент
 * srv.onPage("F:\\javaProject\\HtmlServer013\\www\\");
 * <p>
 * // Запуск сервера
 * srv.start();
 */
public class HttpSrv {

    public CallbackPage callbackSocketPage = null;
    public CallbackPage callbackSocketTerminal = null;
    public CallbackPage callbackOnPage404 = null;
    private HashMap<String, PageObj> pagesList = new HashMap<String, PageObj>(10, (float) 0.5);
    private HashMap<String, File> pagesPathList = new HashMap<String, File>(10, (float) 0.5);
    private HashMap<String, Object> sessionList = new HashMap<String, Object>(10, (float) 0.5);
    private String rootPath = null;

    public class PageObj {
        CallbackPage callback;
        String ContentType;
    }

    public interface CallbackPage {
        public void call(HttpResponse Headers) throws JSONException, IOException;
    }

    private Thread mainThready = null;
    private int port = 9091;
    private AppCompatActivity mainActivity;

    public HttpSrv(int port) {
        this.port = port;
        this.mainActivity = null;
    }

    public HttpSrv(int port, AppCompatActivity mainActivity) {
        this.port = port;
        this.mainActivity = mainActivity;
    }


    private boolean process = false;

    /**
     * Запуск сервера
     */
    public void start() {
        process = true;
        if (mainThready != null) {
            Stop();
            mainThready.stop();
        }
        mainThready = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(port, 32767);
                    while (process == true) {
                        Socket socket = ss.accept();
                        new Thread(new SocketProcessor(socket, mainActivity)).start();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        mainThready.start();    //Запуск потока
    }

    /***
     * Остановка сервера
     */
    public void Stop() {
        process = false;
    }

    /***
     * Обработка подключений к серверу через терминал
     * @param callbackSocketTerminal
     */
    public void onTerminal(CallbackPage callbackSocketTerminal) {
        this.callbackSocketTerminal = callbackSocketTerminal;
    }

    /***
     * Описываем события обработки  не найденого ресурса
     * @param callbackOnPage404
     */
    public void onPage404(CallbackPage callbackOnPage404) {
        this.callbackOnPage404 = callbackOnPage404;
    }

    /***
     * Указываем ресурс для обработки любых запросов
     * @param callbackSocketPage- функция обработки вызова ресурса
     */
    public void onPage(CallbackPage callbackSocketPage) {
        this.callbackSocketPage = callbackSocketPage;
    }

    /***
     * Указываем ресурс для URL запроса
     * @param query - текст запроса (ContentType определяется по расширению ресурса )
     * @param callbackSocketPage- функция обработки вызова ресурса
     */
    public void onPage(String query, CallbackPage callbackSocketPage) {
        PageObj pageObj = new PageObj();
        pageObj.callback = callbackSocketPage;
        pageObj.ContentType = ContentType(new File(query.toLowerCase()));
        this.pagesList.put(query, pageObj);
    }

    /***
     * Указываем ресурс для URL запроса
     * @param query - текст запроса
     * @param contentType - тип контента для интерпритации браузером (ContentType)
     * @param callbackSocketPage - функция обработки вызова ресурса
     */
    public void onPage(String query, String contentType, CallbackPage callbackSocketPage) {
        PageObj pageObj = new PageObj();
        pageObj.callback = callbackSocketPage;
        if (contentType.indexOf(".") != -1) {
            pageObj.ContentType = ContentType(new File("page" + contentType));
        } else {
            pageObj.ContentType = contentType;
        }
        this.pagesList.put(query, pageObj);
    }

    /***
     * Указываем ресурс для URL запроса
     * @param query - URL запрос из брайзера
     * @param absalutePath - абсалютный путь к файла
     */
    public void onPage(String query, String absalutePath) {
        pagesPathList.put(query, new File(absalutePath));
    }

    /**
     * Указываем путь к директории ресурсов
     *
     * @param rootPath
     */
    public void onPage(String rootPath) {
        File f = new File(rootPath);
        if (f.exists() && f.isDirectory()) {
            this.rootPath = rootPath;
        }
    }

    /**
     * Указываем ресурс для URL запроса
     *
     * @param query-      URL запрос из брайзера
     * @param pathResurse - абсалютный путь к файла
     */
    public void onPage(String query, File pathResurse) {
        pagesPathList.put(query, pathResurse);
    }

    /***
     * Основной поток обработки  подключения клиента
     */
    private class SocketProcessor implements Runnable {
        HashMap<String, Object> session; // локальная сессия каждого подключения
        HttpResponse query; // универсальный объект обработки запросов
        AppCompatActivity mainActivity;

        public SocketProcessor(Socket socket, AppCompatActivity mainActivity) throws IOException, JSONException {
            this.mainActivity = mainActivity;
            String textId = "C" + getMd5(socket.getRemoteSocketAddress().toString().split(":")[0] + "_" + socket.getInetAddress().getCanonicalHostName());
            if (sessionList.containsKey(textId)) {
                session = (HashMap<String, Object>) sessionList.get(textId);
            } else {
                session = new HashMap<String, Object>(10, (float) 0.5);
                sessionList.put(textId, session);
            }
            query = new HttpResponse(socket, session, this.mainActivity);
        }

        public void run() {
            try {
                if (readHead()) {
                    writeResponse();
                }
            } catch (Exception ex) {
                Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    query.close();
                    query = null;
                } catch (Exception ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        /***
         * Чтение заголовка запроса от клиента,если в первой строке невстречается слово GET или POST, тогда обрабатывается терминальное подключение
         * @return
         * @throws IOException
         */
        private boolean readHead() throws IOException, JSONException {
            StringBuffer sbInData = new StringBuffer();
            int numLin = 0;
            int charInt;
            StringBuffer sb = new StringBuffer();
            StringBuffer sbTmp = new StringBuffer();
            while ((charInt = query.inputStreamReader.read()) > 0) {
                if (query.socket.isConnected() == false) return false;
                sbTmp.append((char) charInt);
                if ((sbTmp.toString().indexOf("\r") != -1) || (sbTmp.toString().indexOf("\n") != -1) || (charInt == 0)) {
                    numLin++;
                    // если в первой строке невстречается слово GET или POST, тогда отключаем соединение
                    if ((numLin == 1) && (sb.toString().split("\r").length == 1)) {
                        int res = sb.toString().indexOf("GET");
                        if (res == -1) {
                            res = sb.toString().indexOf("POST");
                            if (res == -1) {
                                // обработка терминального запроса
                                StringBuffer sbSub2 = new StringBuffer();
                                int countEnter = 0;
                                int subcharInt = 0;
                                int subcharIntLast = 0;
                                while (query.socket.isConnected()) {
                                    query.firstMessage = sb.toString();
                                    while ((subcharInt = query.is.read()) != -1) {
                                        if (query.socket.isConnected() == false) break;
                                        if (subcharInt == 0) break;
                                        if (((subcharInt == 13) && (subcharIntLast == 13))
                                                || ((subcharInt == 10) && (subcharIntLast == 10))
                                                || ((subcharInt == 13) && (subcharIntLast == 10))
                                                || ((subcharInt == 10) && (subcharIntLast == 13))
                                        ) {
                                            countEnter += 1;
                                            if (countEnter == 2) {
                                                break; // чтение окончено
                                            }
                                        } else {
                                            countEnter = 0;
                                        }
                                        sbSub2.append((char) subcharInt);
                                        query.byteArrayOutputStream.write(subcharInt);
                                        subcharIntLast = subcharInt;
                                    }
                                    if (subcharInt == -1) break; // отключение клиента
                                    query.countQuery++;
                                    query.message = sbSub2.toString();
                                    if (callbackSocketTerminal != null) {
                                        callbackSocketTerminal.call(query);
                                    }
                                    sbSub2.setLength(0);
                                    query.byteArrayOutputStream.reset();
                                }
                                return false;
                            }
                        }
                    }
                    if (sbTmp.toString().length() == 2) {
                        break; // чтение заголовка окончено
                    }
                    sbTmp.setLength(0);
                }
                sb.append((char) charInt);
                if (sb.toString().length() > 4) { // не удачная конструкция, надо подумать о корректировки механизма поиска окончания запроса
                    if ((sb.toString().indexOf("\r\n\r\n") != -1)
                            || (sb.toString().indexOf("\r\r") != -1)
                            || (sb.toString().indexOf("\n\n") != -1)
                    ) {
                        break; // чтение заголовка окончено
                    }
                }
            }
            query.TypeQuery = "GET";
            if (sb.toString().indexOf("Content-Length: ") != -1) {
                String sbTmp2 = sb.toString().substring(sb.toString().indexOf("Content-Length: ") + "Content-Length: ".length(), sb.toString().length());
                String lengPostStr = sbTmp2.substring(0, sbTmp2.indexOf("\n")).replace("\r", "");
                int LengPOstBody = Integer.valueOf(lengPostStr);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                while ((charInt = query.inputStreamReader.read()) > 0) {
                    if (query.socket.isConnected() == false) {
                        return false;
                    }
                    // outLog.write((char) charInt);
                    buffer.write((char) charInt);
                    LengPOstBody--;
                    if (LengPOstBody == 0) {
                        break;
                    }
                }
                buffer.flush();
                // POST = buffer.toByteArray();
                query.request.put("PostBodyText", new JSONObject(new String(buffer.toByteArray())));
                query.POST = buffer.toByteArray();
                query.TypeQuery = "POST";
            }
            int indLine = 0;
            String getCmd = "";
            for (String TitleLine : sb.toString().split("\r")) {
                TitleLine = TitleLine.replace("\n", "");
                indLine++;
                if (indLine == 1) {
                    TitleLine = TitleLine.replaceAll("GET /", "");
                    TitleLine = TitleLine.replaceAll("POST /", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.1", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.0", "");
                    query.contentZapros = java.net.URLDecoder.decode(TitleLine, "UTF-8");
                    query.request.put("ContentZapros", query.contentZapros);
                    if (query.contentZapros.indexOf("?") != -1) {
                        String tmp = query.contentZapros.substring(0, query.contentZapros.indexOf("?") + 1);
                        String param = query.contentZapros.replace(tmp, "");
                        getCmd = param;
                        query.request.put("ParamAll", param);
                        int indParam = 0;
                        for (String par : param.split("&")) {
                            String[] val = par.split("=");
                            if (val.length == 2) {
                                val[0] = java.net.URLDecoder.decode(val[0], "UTF-8");
                                val[1] = java.net.URLDecoder.decode(val[1], "UTF-8");
                                query.request.put(val[0], val[1]);
                                val[0] = val[0].replace(" ", "_");
                                query.request.put(val[0], val[1]);
                                query.requestParam.put(val[0], val[1]);
                            } else {
                                indParam++;
                                val[0] = java.net.URLDecoder.decode(val[0], "UTF-8");
                                query.request.put("Param" + String.valueOf(indParam), val[0]);
                                query.requestParam.put("Param" + String.valueOf(indParam), val[0]);
                            }
                        }
                        query.contentZapros = tmp.substring(0, tmp.length() - 1);//.toLowerCase()
                    }
                    query.request.put("Zapros", query.contentZapros);
                    //query.Json.put("RootPath", rootPath);
                    //query.Json.put("AbsalutZapros", rootPath + "\\" + query.contentZapros);
                } else {
                    if (TitleLine == null || TitleLine.trim().length() == 0) {
                        break;
                    }
                    if (TitleLine.split(":").length > 0) {
                        String val = TitleLine.split(":")[0];
                        val = val.replace(" ", "_");
                        query.request.put(val, TitleLine.replace(TitleLine.split(":")[0] + ":", ""));
                    }
                    if (TitleLine.indexOf("Authorization:") == 0) {
                        //Authorization: Basic dXNlcjoxMjM=
                        String coderead = TitleLine.replaceAll("Authorization: Basic ", "");
                        query.request.put("Author", TitleLine.replaceAll("Authorization: Basic ", ""));
                    }
                }
            }
            //
            // кодировка входных данных
            if (query.request.has("Content-Type") == true) {
                // Content-Type: text/html; charset=windows-1251
                if (query.request.get("Content-Type").toString().split("charset=").length == 2) {
                    query.request.put("Charset", query.request.get("Content-Type").toString().split("charset=")[1]);
                }
            }
            // Парсим Cookie если он есть
            if (query.request.has("Cookie") == true) {
                String Cookie = query.request.get("Cookie").toString();
                Cookie = Cookie.substring(1, Cookie.length());// убираем лишний пробел сначала строки
                for (String elem : Cookie.split("; ")) {
                    String[] val = elem.split("=");
                    query.request.put(val[0], val[1]);
                    val[0] = val[0].replace(" ", "_");
                    query.request.put(val[0], val[1]);
                    query.requestParam.put(val[0], val[1]);
                }
            }
            if (query.request.has("X-Forwarded-For") == true) {
                query.requestParam.put("MacAddClient", query.GetMacClient(query.request.get("X-Forwarded-For").toString()));
            }
            sb.setLength(0);
            return true;
        }

        /**
         * Отправка ответа  вэб браузеру
         *
         * @throws IOException
         */
        private void writeResponse() throws IOException, JSONException {
            String queryText = query.request.getString("Zapros").toLowerCase();
            File pageFile = null;
            if (rootPath != null) {
                pageFile = new File(rootPath + "\\" + queryText);
            }
            if (pagesPathList.containsKey(queryText)) {
                sendRawFile(pagesPathList.get(queryText));
            } else if ((pageFile != null) && (pageFile.isFile())) {
                sendRawFile(pageFile);
            } else if (pagesList.containsKey(queryText)) {
                PageObj pageObj = pagesList.get(queryText);
                query.request.put("ContentType", pageObj.ContentType);
                pageObj.callback.call(query);
            } else if (callbackSocketPage != null) {
                callbackSocketPage.call(query);
            } else if (callbackOnPage404 != null) {
                callbackOnPage404.call(query);
            } else {
                query.sendJson("{\"ok\":true}");
            }
        }

        /**
         * Отправка бинарного файла клиенту
         *
         * @param pageFile
         */
        private void sendRawFile(File pageFile) {
            try {
                String TypeCont = ContentType(pageFile);
                // Первая строка ответа
                query.os.write("HTTP/1.1 200 OK\r\n".getBytes());
                // дата создания в GMT
                DateFormat df = DateFormat.getTimeInstance();
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                // Время последней модификации файла в GMT
                query.os.write(("Last-Modified: " + df.format(new Date(pageFile.lastModified())) + "\r\n").getBytes());
                // Длина файла
                query.os.write(("Content-Length: " + pageFile.length() + "\r\n").getBytes());
                query.os.write(("Content-Type: " + TypeCont + "; charset=utf-8\r\n").getBytes());
                //query.os.write(("Content-Type: " + TypeCont + "; ").getBytes());
                //query.os.write(("charset=" + query.Json.get("Charset") + "\r\n").getBytes());;
                // Остальные заголовки
                query.os.write("Connection: close\r\n".getBytes());
                query.os.write("Server: HTMLserver\r\n\r\n\r\n".getBytes());
                // Сам файл:
                FileInputStream fis = new FileInputStream(pageFile.getAbsolutePath());
                int lengRead = 1;
                byte buf[] = new byte[1024];
                while ((lengRead = fis.read(buf)) != -1) {
                    query.os.write(buf, 0, lengRead);
                    query.os.flush();
                }
                // закрыть файл
                fis.close();
                // завершаем соединение
                query.os.flush();
                query.is.close();
            } catch (IOException ex) {
                Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Определить по файлу тип HTML контента
     *
     * @param pageFile
     * @return
     */
    public static String ContentType(File pageFile) {
        String ras = null;
        // путь без файла
        String Dir = pageFile.getPath().replace(pageFile.getName(), "").toLowerCase();
        ;
        // имя файла с расширением
        String FileName = pageFile.getName();
        // расширение файла
        String rashirenie = FileName.substring(FileName.lastIndexOf(".") + 1);
        // путь к файлу + имя файла - расширение файла
        String DirFile = pageFile.getPath().replace("." + rashirenie, "");
        // имя файла без расширения
        String File2 = FileName.replace("." + rashirenie, "");
        rashirenie = rashirenie.toLowerCase();// преобразуем в нижний регистр

        //  try {
        //   PrintWriter pw = new PrintWriter(new FileWriter("E:\\YandexDisk\\WebServer\\HtmlServer_012\\LOG!!!!.txt"));
        //   pw.write(rashirenie);
        //   pw.close();
        //  } catch (IOException ex) {
        //     Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
        //  }

        if (rashirenie.equals("css")) {
            return "text/css";
        }
        if (rashirenie.equals("js")) {
            return "application/x-javascript";
        }
        if (rashirenie.equals("xml") || rashirenie.equals("dtd")) {
            return "text/xml";
        }
        if ((rashirenie.equals("txt")) || (rashirenie.equals("inf")) || (rashirenie.equals("nfo"))) {
            return "text/plain";
        }
        if ((rashirenie.equals("html")) || (rashirenie.equals("htm")) || (rashirenie.equals("shtml")) || (rashirenie.equals("shtm")) || (rashirenie.equals("stm")) || (rashirenie.equals("sht"))) {
            return "text/html";
        }
        if ((rashirenie.equals("mpeg")) || (rashirenie.equals("mpg")) || (rashirenie.equals("mpe"))) {
            return "video/mpeg";
        }
        if ((rashirenie.equals("ai")) || (rashirenie.equals("ps")) || (rashirenie.equals("eps"))) {
            return "application/postscript";
        }
        if (rashirenie.equals("rtf")) {
            return "application/rtf";
        }
        if ((rashirenie.equals("au")) || (rashirenie.equals("snd"))) {
            return "audio/basic";
        }
        if ((rashirenie.equals("bin")) || (rashirenie.equals("dms")) || (rashirenie.equals("lha")) || (rashirenie.equals("lzh")) || (rashirenie.equals("class")) || (rashirenie.equals("exe"))) {
            return "application/octet-stream";
        }
        if (rashirenie.equals("doc")) {
            return "application/msword";
        }
        if (rashirenie.equals("pdf")) {
            return "application/pdf";
        }
        if (rashirenie.equals("ppt")) {
            return "application/powerpoint";
        }
        if ((rashirenie.equals("smi")) || (rashirenie.equals("smil")) || (rashirenie.equals("sml"))) {
            return "pplication/smil";
        }
        if (rashirenie.equals("zip")) {
            return "application/zip";
        }
        if ((rashirenie.equals("midi")) || (rashirenie.equals("kar"))) {
            return "audio/midi";
        }
        if ((rashirenie.equals("mpga")) || (rashirenie.equals("mp2")) || (rashirenie.equals("mp3"))) {
            return "audio/mpeg";
        }
        if (rashirenie.equals("wav")) {
            return "audio/x-wav";
        }
        if (rashirenie.equals("ief")) {
            return "image/ief";
        }

        if ((rashirenie.equals("jpeg")) || (rashirenie.equals("jpg")) || (rashirenie.equals("jpe"))) {
            return "image/jpeg";
        }
        if (rashirenie.equals("png")) {
            return "image/png";
        }
        if (rashirenie.equals("ico")) {
            return "image/x-icon";
        }
        if ((rashirenie.equals("tiff")) || (rashirenie.equals("tif"))) {
            return "image/tiff";
        }
        if ((rashirenie.equals("wrl")) || (rashirenie.equals("vrml"))) {
            return "model/vrml";
        }
        if (rashirenie.equals("avi")) {
            return "video/x-msvideo";
        }
        if (rashirenie.equals("flv")) {
            return "video/x-flv";
        }
        if (rashirenie.equals("ogg")) {
            return "video/ogg";
        }
        return "application/octet-stream";
    }

    /***
     * Ренерация MD5 хэша
     * @param input
     * @return
     */
    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext.toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] HttpsClientQuery(String https_url) {
        // String https_url = "https://c.tile.openstreetmap.org/10/747/329.png";
        try {
            URL url = new URL(https_url);
            HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
            httpConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                InputStream inputStream = httpConn.getInputStream();
                ByteArrayOutputStream bufferArr = new ByteArrayOutputStream();
                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    bufferArr.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                return bufferArr.toByteArray();
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            }
            httpConn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        byte[] data = new byte[1];
        return data;
    }

    /**
     * Получить JSON объект через URL соединение
     *
     * @param https_url
     * @return
     */
    private byte[] getJSONFromUrl(String https_url) {
        try {
            URLConnection urlConnection;
            HttpURLConnection httpConn;
            URL url = new URL(https_url);
            if (url.getProtocol().toLowerCase().equals("https")) {
                trustAllHosts();
                httpConn = (HttpsURLConnection) url.openConnection();
            } else {
                httpConn = (HttpURLConnection) url.openConnection();
            }
            httpConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            httpConn.connect();
            return getResponseMessage(httpConn).toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "".getBytes();
    }

    /**
     * Получение витового контента из HTTP коннекта
     *
     * @param connection
     * @return
     * @throws Exception
     */
    private ByteArrayOutputStream getResponseMessage(HttpURLConnection connection) throws Exception {
        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream bufferReader = new ByteArrayOutputStream();
        int ch_tmp;
        while ((ch_tmp = inputStream.read()) != -1) {
            bufferReader.write(ch_tmp);
        }
        inputStream.close();
        return bufferReader;
    }

    /**
     * Скачать файл локально через URL запрос
     *
     * @param fileURL
     * @param saveFilePath
     */
    public void getFileFromUrl(String fileURL, String saveFilePath) {
        try {
            URLConnection urlConnection;
            HttpURLConnection httpConn;
            URL url = new URL(fileURL);
            if (url.getProtocol().toLowerCase().equals("https")) {
                trustAllHosts();
                httpConn = (HttpsURLConnection) url.openConnection();
            } else {
                httpConn = (HttpURLConnection) url.openConnection();
            }
            httpConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            httpConn.connect();
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                InputStream inputStream = httpConn.getInputStream();
                File cmpFiletmp = new File(saveFilePath);
                if (!cmpFiletmp.getParentFile().exists()) {
                    cmpFiletmp.getParentFile().mkdirs();
                }
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);
                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            }
            httpConn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * функция для использования HTTPS запросов
     */
    private void trustAllHosts() {
        X509TrustManager easyTrustManager = new X509TrustManager() {
            public void checkClientTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
            }

            public void checkServerTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        };
        TrustManager[] trustAllCerts = new TrustManager[]{easyTrustManager};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @author MyasnikovIA
     */
    public static class HttpResponse {
        public int countQuery = 0;
        private boolean isExit = false;
        public JSONObject request = new JSONObject();
        public JSONObject requestParam = new JSONObject();
        public byte[] POST = null;
        public Socket socket;
        public InputStream is;
        public OutputStream os;
        public DataOutputStream dataOutputStream;
        public String contentZapros = "";
        public String message = "";
        public String firstMessage = "";
        public String TypeQuery = "";
        public BufferedReader bufferedReader;
        public StringBuffer stringBuffer;
        public InputStreamReader inputStreamReader;
        public String Adress;
        public HashMap<String, Object> session = null;
        public String lastAttribetName = "msg"; // имя последнего поля
        public String lastCommandName = "msg";  // имя последней команды
        public String DeviceNameSendTo = "";  // уостройство для которого отправлено сообщение
        public String UserName = "";  // имя подключаемого устройства
        public String UserPass = "";  // Пароль подключаемого устройства
        public AppCompatActivity mainActivity;
        public ByteArrayOutputStream byteArrayOutputStream;


        public HttpResponse(Socket socket, HashMap<String, Object> session) throws IOException, JSONException {
            this.socket = socket;
            this.socket.setSoTimeout(86400000);
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.Adress = socket.getRemoteSocketAddress().toString();
            this.request.put("RemoteIPAdress", Adress);
            this.request.put("charset", "utf-8");
            //this.Json.put("RemoteMacAdress", GetMacClient(this.Adress));
            this.bufferedReader = new BufferedReader(new InputStreamReader(this.is));
            this.inputStreamReader = new InputStreamReader(this.is);
            this.contentZapros = "";
            this.session = session;
            this.stringBuffer = new StringBuffer();
            this.byteArrayOutputStream = new ByteArrayOutputStream();
        }

        public HttpResponse(Socket socket, HashMap<String, Object> session, AppCompatActivity mainActivity) throws IOException, JSONException {
            this.socket = socket;
            this.socket.setSoTimeout(86400000);
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.Adress = socket.getRemoteSocketAddress().toString();
            this.request.put("RemoteIPAdress", Adress);
            this.request.put("charset", "utf-8");
            //this.Json.put("RemoteMacAdress", GetMacClient(this.Adress));
            this.bufferedReader = new BufferedReader(new InputStreamReader(this.is));
            this.inputStreamReader = new InputStreamReader(this.is);
            this.contentZapros = "";
            this.session = session;
            this.stringBuffer = new StringBuffer();
            this.mainActivity = mainActivity;
            this.byteArrayOutputStream = new ByteArrayOutputStream();
        }

        /**
         * Закрытие соединения
         *
         * @throws IOException
         */
        public void close() throws IOException {
            if (socket.isConnected() == false) {
                return;
            }
            is.close();
            dataOutputStream.close();
            os.close();
            inputStreamReader.close();
            bufferedReader.close();
            socket.close();
            is = null;
            os = null;
            socket = null;
            inputStreamReader = null;
            bufferedReader = null;
        }

        /**
         * Получить статус подключения (устарело)
         *
         * @return
         */
        public boolean getStatusExut() {
            return isExit;
        }

        /***
         * Изменить статус подключения (устарело)
         * @return
         */
        public boolean exit() {
            isExit = !isExit;
            return isExit;
        }

        /***
         * Чтение текста от терминального клиента
         * @return
         * @throws IOException
         */
        public StringBuffer readText() throws IOException {
            int subcharInt;
            StringBuffer sbSubTmp = new StringBuffer();
            StringBuffer sbSub = new StringBuffer();
            while ((subcharInt = is.read()) != -1) {
                if (socket.isConnected() == false) break;
                if (subcharInt == 0) break;
                sbSubTmp.append((char) subcharInt);
                if (sbSubTmp.toString().indexOf("\n") != -1) {
                    if (sbSubTmp.toString().length() == 2) {
                        break; // чтение заголовка окончено
                    }
                    sbSubTmp.setLength(0);
                }
                sbSub.append((char) subcharInt);
            }
            return sbSub;
        }

        /**
         * Получить МАС адрес клиента (работает с ошибками - применять не стоит)
         *
         * @param adress
         * @return
         */
        public String GetMacClient(String adress) {
            String macStr = null;
            try {
                InetAddress address = InetAddress.getByName(adress);
                NetworkInterface ni = NetworkInterface.getByInetAddress(address);
                if (ni != null) {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                        }
                        macStr = sb.toString();
                    }
                }
            } catch (SocketException ex) {
                Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownHostException ex) {
                Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
            }
            return macStr;
        }

        /**
         * Отправка текста как HTML страницу (Устарело)
         *
         * @param Head
         * @param content
         */
        public void sendHtml(HttpResponse Head, String content) throws IOException {
            OutputStream os = Head.os;
            DataOutputStream dataOutputStream = Head.dataOutputStream;
            dataOutputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
            // дата создания в GMT
            // DateFormat df = DateFormat.getTimeInstance();
            // df.setTimeZone(TimeZone.getTimeZone("GMT"));
            // Длина файла
            dataOutputStream.write(("Content-Length: " + content.length() + "\r\n").getBytes());
            dataOutputStream.write(("Content-Type: text/html; charset=utf-8\r\n").getBytes());
            // Остальные заголовки
            dataOutputStream.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            dataOutputStream.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            dataOutputStream.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            dataOutputStream.write("Connection: close\r\n".getBytes());
            dataOutputStream.write("Server: HTMLserver\r\n\r\n".getBytes());
            dataOutputStream.write(content.getBytes(Charset.forName("UTF-8")));
            dataOutputStream.write(0);
            dataOutputStream.flush();
        }

        /**
         * Отправка бинарного файла клиенту (для браузера)
         *
         * @param Head
         * @param pageFile
         * @throws IOException
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void sendFile(HttpResponse Head, File pageFile) throws IOException {
            DataOutputStream dataOutputStream = Head.dataOutputStream;
            System.out.println(pageFile.getAbsolutePath());
            String TypeCont = ContentType(pageFile);
            byte[] data = Files.readAllBytes(Paths.get(pageFile.getAbsolutePath()));
            dataOutputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
            // дата создания в GMT
            DateFormat df = DateFormat.getTimeInstance();
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            // Длина файла
            dataOutputStream.write(("Content-Length: " + data.length + "\r\n").getBytes());
            dataOutputStream.write(("Content-Type: " + TypeCont + "; charset=utf-8\r\n").getBytes());
            dataOutputStream.write(("Last-Modified: " + df.format(new Date(pageFile.lastModified())) + "\r\n").getBytes());
            // Остальные заголовки
            dataOutputStream.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            dataOutputStream.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            dataOutputStream.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            dataOutputStream.write("Connection: close\r\n".getBytes());
            dataOutputStream.write("Server: HTMLserver\r\n\r\n".getBytes());
            dataOutputStream.write(data);
            dataOutputStream.write(0);
            dataOutputStream.flush();
        }

        /**
         * Отправка JSON объекта клиенту браузеру
         *
         * @param json
         */
        public void sendJson(JSONObject json) {
            sendJson(json.toString());
        }

        /**
         * Отправка JSON строки клиенту браузеру
         *
         * @param jsonObject
         */
        public void sendJson(String jsonObject) {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
                // дата создания в GMT
                //  DateFormat df = DateFormat.getTimeInstance();
                //  df.setTimeZone(TimeZone.getTimeZone("GMT"));
                // Длина файла
                dataOutputStream.write(("Content-Length: " + jsonObject.length() + "\r\n").getBytes());
                dataOutputStream.write(("Content-Type: application/x-javascript; charset=utf-8\r\n").getBytes());
                // Остальные заголовки
                dataOutputStream.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                dataOutputStream.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                dataOutputStream.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                dataOutputStream.write("Connection: close\r\n".getBytes());
                dataOutputStream.write("Server: HTMLserver\r\n\r\n".getBytes());
                //Log.d("TAG", jsonObject);
                dataOutputStream.write(jsonObject.getBytes(Charset.forName("UTF-8")));
                // dataOutputStream.write(jsonObject.getBytes(), 0, jsonObject.length());
                dataOutputStream.write(0);
                dataOutputStream.flush();
                // завершаем соединение
                // System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            } catch (IOException e) {
                e.printStackTrace();
                Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        public void sendHtml(String htmlContent) {
            try {
                OutputStream os = socket.getOutputStream();
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write(("Content-Type: text/html; charset=utf-8\r\n").getBytes());
                os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("Connection: close\r\n\r\n".getBytes());
                // os.write(htmlContent.getBytes(StandardCharsets.UTF_8));
                os.write(htmlContent.getBytes());
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendHtml_old(String htmlContent) {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
                dataOutputStream.write(("Content-Type: text/html; charset=utf-8\r\n").getBytes());
                dataOutputStream.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                dataOutputStream.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                dataOutputStream.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                dataOutputStream.write("Server: HTMLserver\r\n".getBytes());
                dataOutputStream.write("Connection: close\r\n\r\n".getBytes());
                //dataOutputStream.write(htmlContent.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.write(htmlContent.getBytes());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void Head(String contentType) {
            stringBuffer.setLength(0);
            stringBuffer.append("HTTP/1.1 200 OK\r\n");
            stringBuffer.append(("Content-Type: " + contentType + "; charset=utf-8\r\n"));
            stringBuffer.append("Access-Control-Allow-Origin: *\r\n");
            stringBuffer.append("Access-Control-Allow-Credentials: true\r\n");
            stringBuffer.append("Access-Control-Expose-Headers: FooBar\r\n");
            stringBuffer.append("Server: HTMLserver\r\n");
            stringBuffer.append("Connection: close\r\n\r\n");
        }

        public void Head() {
            Head("text/html");
        }

        public void Body(String content) {
            stringBuffer.append(content);
        }

        public void End() {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                //dataOutputStream.write(stringBuffer.toString().getBytes(StandardCharsets.UTF_8));
                dataOutputStream.write(stringBuffer.toString().getBytes());
                dataOutputStream.flush();
                stringBuffer.setLength(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        /***
         *  Отправляем заголовок HTML страниц
         * @param contentType - Указываем тип ответа для  интерпритации браузером (MIME type)
         * @throws IOException
         */
        public void HeadSend(String contentType) {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
                // DateFormat df = DateFormat.getTimeInstance();
                // df.setTimeZone(TimeZone.getTimeZone("GMT"));
                dataOutputStream.write(("Content-Type: " + contentType + "; charset=utf-8\r\n").getBytes());
                dataOutputStream.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                dataOutputStream.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                dataOutputStream.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                dataOutputStream.write("Server: HTMLserver\r\n".getBytes());
                dataOutputStream.write("Connection: close\r\n\r\n".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        /***
         * Отправляем заголовок HTML страниц (MIME тип по умолчанию "text/html" )
         * @throws IOException
         * @throws JSONException
         */
        public void HeadSend() throws IOException, JSONException {
            HeadSend("text/html");
        }


        /**
         * Добавляем фрагмент HTML страницы
         *
         * @param content
         * @throws IOException
         * @throws JSONException
         */
        public void BodySend(String content) throws IOException {
            BodySend(content.getBytes(Charset.forName("UTF-8")));
        }

        public void BodySend(byte[] data) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(data);
        }

        /**
         * Добавляем текст в тело HTML ответа
         *
         * @throws IOException
         */
        public void EndSend() throws IOException {
            //os.write(0);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.flush();
        }

        public void flush() throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.flush();
        }

        public void write(int data) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(data);
        }

        /**
         * Отправляем текст ответа клиенту, при терминальном подключении к серверу
         *
         * @param text
         * @throws IOException
         */
        public void write(String text) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(text.getBytes());
        }

        /**
         * Отправляем бинарный ответ клиенту, при терминальном подключении к серверу
         *
         * @param data
         * @throws IOException
         */
        public void write(byte[] data) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(data);
            dataOutputStream.write(0);
        }
    }
}