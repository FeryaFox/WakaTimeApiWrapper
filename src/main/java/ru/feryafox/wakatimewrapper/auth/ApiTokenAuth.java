package ru.feryafox.wakatimewrapper.auth;
import java.util.Base64;
import java.util.HashMap;

public class ApiTokenAuth implements BaseAuth{

    private String api_token_base64;
    private String api_token;

    ApiTokenAuth(String api_token){
        this.api_token_base64 = Base64.getEncoder().encodeToString(api_token.getBytes());
        this.api_token = api_token;
    }

    @Override
    public HashMap<String, String> getAllData() {
        HashMap<String, String> data = new HashMap<>();

        data.put("api_token", api_token);

        return data;
    }

    @Override
    public String getHeader() {
        return "Basic " + api_token_base64;
    }

    @Override
    public void refreshToken() {

    }
}
