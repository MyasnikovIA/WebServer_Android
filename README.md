# WebServer_Android
Вэб сервер для ОС Android

В примере раализована раздача вэб страниц, и возможность организовать обмен  через Socket подключение

Пример реализации вэб сервера  на платформе Android
<img src="https://github.com/MyasnikovIA/WebServer_Android/blob/main/img/20220223_101839.jpg?raw=true"/>

Обращение к реурсу из браузера<br/>
<img src="https://github.com/MyasnikovIA/WebServer_Android/blob/main/img/brow.png?raw=true"/>

Пример написания Вэб рксурса в JAVA коде
<img src="https://github.com/MyasnikovIA/WebServer_Android/blob/main/img/TestPage.png?raw=true"/>

Привязка ресурса к Java коду
<img src="https://github.com/MyasnikovIA/WebServer_Android/blob/main/img/TestPageColl.png?raw=true"/>

Обмен данными с исмользованием POST AJAX запросы, с любого сервера:
<pre>
    var DevFromName = "LazerCrtl"; // имя отправителя сообщения
    var DevName = "LaserPowerSW"; // имя устройство для которого предназначено сообщение
    var DevPass = "**********"; // Пароль устройства для которого отправляется сообщение
    send = function (data) {
        const request = new XMLHttpRequest();
        request.open('POST', 'http://128.0.24.172:8200/signalchange.ru', true);
        request.addEventListener("readystatechange", () => {
            if (request.readyState === 4 && request.status === 200) {
                console.log("Сообщение отправлено устройству через сервер сообщений:"+request.responseText);
                setTimeout(function tick() { // Пауза перед чтением ответа 
                    const requestLog = new XMLHttpRequest();
                    requestLog.open('POST', 'http://128.0.24.172:8200/signalchange.ru', true);
                    requestLog.addEventListener("readystatechange", () => {
                        if (requestLog.readyState === 4 && requestLog.status === 200) {
                            console.log("Получен ответ с устройства через сервер сообщений:");
                            console.log(requestLog.responseText);
                        }
                    });
                    requestLog.send(JSON.stringify({pop:DevFromName,from: DevFromName}));
                }, 1000);
            }
        });
        request.send(JSON.stringify(data));
    }


    RestartDev = function () {
        send({send: DevName, from: DevFromName, pass: DevPass, reset: 0, delay: 1000000});
    };

    OnVent = function () {
        send({send: DevName, from: DevFromName, on: 1, pass: DevPass});
    };

    OffVent = function () {
        send({send: DevName, from: DevFromName, off: 1, pass: DevPass});
    };

</pre>

