package ru.feryafox.wakatimewrapper;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.feryafox.wakatimewrapper.auth.BaseAuth;
import okhttp3.*;
import ru.feryafox.wakatimewrapper.exceptions.WakaTimeException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static ru.feryafox.wakatimewrapper.utils.Parser.parseJson;

public class WakaTimeWrapper {
    private final BaseAuth auth;

    private final OkHttpClient httpClient = new OkHttpClient();
    public WakaTimeWrapper(BaseAuth auth){
        this.auth = auth;
    }

    /**
     * Makes an HTTP request to the WakaTime API with authorization.
     * <p>
     * This method handles adding the authorization header to the request
     * and automatically refreshes the access token if it has expired.
     *
     * @param request The Request object to be executed.
     * @return The Response object from the API.
     * @throws IOException If a network error occurs during the request.
     * @throws WakaTimeException If the API returns an error code or token refresh fails.
     */
    private Response makeRequest(Request request) throws IOException {
        request = request.newBuilder()
                .addHeader("Authorization", auth.getHeader())
                .build();
        Response response = httpClient.newCall(request).execute();

        if (response.code() == 401) {  // Unauthorized (token expired)
            // Try refreshing the access token
            auth.refreshToken();

            request = request.newBuilder()
                    .removeHeader("Authorization") // Remove old header
                    .addHeader("Authorization", auth.getHeader())
                    .build();
            response = httpClient.newCall(request).execute();

        }

        return switch (response.code()) {
            case 200, 201, 202 -> response;
            case 400 -> throw new WakaTimeException("Bad Request: " + response.body().string());
            case 401 -> throw new WakaTimeException("Unauthorized: Invalid or missing access token.");
            case 403 -> throw new WakaTimeException("Forbidden: Insufficient permissions for this request.");
            case 404 -> throw new WakaTimeException("Not Found: The requested resource does not exist.");
            case 429 -> throw new WakaTimeException("Too Many Requests: Rate limit exceeded. Please try again later.");
            case 500 ->
                    throw new WakaTimeException("Internal Server Error: WakaTime server error. Please try again later.");
            default -> throw new WakaTimeException("Unknown Error: " + response.body().string());
        };
    }

    /**
     * Retrieves the total time logged since the specified user's account was created.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param project (Optional) The project name. If provided, returns the total time for that project since its creation.
     * @return A Map containing the parsed JSON response with time data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getAllTimeSinceToday(String user, String project) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/all_time_since_today")
                .newBuilder();

        if (project != null) {
            urlBuilder.addQueryParameter("project", project);
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch commit data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves the total time logged since the currently authenticated user's account was created.
     *
     * @param project (Optional) The project name. If provided, returns the total time for that project since its creation.
     * @return A Map containing the parsed JSON response with time data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getAllTimeSinceToday(String project) throws IOException {
        return getAllTimeSinceToday("current", project);
    }

    /**
     * Retrieves data for a single commit for the specified user and project.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param project The project name.
     * @param hash The commit hash.
     * @param branch (Optional) The branch name. Defaults to the repo's default branch.
     * @return A Map containing the parsed JSON response with commit data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getCommit(String user, String project, String hash, String branch)
            throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user +
                "/projects/" + project + "/commits/" + hash).newBuilder();

        if (branch != null) {
            urlBuilder.addQueryParameter("branch", branch);
        }

        Request request = new Request.Builder().url(urlBuilder.build()).build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch commit data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves data for a single commit for the currently authenticated user and project.
     *
     * @param project The project name.
     * @param hash The commit hash.
     * @param branch (Optional) The branch name. Defaults to the repo's default branch.
     * @return A Map containing the parsed JSON response with commit data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getCommit(String project, String hash, String branch) throws IOException {
        return getCommit("current", project, hash, branch);
    }

    /**
     * Retrieves a list of commits for the specified user and project.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param project The project name.
     * @param author (Optional) Filter commits to only those authored by the given username.
     * @param branch (Optional) Filter commits to a specific branch. Defaults to the repo's default branch.
     * @param page (Optional) Page number of results.
     * @return A Map containing the parsed JSON response with commit data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getCommits(String user, String project, String author, String branch, Integer page)
            throws IOException {


        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user +
                        "/projects/" + project + "/commits")
                .newBuilder();

        if (author != null) {
            urlBuilder.addQueryParameter("author", author);
        }
        if (branch != null) {
            urlBuilder.addQueryParameter("branch", branch);
        }
        if (page != null) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch commit data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of commits for the currently authenticated user and the specified project.
     *
     * @param project The project name.
     * @param author (Optional) Filter commits to only those authored by the given username.
     * @param branch (Optional) Filter commits to a specific branch. Defaults to the repo's default branch.
     * @param page (Optional) Page number of results.
     * @return A Map containing the parsed JSON response with commit data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getCommits(String project, String author, String branch, Integer page)
            throws IOException {
        return getCommits("current", project, author, branch, page);
    }

    /**
     * Retrieves a list of data exports for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with data about data exports.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getDataDumps(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/data_dumps")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch data dumps. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of data exports for the currently authenticated user.
     *
     * @return A Map containing the parsed JSON response with data about data exports.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getDataDumps() throws IOException {
        return getDataDumps("current");
    }

    /**
     * Initiates a data export for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param type The type of data export: "daily" or "heartbeats".
     * @param emailWhenFinished (Optional) Whether to send an email notification when the export is complete. Defaults to true.
     * @return A Map containing the parsed JSON response with information about the initiated data export.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createDataDump(String user, String type, Boolean emailWhenFinished) throws IOException {
        // Build JSON body
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("type", type);
        if (emailWhenFinished != null) {
            jsonBody.put("email_when_finished", emailWhenFinished);
        }

        // Build request
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/data_dumps");
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to initiate data dump. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Initiates a data export for the currently authenticated user.
     *
     * @param type The type of data export: "daily" or "heartbeats".
     * @param emailWhenFinished (Optional) Whether to send an email notification when the export is complete. Defaults to true.
     * @return A Map containing the parsed JSON response with information about the initiated data export.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createDataDump(String type, Boolean emailWhenFinished) throws IOException {
        return createDataDump("current", type, emailWhenFinished);
    }

    /**
     * Retrieves a user's coding activity for the given day as durations.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param date The requested date in YYYY-MM-DD format.
     * @param project (Optional) Filter durations to a specific project.
     * @param branches (Optional) Comma-separated list of branch names to filter durations.
     * @param timeout (Optional) Keystroke timeout value used for joining heartbeats into durations. Defaults to the user's setting.
     * @param writesOnly (Optional) Whether to include only durations with write activity. Defaults to the user's setting.
     * @param timezone (Optional) Timezone for the given date. Defaults to the user's timezone.
     * @param sliceBy (Optional) Primary key to use for slicing durations (entity, language, dependencies, os, editor, category, machine). Defaults to "entity".
     * @return A Map containing the parsed JSON response with duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getDurations(String user, String date, String project, String branches,
                                            Integer timeout, Boolean writesOnly, String timezone, String sliceBy) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/durations")
                .newBuilder()
                .addQueryParameter("date", date);

        // Add optional parameters
        if (project != null) urlBuilder.addQueryParameter("project", project);
        if (branches != null) urlBuilder.addQueryParameter("branches", branches);
        if (timeout != null) urlBuilder.addQueryParameter("timeout", String.valueOf(timeout));
        if (writesOnly != null) urlBuilder.addQueryParameter("writes_only", String.valueOf(writesOnly));
        if (timezone != null) urlBuilder.addQueryParameter("timezone", timezone);
        if (sliceBy != null) urlBuilder.addQueryParameter("slice_by", sliceBy);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch durations. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves durations for the currently authenticated user with the given date.
     *
     * @param date The requested date in YYYY-MM-DD format.
     * @param project (Optional) Filter durations to a specific project.
     * @param branches (Optional) Comma-separated list of branch names to filter durations.
     * @param timeout (Optional) Keystroke timeout value used for joining heartbeats into durations. Defaults to the user's setting.
     * @param writesOnly (Optional) Whether to include only durations with write activity. Defaults to the user's setting.
     * @param timezone (Optional) Timezone for the given date. Defaults to the user's timezone.
     * @param sliceBy (Optional) Primary key to use for slicing durations (entity, language, dependencies, os, editor, category, machine). Defaults to "entity".
     * @return A Map containing the parsed JSON response with duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getDurations(String date, String project, String branches,
                                            Integer timeout, Boolean writesOnly, String timezone, String sliceBy) throws IOException {
        return getDurations("current", date, project, branches, timeout, writesOnly, timezone, sliceBy);
    }

    /**
     * Retrieves a list of available WakaTime IDE plugins, their versions, and colors.
     *
     * @param user       The username or "current" for the currently authenticated user (not used in this endpoint).
     * @param unreleased (Optional) Whether to include unreleased plugins. Defaults to false.
     * @return A Map containing the parsed JSON response with editor data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getEditors(String user, Boolean unreleased) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/editors")
                .newBuilder();
        if (unreleased != null) {
            urlBuilder.addQueryParameter("unreleased", unreleased.toString());
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();
        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch editor data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a user's external durations for the given day.
     *
     * @param user     The username or "current" for the currently authenticated user.
     * @param date     The requested date in YYYY-MM-DD format.
     * @param project  (Optional) Only show durations for this project.
     * @param branches (Optional) Only show durations for these branches; comma separated list of branch names.
     * @param timezone (Optional) The timezone for given date. Defaults to the user's timezone.
     * @return A Map containing the parsed JSON response with external duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getExternalDurations(String user, String date, String project, String branches, String timezone) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/external_durations")
                .newBuilder()
                .addQueryParameter("date", date);

        if (project != null) urlBuilder.addQueryParameter("project", project);
        if (branches != null) urlBuilder.addQueryParameter("branches", branches);
        if (timezone != null) urlBuilder.addQueryParameter("timezone", timezone);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch external durations. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves the currently authenticated user's external durations for the given day.
     *
     * @param date     The requested date in YYYY-MM-DD format.
     * @param project  (Optional) Only show durations for this project.
     * @param branches (Optional) Only show durations for these branches; comma separated list of branch names.
     * @param timezone (Optional) The timezone for given date. Defaults to the user's timezone.
     * @return A Map containing the parsed JSON response with external duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getExternalDurations(String date, String project, String branches, String timezone) throws IOException {
        return getExternalDurations("current", date, project, branches, timezone);
    }

    /**
     * Creates a duration representing activity for the specified user with start and end time.
     *
     * @param user        The username or "current" for the currently authenticated user.
     * @param externalId The unique identifier for this duration on the external provider.
     * @param entity      The entity which this duration is logging time towards, such as an absolute file path or a domain.
     * @param type        The type of entity; can be file, app, or domain.
     * @param startTime   The UNIX epoch timestamp when the activity started.
     * @param endTime     The UNIX epoch timestamp when the activity ended.
     * @param category    (Optional) The category for this activity.
     * @param project     (Optional) The project name.
     * @param branch      (Optional) The branch name.
     * @param language    (Optional) The language name.
     * @param meta        (Optional) A metadata string value.
     * @return A Map containing the parsed JSON response with the created duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createExternalDuration(String user, String externalId, String entity, String type,
                                                      double startTime, double endTime, String category, String project,
                                                      String branch, String language, String meta) throws IOException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("external_id", externalId);
        jsonBody.put("entity", entity);
        jsonBody.put("type", type);
        jsonBody.put("start_time", startTime);
        jsonBody.put("end_time", endTime);
        if (category != null) jsonBody.put("category", category);
        if (project != null) jsonBody.put("project", project);
        if (branch != null) jsonBody.put("branch", branch);
        if (language != null) jsonBody.put("language", language);
        if (meta != null) jsonBody.put("meta", meta);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/external_durations");
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to create external duration. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Creates a duration representing activity for the currently authenticated user with start and end time.
     *
     * @param externalId The unique identifier for this duration on the external provider.
     * @param entity      The entity which this duration is logging time towards, such as an absolute file path or a domain.
     * @param type        The type of entity; can be filed, app, or domain.
     * @param startTime   The UNIX epoch timestamp when the activity started.
     * @param endTime     The UNIX epoch timestamp when the activity ended.
     * @param category    (Optional) The category for this activity.
     * @param project     (Optional) The project name.
     * @param branch      (Optional) The branch name.
     * @param language    (Optional) The language name.
     * @param meta        (Optional) A metadata string value.
     * @return A Map containing the parsed JSON response with the created duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createExternalDuration(String externalId, String entity, String type,
                                                      double startTime, double endTime, String category, String project,
                                                      String branch, String language, String meta) throws IOException {
        return createExternalDuration("current", externalId, entity, type, startTime, endTime, category,
                project, branch, language, meta);
    }

    /**
     * Creates durations representing activities for the specified user in bulk with start and end times.
     *
     * @param user              The username or "current" for the currently authenticated user.
     * @param externalDurations A list of Maps, each representing an external duration with the following keys:
     *                          "external_id", "entity", "type", "start_time", "end_time", "category" (optional),
     *                          "project" (optional), "branch" (optional), "language" (optional), "meta" (optional).
     * @return A Map containing the parsed JSON response with status codes for each created duration.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createExternalDurationsBulk(String user, List<Map<String, Object>> externalDurations) throws IOException {
        // Build JSON body
        JSONArray durationsArray = new JSONArray();
        for (Map<String, Object> duration : externalDurations) {
            JSONObject durationObject = new JSONObject();
            durationObject.put("external_id", duration.get("external_id"));
            durationObject.put("entity", duration.get("entity"));
            durationObject.put("type", duration.get("type"));
            durationObject.put("start_time", duration.get("start_time"));
            durationObject.put("end_time", duration.get("end_time"));
            if (duration.containsKey("category")) durationObject.put("category", duration.get("category"));
            if (duration.containsKey("project")) durationObject.put("project", duration.get("project"));
            if (duration.containsKey("branch")) durationObject.put("branch", duration.get("branch"));
            if (duration.containsKey("language")) durationObject.put("language", duration.get("language"));
            if (duration.containsKey("meta")) durationObject.put("meta", duration.get("meta"));
            durationsArray.put(durationObject);
        }

        // Build request
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), durationsArray.toString());
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/external_durations.bulk");
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to create external durations in bulk. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Creates durations representing activity for the currently authenticated user with start and end time in bulk.
     *
     * @param durations A list of Maps, each representing an external duration with the same structure as the createExternalDuration method.
     * @return A Map containing the parsed JSON response with the created duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createExternalDurationsBulk(List<Map<String, Object>> durations) throws IOException {
        return createExternalDurationsBulk("current", durations);
    }

    /**
     * Retrieves a single goal for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param goal The ID of the goal to retrieve.
     * @return A Map containing the parsed JSON response with goal data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getGoal(String user, String goal) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/goals/" + goal)
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch goal data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of goals for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with goal data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getGoals(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/goals")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch goals. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of goals for the currently authenticated user.
     *
     * @return A Map containing the parsed JSON response with goal data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getGoals() throws IOException {
        return getGoals("current");
    }

    /**
     * Retrieves a user's heartbeats sent from plugins for the given day.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param date The requested date in YYYY-MM-DD format.
     * @return A Map containing the parsed JSON response with heartbeat data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getHeartbeats(String user, String date) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/heartbeats")
                .newBuilder()
                .addQueryParameter("date", date)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch heartbeats. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves the currently authenticated user's heartbeats sent from plugins for the given day.
     *
     * @param date The requested date in YYYY-MM-DD format.
     * @return A Map containing the parsed JSON response with heartbeat data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getHeartbeats(String date) throws IOException {
        return getHeartbeats("current", date);
    }

    /**
     * Creates a heartbeat representing activity for the specified user.
     *
     * @param user         The username or "current" for the currently authenticated user.
     * @param entity       The entity which this heartbeat is logging time against, such as an absolute file path or a domain.
     * @param type         The type of entity; can be file, app, or domain.
     * @param time         The UNIX epoch timestamp.
     * @param category     (Optional) The category for this activity.
     * @param project      (Optional) The project name.
     * @param branch       (Optional) The branch name.
     * @param language     (Optional) The language name.
     * @param dependencies (Optional) A comma-separated list of dependencies detected from the entity file.
     * @param lines        (Optional) The total number of lines in the entity (when entity type is file).
     * @param lineAdditions   (Optional) The number of lines added since the last heartbeat in the current file.
     * @param lineDeletions   (Optional) The number of lines removed since the last heartbeat in the current file.
     * @param lineno       (Optional) The current line row number of the cursor.
     * @param cursorpos    (Optional) The current cursor column position.
     * @param isWrite      (Optional) Whether this heartbeat was triggered from writing to a file.
     * @return A Map containing the parsed JSON response with the created heartbeat data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createHeartbeat(String user, String entity, String type, float time,
                                               String category, String project, String branch, String language,
                                               String dependencies, Integer lines, Integer lineAdditions,
                                               Integer lineDeletions, Integer lineno, Integer cursorpos,
                                               Boolean isWrite) throws IOException {
        // Build JSON body
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("entity", entity);
        jsonBody.put("type", type);
        jsonBody.put("time", time);
        if (category != null) jsonBody.put("category", category);
        if (project != null) jsonBody.put("project", project);
        if (branch != null) jsonBody.put("branch", branch);
        if (language != null) jsonBody.put("language", language);
        if (dependencies != null) jsonBody.put("dependencies", dependencies);
        if (lines != null) jsonBody.put("lines", lines);
        if (lineAdditions != null) jsonBody.put("line_additions", lineAdditions);
        if (lineDeletions != null) jsonBody.put("line_deletions", lineDeletions);
        if (lineno != null) jsonBody.put("lineno", lineno);
        if (cursorpos != null) jsonBody.put("cursorpos", cursorpos);
        if (isWrite != null) jsonBody.put("is_write", isWrite);

        // Build request
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/heartbeats");
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to create heartbeat. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Creates a heartbeat representing activity for the currently authenticated user.
     *
     * @param entity       The entity which this heartbeat is logging time against, such as an absolute file path or a domain.
     * @param type         The type of entity; can be file, app, or domain.
     * @param time         The UNIX epoch timestamp.
     * @param category     (Optional) The category for this activity.
     * @param project      (Optional) The project name.
     * @param branch       (Optional) The branch name.
     * @param language     (Optional) The language name.
     * @param dependencies (Optional) A comma-separated list of dependencies detected from the entity file.
     * @param lines        (Optional) The total number of lines in the entity (when entity type is file).
     * @param lineAdditions   (Optional) The number of lines added since the last heartbeat in the current file.
     * @param lineDeletions   (Optional) The number of lines removed since the last heartbeat in the current file.
     * @param lineno       (Optional) The current line row number of the cursor.
     * @param cursorpos    (Optional) The current cursor column position.
     * @param isWrite      (Optional) Whether this heartbeat was triggered from writing to a file.
     * @return A Map containing the parsed JSON response with the created heartbeat data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createHeartbeat(String entity, String type, float time,
                                               String category, String project, String branch, String language,
                                               String dependencies, Integer lines, Integer lineAdditions,
                                               Integer lineDeletions, Integer lineno, Integer cursorpos,
                                               Boolean isWrite) throws IOException {
        return createHeartbeat("current", entity, type, time, category, project, branch, language,
                dependencies, lines, lineAdditions, lineDeletions, lineno, cursorpos, isWrite);
    }

    /**
     * Creates heartbeats representing activities for the specified user in bulk.
     *
     * @param user          The username or "current" for the currently authenticated user.
     * @param heartbeats A list of Maps, each representing a heartbeat with the following keys:
     *                    "entity", "type", "time", "category" (optional), "project" (optional), "branch" (optional),
     *                    "language" (optional), "dependencies" (optional), "lines" (optional),
     *                    "line_additions" (optional), "line_deletions" (optional), "lineno" (optional),
     *                    "cursorpos" (optional), "is_write" (optional).
     * @return A Map containing the parsed JSON response with status codes for each created heartbeat.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createHeartbeatsBulk(String user, List<Map<String, Object>> heartbeats) throws IOException {
        // Build JSON body
        JSONArray jsonBody = new JSONArray();
        for (Map<String, Object> heartbeat : heartbeats) {
            jsonBody.put(new JSONObject(heartbeat));
        }

        // Build request
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/heartbeats.bulk");
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to create heartbeats in bulk. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Creates heartbeats representing activities for the currently authenticated user in bulk.
     *
     * @param heartbeats A list of Maps, each representing a heartbeat with the following keys:
     *                    "entity", "type", "time", "category" (optional), "project" (optional), "branch" (optional),
     *                    "language" (optional), "dependencies" (optional), "lines" (optional),
     *                    "line_additions" (optional), "line_deletions" (optional), "lineno" (optional),
     *                    "cursorpos" (optional), "is_write" (optional).
     * @return A Map containing the parsed JSON response with status codes for each created heartbeat.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> createHeartbeatsBulk(List<Map<String, Object>> heartbeats) throws IOException {
        return createHeartbeatsBulk("current", heartbeats);
    }

    /**
     * Retrieves insights about the specified user's coding activity for the given time range.
     *
     * @param user         The username or "current" for the currently authenticated user.
     * @param insightType The type of insight to retrieve (e.g., "weekday", "days", "best_day").
     * @param range        The time range for the insights (e.g., "last_7_days", "last_year", "all_time").
     * @param timeout      (Optional) The keystroke timeout value used to calculate the stats.
     * @param writesOnly   (Optional) The writes_only value used to calculate the stats.
     * @param weekday      (Optional) Filter days to only the given weekday (0-6 or "monday"-"sunday").
     * @return A Map containing the parsed JSON response with insight data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getInsights(String user, String insightType, String range,
                                           Integer timeout, Boolean writesOnly, String weekday) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user +
                        "/insights/" + insightType + "/" + range)
                .newBuilder();

        // Add optional parameters
        if (timeout != null) urlBuilder.addQueryParameter("timeout", String.valueOf(timeout));
        if (writesOnly != null) urlBuilder.addQueryParameter("writes_only", String.valueOf(writesOnly));
        if (weekday != null) urlBuilder.addQueryParameter("weekday", weekday);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch insights. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves insights about the currently authenticated user's coding activity for the given time range.
     *
     * @param insightType The type of insight to retrieve (e.g., "weekday", "days", "best_day").
     * @param range        The time range for the insights (e.g., "last_7_days", "last_year", "all_time").
     * @param timeout      (Optional) The keystroke timeout value used to calculate the stats.
     * @param writesOnly   (Optional) The writes_only value used to calculate the stats.
     * @param weekday      (Optional) Filter days to only the given weekday (0-6 or "monday"-"sunday").
     * @return A Map containing the parsed JSON response with insight data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getInsights(String insightType, String range,
                                           Integer timeout, Boolean writesOnly, String weekday) throws IOException {
        return getInsights("current", insightType, range, timeout, writesOnly, weekday);
    }

    /**
     * Retrieves a list of users ranked by coding activity in descending order.
     *
     * @param language    (Optional) Filter leaders by a specific language.
     * @param isHireable  (Optional) Filter leaders by the hireable badge.
     * @param countryCode (Optional) Filter leaders by a two-character country code.
     * @param page        (Optional) Page number of the leaderboard.
     * @return A Map containing the parsed JSON response with leader data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getLeaders(String language, Boolean isHireable, String countryCode, Integer page) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/leaders").newBuilder();

        if (language != null) {
            urlBuilder.addQueryParameter("language", language);
        }
        if (isHireable != null) {
            urlBuilder.addQueryParameter("is_hireable", isHireable.toString());
        }
        if (countryCode != null) {
            urlBuilder.addQueryParameter("country_code", countryCode);
        }
        if (page != null) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }

        Request request = new Request.Builder().url(urlBuilder.build()).build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch leaders data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of machines for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with machine data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getMachineNames(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/machine_names")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch machine names. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of machines for the currently authenticated user.
     *
     * @return A Map containing the parsed JSON response with machine data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getMachineNames() throws IOException {
        return getMachineNames("current");
    }

    /**
     * Retrieves information about WakaTime, such as a list of public IP addresses used by WakaTime servers.
     *
     * @return A Map containing the parsed JSON response with WakaTime metadata.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getMeta() throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/meta")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch WakaTime meta data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a dashboard member's coding activity for the given day as an array of durations.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param org The organization name.
     * @param dashboard The dashboard name.
     * @param member The member username.
     * @param date The requested date in YYYY-MM-DD format.
     * @param project (Optional) Only show durations for this project.
     * @param branches (Optional) Only show durations for these branches; comma separated list of branch names.
     * @return A Map containing the parsed JSON response with duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboardMemberDurations(String user, String org, String dashboard, String member, String date, String project, String branches) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/orgs/" + org + "/dashboards/" + dashboard + "/members/" + member + "/durations")
                .newBuilder()
                .addQueryParameter("date", date);

        if (project != null) {
            urlBuilder.addQueryParameter("project", project);
        }
        if (branches != null) {
            urlBuilder.addQueryParameter("branches", branches);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch durations. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a dashboard member's coding activity for the given day as an array of durations.
     *
     * @param org The organization name.
     * @param dashboard The dashboard name.
     * @param member The member username.
     * @param date The requested date in YYYY-MM-DD format.
     * @param project (Optional) Only show durations for this project.
     * @param branches (Optional) Only show durations for these branches; comma separated list of branch names.
     * @return A Map containing the parsed JSON response with duration data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboardMemberDurations(String org, String dashboard, String member, String date, String project, String branches) throws IOException {
        return getOrgDashboardMemberDurations("current", org, dashboard, member, date, project, branches);
    }

    /**
     * Retrieves an organization dashboard member's coding activity for the given time range as an array of summaries segmented by day.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param org The organization name.
     * @param dashboard The dashboard name.
     * @param member The member username.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param project (Optional) Only show time logged to this project.
     * @param branches (Optional) Only show coding activity for these branches; comma separated list of branch names.
     * @param range (Optional) Alternative way to supply start and end dates. Can be one of “Today”, “Yesterday”, “Last 7 Days”, “Last 7 Days from Yesterday”, “Last 14 Days”, “Last 30 Days”, “This Week”, “Last Week”, “This Month”, or “Last Month”.
     * @return A Map containing the parsed JSON response with summary data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboardMemberSummaries(String user, String org, String dashboard, String member, String start, String end, String project, String branches, String range) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/orgs/" + org + "/dashboards/" + dashboard + "/members/" + member + "/summaries")
                .newBuilder()
                .addQueryParameter("start", start)
                .addQueryParameter("end", end);

        if (project != null) {
            urlBuilder.addQueryParameter("project", project);
        }
        if (branches != null) {
            urlBuilder.addQueryParameter("branches", branches);
        }
        if (range != null) {
            urlBuilder.addQueryParameter("range", range);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch summaries. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves an organization dashboard member's coding activity for the given time range as an array of summaries segmented by day.
     *
     * @param org The organization name.
     * @param dashboard The dashboard name.
     * @param member The member username.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param project (Optional) Only show time logged to this project.
     * @param branches (Optional) Only show coding activity for these branches; comma separated list of branch names.
     * @param range (Optional) Alternative way to supply start and end dates. Can be one of “Today”, “Yesterday”, “Last 7 Days”, “Last 7 Days from Yesterday”, “Last 14 Days”, “Last 30 Days”, “This Week”, “Last Week”, “This Month”, or “Last Month”.
     * @return A Map containing the parsed JSON response with summary data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboardMemberSummaries(String org, String dashboard, String member, String start, String end, String project, String branches, String range) throws IOException {
        return getOrgDashboardMemberSummaries("current", org, dashboard, member, start, end, project, branches, range);
    }

    /**
     * Retrieves a list of an organization's dashboard members.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param org The organization name.
     * @param dashboard The dashboard name.
     * @return A Map containing the parsed JSON response with member data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboardMembers(String user, String org, String dashboard) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/orgs/" + org + "/dashboards/" + dashboard + "/members")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch dashboard members. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of an organization's dashboard members.
     *
     * @param org The organization name.
     * @param dashboard The dashboard name.
     * @return A Map containing the parsed JSON response with member data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboardMembers(String org, String dashboard) throws IOException {
        return getOrgDashboardMembers("current", org, dashboard);
    }

    /**
     * Retrieves a list of an organization's dashboards.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param org The organization name.
     * @return A Map containing the parsed JSON response with dashboard data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboards(String user, String org) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/orgs/" + org + "/dashboards")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch dashboards. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of an organization's dashboards.
     *
     * @param org The organization name.
     * @return A Map containing the parsed JSON response with dashboard data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgDashboards(String org) throws IOException {
        return getOrgDashboards("current", org);
    }


    /**
     * Retrieves a list of a user's organizations.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with organization data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgs(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/orgs")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch organizations. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of a user's organizations.
     *
     * @return A Map containing the parsed JSON response with organization data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getOrgs() throws IOException {
        return getOrgs("current");
    }

    /**
     * Retrieves a list of a user's private leaderboards.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with private leaderboard data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getPrivateLeaderboards(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/leaderboards")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch private leaderboards. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of a user's private leaderboards.
     *
     * @return A Map containing the parsed JSON response with private leaderboard data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getPrivateLeaderboards() throws IOException {
        return getPrivateLeaderboards("current");
    }

    /**
     * Retrieves a list of users in a private leaderboard ranked by coding activity in descending order.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param board The ID of the private leaderboard to retrieve.
     * @param language (Optional) Filter leaders by a specific language.
     * @param countryCode (Optional) Filter leaders by two-character country code.
     * @param page (Optional) Page number of leaderboard. If authenticated, defaults to the page containing the currently authenticated user.
     * @return A Map containing the parsed JSON response with leaderboard data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getPrivateLeaderboardLeaders(String user, String board, String language, String countryCode, Integer page) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/leaderboards/" + board)
                .newBuilder();

        if (language != null) {
            urlBuilder.addQueryParameter("language", language);
        }
        if (countryCode != null) {
            urlBuilder.addQueryParameter("country_code", countryCode);
        }
        if (page != null) {
            urlBuilder.addQueryParameter("page", String.valueOf(page));
        }

        Request request = new Request.Builder().url(urlBuilder.build()).build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch private leaderboard leaders. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of users in a private leaderboard ranked by coding activity in descending order.
     *
     * @param board The ID of the private leaderboard to retrieve.
     * @param language (Optional) Filter leaders by a specific language.
     * @param countryCode (Optional) Filter leaders by two-character country code.
     * @param page (Optional) Page number of leaderboard. If authenticated, defaults to the page containing the currently authenticated user.
     * @return A Map containing the parsed JSON response with leaderboard data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getPrivateLeaderboardLeaders(String board, String language, String countryCode, Integer page) throws IOException {
        return getPrivateLeaderboardLeaders("current", board, language, countryCode, page);
    }

    /**
     * Retrieves a list of all verified program languages supported by WakaTime.
     *
     * @return A Map containing the parsed JSON response with language data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getProgramLanguages() throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/program_languages")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch program languages. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of WakaTime projects for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param q (Optional) Filter project names by a search term.
     * @return A Map containing the parsed JSON response with project data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getProjects(String user, String q) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/projects")
                .newBuilder();

        if (q != null) {
            urlBuilder.addQueryParameter("q", q);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch projects. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of WakaTime projects for the currently authenticated user.
     *
     * @param q (Optional) Filter project names by a search term.
     * @return A Map containing the parsed JSON response with project data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getProjects(String q) throws IOException {
        return getProjects("current", q);
    }

    /**
     * Retrieves a user's coding activity for the given time range.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param range (Optional) YYYY year, YYYY-MM month, or one of last_7_days, last_30_days, last_6_months, last_year, or all_time. When range isn’t present, the user’s public profile range is used.
     * @param timeout (Optional) The keystroke timeout value used to calculate these stats. Defaults the the user's keystroke timeout value.
     * @param writesOnly (Optional) The writes_only value used to calculate these stats. Defaults to the user's writes_only setting.
     * @return A Map containing the parsed JSON response with stats data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getStats(String user, String range, Integer timeout, Boolean writesOnly) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/stats")
                .newBuilder();

        if (range != null) {
            urlBuilder.addPathSegment(range);
        }
        if (timeout != null) {
            urlBuilder.addQueryParameter("timeout", String.valueOf(timeout));
        }
        if (writesOnly != null) {
            urlBuilder.addQueryParameter("writes_only", String.valueOf(writesOnly));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch stats. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a user's coding activity for the given time range.
     *
     * @param timeout (Optional) The keystroke timeout value used to calculate these stats. Defaults the the user's keystroke timeout value.
     * @param writesOnly (Optional) The writes_only value used to calculate these stats. Defaults to the user's writes_only setting.
     * @return A Map containing the parsed JSON response with stats data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getStats(Integer timeout, Boolean writesOnly) throws IOException {
        return getStats("current", null, timeout, writesOnly);
    }

    /**
     * Retrieves aggregate stats of all WakaTime users over the given time range.
     *
     * @param range (Optional) One of last_7_days or any year in the past since 2013 (e.g., 2020).
     * @return A Map containing the parsed JSON response with aggregate stats data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getStatsAggregated(String range) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/stats")
                .newBuilder();

        if (range != null) {
            urlBuilder.addPathSegment(range);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch aggregated stats. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a user's coding activity today for displaying in IDE text editor status bars.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with status bar data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getStatusbarToday(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/status_bar/today")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch status bar data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves the currently authenticated user's coding activity today for displaying in IDE text editor status bars.
     *
     * @return A Map containing the parsed JSON response with status bar data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getStatusbarToday() throws IOException {
        return getStatusbarToday("current");
    }

    /**
     * Retrieves a user's coding activity for the given time range as an array of summaries segmented by day.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param project (Optional) Only show time logged to this project.
     * @param branches (Optional) Only show coding activity for these branches; comma separated list of branch names.
     * @param timeout (Optional) The keystroke timeout preference used when joining heartbeats into durations. Defaults the the user's keystroke timeout value. See the FAQ for more info.
     * @param writesOnly (Optional) The writes_only preference. Defaults to the user's writes_only setting.
     * @param timezone (Optional) The timezone for given start and end dates. Defaults to the user's timezone.
     * @param range (Optional) Alternative way to supply start and end dates. Can be one of “Today”, “Yesterday”, “Last 7 Days”, “Last 7 Days from Yesterday”, “Last 14 Days”, “Last 30 Days”, “This Week”, “Last Week”, “This Month”, or “Last Month”.
     * @return A Map containing the parsed JSON response with summary data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getSummaries(String user, String start, String end, String project, String branches, Integer timeout, Boolean writesOnly, String timezone, String range) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/summaries")
                .newBuilder()
                .addQueryParameter("start", start)
                .addQueryParameter("end", end);

        if (project != null) {
            urlBuilder.addQueryParameter("project", project);
        }
        if (branches != null) {
            urlBuilder.addQueryParameter("branches", branches);
        }
        if (timeout != null) {
            urlBuilder.addQueryParameter("timeout", String.valueOf(timeout));
        }
        if (writesOnly != null) {
            urlBuilder.addQueryParameter("writes_only", String.valueOf(writesOnly));
        }
        if (timezone != null) {
            urlBuilder.addQueryParameter("timezone", timezone);
        }
        if (range != null) {
            urlBuilder.addQueryParameter("range", range);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch summaries. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a user's coding activity for the given time range as an array of summaries segmented by day.
     *
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param project (Optional) Only show time logged to this project.
     * @param branches (Optional) Only show coding activity for these branches; comma separated list of branch names.
     * @param timeout (Optional) The keystroke timeout preference used when joining heartbeats into durations. Defaults the the user's keystroke timeout value. See the FAQ for more info.
     * @param writesOnly (Optional) The writes_only preference. Defaults to the user's writes_only setting.
     * @param timezone (Optional) The timezone for given start and end dates. Defaults to the user's timezone.
     * @param range (Optional) Alternative way to supply start and end dates. Can be one of “Today”, “Yesterday”, “Last 7 Days”, “Last 7 Days from Yesterday”, “Last 14 Days”, “Last 30 Days”, “This Week”, “Last Week”, “This Month”, or “Last Month”.
     * @return A Map containing the parsed JSON response with summary data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getSummaries(String start, String end, String project, String branches, Integer timeout, Boolean writesOnly, String timezone, String range) throws IOException {
        return getSummaries("current", start, end, project, branches, timeout, writesOnly, timezone, range);
    }

    /**
     * Retrieves a list of plugins that have sent data for the specified user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with user agent data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getUserAgents(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user + "/user_agents")
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch user agents. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves a list of plugins that have sent data for the currently authenticated user.
     *
     * @return A Map containing the parsed JSON response with user agent data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getUserAgents() throws IOException {
        return getUserAgents("current");
    }

    /**
     * Retrieves information about a single user.
     *
     * @param user The username or "current" for the currently authenticated user.
     * @return A Map containing the parsed JSON response with user data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getUser(String user) throws IOException {
        HttpUrl url = HttpUrl.parse("https://wakatime.com/api/v1/users/" + user)
                .newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = makeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new WakaTimeException("Failed to fetch user data. HTTP code: " + response.code());
            }
            return parseJson(response.body().string());
        }
    }

    /**
     * Retrieves information about the currently authenticated user.
     *
     * @return A Map containing the parsed JSON response with user data.
     * @throws IOException If an error occurs during the API request.
     * @throws WakaTimeException If the API returns an error code.
     */
    public Map<String, Object> getUser() throws IOException {
        return getUser("current");
    }
}
