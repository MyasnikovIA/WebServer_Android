package ru.miacomsoft.signalserver.Interface;

import java.io.IOException;

public interface CallbackString {
    public void call(String response) throws IOException;
}

