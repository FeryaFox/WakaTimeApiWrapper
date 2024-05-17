package ru.feryafox.wakatimewrapper.auth;

import java.io.IOException;
import java.util.HashMap;

public interface BaseAuth {
    public String getHeader();
    public HashMap<String, String> getAllData();
    public void refreshToken() throws IOException;
}
