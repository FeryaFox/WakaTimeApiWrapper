package ru.feryafox.wakatimewrapper.auth;

import okhttp3.*;
import ru.feryafox.wakatimewrapper.enums.Scope;
import ru.feryafox.wakatimewrapper.utils.Parser;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AccessTokenAuth implements BaseAuth {
    private static String CLIENT_ID;
    private static String CLIENT_SECRET;
    private static String REDIRECT_URI; // e.g., http://localhost:8080/callback
    private String scope = null;

    private String accessToken;
    private String refreshToken;
    private Integer expires_in;

    private final OkHttpClient httpClient = new OkHttpClient();

    public AccessTokenAuth(String client_id, String client_secret, String redirect_uri) {
        CLIENT_ID = client_id;
        CLIENT_SECRET = client_secret;
        REDIRECT_URI = redirect_uri;
    }

    public AccessTokenAuth(String client_id, String client_secret, String redirect_uri, Scope[] scope) {
        CLIENT_ID = client_id;
        CLIENT_SECRET = client_secret;
        REDIRECT_URI = redirect_uri;
        this.scope = Scope.scopesToString(scope);
    }
    public AccessTokenAuth(String client_id, String client_secret, String redirect_uri, String scope) {
        CLIENT_ID = client_id;
        CLIENT_SECRET = client_secret;
        REDIRECT_URI = redirect_uri;
        this.scope = scope;
    }

    public AccessTokenAuth(String client_id, String client_secret, String redirect_uri, String accessToken, String refreshToken) {
        CLIENT_ID = client_id;
        CLIENT_SECRET = client_secret;
        REDIRECT_URI = redirect_uri;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public AccessTokenAuth(String client_id, String client_secret, String redirect_uri, Scope[] scope, String accessToken, String refreshToken) {
        CLIENT_ID = client_id;
        CLIENT_SECRET = client_secret;
        REDIRECT_URI = redirect_uri;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.scope = Scope.scopesToString(scope);
    }

    public AccessTokenAuth(String client_id, String client_secret, String redirect_uri, String scope, String accessToken, String refreshToken) {
        CLIENT_ID = client_id;
        CLIENT_SECRET = client_secret;
        REDIRECT_URI = redirect_uri;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    @Override
    public String getHeader() {
        return "Bearer " + accessToken;
    }

    @Override
    public void refreshToken() throws IOException {
        refreshAccessToken();
    }

    @Override
    public HashMap<String, String> getAllData() {
        HashMap<String, String> data = new HashMap<>();
        data.put("client_id", CLIENT_ID);
        data.put("client_secret", CLIENT_SECRET);
        data.put("redirect_uri", REDIRECT_URI);
        data.put("scope", scope);
        data.put("access_token", accessToken);
        data.put("refresh_token", refreshToken);
        return data;
    }

    public void setScope(Scope[] scope) {
        this.scope = Scope.scopesToString(scope);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Integer getExpiresIn() {
        return expires_in;
    }

    public String getAuthorizationUrl(String state) {
        if (scope == null) {

            HttpUrl url = HttpUrl.parse("https://wakatime.com/oauth/authorize")
                    .newBuilder()
                    .addQueryParameter("client_id", CLIENT_ID)
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("redirect_uri", REDIRECT_URI)
                    .addQueryParameter("state", state)
                    .build();
            return url.toString();
        }
        HttpUrl url = HttpUrl.parse("https://wakatime.com/oauth/authorize")
                .newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("response_type", "code")
                .addQueryParameter("redirect_uri", REDIRECT_URI)
                .addQueryParameter("state", state)
                .addQueryParameter("scope", scope)
                .build();
        return url.toString();
    }
    public String getAuthorizationUrl(){
        if (scope == null) {
            HttpUrl url = HttpUrl.parse("https://wakatime.com/oauth/authorize")
                    .newBuilder()
                    .addQueryParameter("client_id", CLIENT_ID)
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("redirect_uri", REDIRECT_URI)
                    .build();
            return url.toString();
        }
        HttpUrl url = HttpUrl.parse("https://wakatime.com/oauth/authorize")
                .newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("response_type", "code")
                .addQueryParameter("redirect_uri", REDIRECT_URI)
                .addQueryParameter("scope", scope)
                .build();
        return url.toString();
    }

    public String getAccessToken(String code) throws IOException {
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build();

        Request request = new Request.Builder()
                .url("https://wakatime.com/oauth/token")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            Map responseMap = Parser.parseUrlEncoded(response.body().string());
            accessToken = (String) responseMap.get("access_token");
            refreshToken = (String) responseMap.get("refresh_token");
            expires_in = Integer.parseInt((String) responseMap.get("expires_in"));
        }
        return accessToken;
    }

    public String refreshAccessToken() throws IOException {
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", CLIENT_SECRET)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url("https://wakatime.com/oauth/token")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            Map responseMap = Parser.parseUrlEncoded(response.body().string());
            accessToken = (String) responseMap.get("access_token");
            refreshToken = (String) responseMap.get("refresh_token");
            expires_in = Integer.parseInt((String) responseMap.get("expires_in"));
        }

        return accessToken;
    }

    public boolean revokeToken(String token) throws IOException {
        return revokeToken(token, false, null);
    }

    public boolean revokeAllUserTokens(String userId) throws IOException {
        return revokeToken(null, true, userId);
    }

    public boolean revokeAllAppTokens() throws IOException {
        return revokeToken(null, true, null);
    }

    private boolean revokeToken(String token, boolean all, String userId) throws IOException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET);

        if (token != null) {
            formBodyBuilder.add("token", token);
        }
        if (all) {
            formBodyBuilder.add("all", "true");
        }
        if (userId != null) {
            formBodyBuilder.add("user_id", userId);
        }

        RequestBody requestBody = formBodyBuilder.build();
        Request request = new Request.Builder()
                .url("https://wakatime.com/oauth/revoke")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful(); // 200 indicates success even if token is already revoked
        }
    }

    public static String generateState(){
        StringBuilder hexString = new StringBuilder();

        try {
            Random random = new SecureRandom();

            byte[] randomBytes = new byte[40];
            random.nextBytes(randomBytes);

            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            byte[] hashBytes = digest.digest(randomBytes);


            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }
}
