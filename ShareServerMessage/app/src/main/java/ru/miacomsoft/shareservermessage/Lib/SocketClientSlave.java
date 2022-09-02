package ru.miacomsoft.shareservermessage.Lib;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Пример использования:
 * <p>
 * mBtnSend = (Button) findViewById(R.id.btn_send);
 * mTextView = (TextView) findViewById(R.id.textView);
 * mEdit = (EditText) findViewById(R.id.edText);
 * <p>
 * mBtnSend.setOnClickListener(new View.OnClickListener() {
 *
 * @Override public void onClick(View v) {
 * String text;
 * text = mEdit.getText().toString();
 * // отправляем сообщение
 * sc.send(text + "\r\r\r",(byte[] data)->{
 * Log.e("MainActivity", " Ответ на команду " + new String(data));
 * });
 * }
 * });
 * <p>
 * new Thread(new Runnable() {
 * @Override public void run() {
 * sc = new SocketClientSlave(HOST, PORT, "t4");
 * sc.loop((byte[] data) -> {
 * // mTextView.setText(countmes+" "+new String(data));
 * Log.e("MainActivity", " Основной поток " + new String(data));
 * runOnUiThread(new Runnable() { // переключаем контекст основного потока интерфейса, для визуализации данных
 * @Override public void run() {
 * mTextView.setText(new String(data));
 * }
 * });
 * });
 * sc.start();
 * }
 * }).start();
 */
public class SocketClientSlave {

    public interface CallbackByteArr { // интерфейс для колбэков
        public void call(byte[] response);
    }

    class MessageObject {
        public CallbackByteArr callbackByteArr = null;
        public byte[] data;

        public MessageObject(byte[] data, CallbackByteArr callbackByteArr) {
            this.callbackByteArr = callbackByteArr;
            this.data = data;
        }
    }

    private String HOST = "128.0.24.172";
    private int PORT = 8200;
    private String DeviceName;
    private boolean isConnectAndLoop = false; // признак работы  отдельного подключения
    private boolean isWorClient = false;     // признак работы клиента, если подкючение разрывается, а клиент еще работает, то  происходит переподключение к сигнальному серверу через 10 секунд
    private Connection mConnect = null;
    private MainClientLoop mainClientLoop = null;
    private Thread mainClientThread = null;

    private static String LOG_TAG = "MainActivity";
    private CallbackByteArr callbackByteArrLoop = null;
    private ArrayList<MessageObject> MESSAGE_ARRAY = new ArrayList<MessageObject>();


    public SocketClientSlave(String HOST, int PORT, String DeviceName) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.DeviceName = DeviceName;
    }

    public void loop(CallbackByteArr callbackByteArrLoop) {
        this.callbackByteArrLoop = callbackByteArrLoop;
    }

    public void start() {
        isWorClient = true;
        while (isWorClient) {
            isConnectAndLoop = true;
            try {
                mainClientThread = new Thread(new MainClientLoop());
                mainClientThread.start();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            while (isConnectAndLoop) {
                pause(2000);
            }
            Log.d(LOG_TAG, "restart");
        }
    }

    class MainClientLoop implements Runnable {
        @Override
        public void run() {
            try {
                if (mConnect != null) {
                    mConnect.finalize();
                    mConnect = null;
                }
                // Создание подключения
                mConnect = new Connection(HOST, PORT);
                mConnect.openConnection(DeviceName, callbackByteArrLoop);
                send("list\r\r\r");
            } catch (Throwable throwable) {
                Log.e(LOG_TAG, throwable.getMessage());
                throwable.printStackTrace();
            }
        }
    }


    public void shellExecute(String cmd) {
        Process proc;
        try {
            proc = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                send(line + "\r");
                proc.waitFor();
            }
            send("\r\r\r");
        } catch (Exception e) {
            send(e.getMessage() + "\r\r\r");
        }
    }

    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "IOException: %s%n", e);
        }
    }


    public void send(String msg) {
        send(msg.getBytes(), null);
    }

    public void send(String msg, CallbackByteArr callbackByteArr) {
        send(msg.getBytes(), callbackByteArr);
    }

    public void send(byte[] msgByte, CallbackByteArr callbackByteArr) {
        if (mConnect == null) {
            // isConnectAndLoop = false;
            Log.e(LOG_TAG, "Соединение не установлено");
        } else {
            MESSAGE_ARRAY.add(new MessageObject(msgByte, callbackByteArr));
        }
    }

    public class Connection {

        private InputStream is;
        private OutputStream outputStream;
        private CallbackByteArr callbackLoop;
        private Socket mSocket = null;
        private String mHost = null;
        private int mPort = 0;

        public Connection() {
        }

        public Connection(final String host, final int port) {
            this.mHost = host;
            this.mPort = port;
        }

        // Метод открытия сокета
        public void openConnection(String DeviceName, CallbackByteArr callbackLoop) throws Exception {
            this.callbackLoop = callbackLoop;
            closeConnection(); // Если сокет уже открыт, то он закрывается
            try {
                isConnectAndLoop = true;
                // Создание сокета
                mSocket = new Socket(mHost, mPort);
                InputStream inputStream = mSocket.getInputStream();
                is = mSocket.getInputStream();
                outputStream = mSocket.getOutputStream();
                outputStream.write((DeviceName + "\r\r").getBytes()); // регистрация устройства на сервере
                outputStream.write(0);
                outputStream.flush();
                StringBuffer sbSub2 = new StringBuffer();
                int subcharInt = 0;
                while ((subcharInt = inputStream.read()) != 0) {
                    sbSub2.append((char) subcharInt); // читаем информацию с сервера, при регистрации устройства
                }
                ByteArrayOutputStream bufferReader = new ByteArrayOutputStream();
                while (isConnectAndLoop) {
                    // если есть данные входящие с сервера, тогда вычитываем их все
                    while ((is.available() > 0) && ((subcharInt = inputStream.read()) != -1)) {
                        if (subcharInt == -1) break;
                        if (!isConnectAndLoop) break;
                        bufferReader.write(subcharInt);
                        if ((subcharInt == 10) || (subcharInt == 0)) {
                            if (this.callbackLoop != null) {
                                this.callbackLoop.call(bufferReader.toByteArray());
                            } else {
                                Log.d(LOG_TAG, "callbackLoop " + subcharInt + "  " + new String(bufferReader.toByteArray()));
                            }
                            bufferReader.reset();
                        }
                    }
                    // если очередь сообщений не пустая, тогда обрабатываем сообщения на отправку
                    if (MESSAGE_ARRAY.size() > 0) {
                        for (int i = 0; i < MESSAGE_ARRAY.size(); i++) {
                            MessageObject msg = MESSAGE_ARRAY.get(i);
                            sendData(msg.data); // отправка сообщения
                            if (msg.callbackByteArr != null) { // если к сообщению прикреплен колбэк, тогда ждем ответа и прочитав его отправляем в колбэк функцию
                                ByteArrayOutputStream bufferReaderSub = new ByteArrayOutputStream();
                                while ((subcharInt = inputStream.read()) != -1) {
                                    if (subcharInt == -1)
                                        break; // если разрыв соединения , тогда выходим из чтения
                                    if (!isConnectAndLoop)
                                        break; // если остановлен основной поток (механизм имеет ошибку, надо исправить)
                                    bufferReaderSub.write(subcharInt);
                                    if ((subcharInt == 10) || (subcharInt == 0) || (is.available() == 0)) {
                                        msg.callbackByteArr.call(bufferReaderSub.toByteArray());
                                        bufferReaderSub.reset(); //очищаем буфер
                                        if (is.available() == 0)
                                            break; // если все входящие данные прочитаны, тогда выходим из цикла чтения
                                    }
                                }
                            }
                        }
                        MESSAGE_ARRAY.clear();// очистка очереди сообщений
                    }
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                isConnectAndLoop = false;
            }
        }

        /**
         * Метод закрытия сокета
         */
        public void closeConnection() {
            if (mSocket != null && !mSocket.isClosed()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Ошибка при закрытии сокета :" + e.getMessage());
                } finally {
                    mSocket = null;
                }
            }
            mSocket = null;
            isConnectAndLoop = false;
        }

        /**
         * Метод отправки данных
         */
        public void sendData(byte[] data) throws Exception {
            // Проверка открытия сокета
            if (mSocket == null || mSocket.isClosed()) {
                isConnectAndLoop = false;
                Log.e(LOG_TAG, "Ошибка отправки данных. " + "Сокет не создан или закрыт");
                mainClientThread.stop();
                //throw new Exception("Ошибка отправки данных. " + "Сокет не создан или закрыт");
            }
            // Отправка данных
            try {
                mSocket.getOutputStream().write(data);
                mSocket.getOutputStream().flush();
            } catch (IOException e) {
                isConnectAndLoop = false;
                mainClientThread.stop();
                Log.e(LOG_TAG, "Ошибка отправки данных : " + e.getMessage());
                // throw new Exception("Ошибка отправки данных : " + e.getMessage());
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            closeConnection();
        }
    }

}
