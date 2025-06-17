package com.plugin.demo.yapi;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.plugin.demo.setting.CodeChronoSettings;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Liu Guangxin
 * @date 2025/6/16 16:39
 */
public class YapiInterface {
    private static String cookie;

    public static String getToken() {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/user/login";
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        String formatted = formatter.format(dateTime);
        String cookie = "_yapi_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjI5NSwiaWF0IjoxNzUwMDYzNzM3LCJleHAiOjE3NTA2Njg1Mzd9.pnrL0OF7ZgwZGmNhOYQgu-UKdl1oyf__lyVZcDcqW94;" +
                " path=/; expires=" + formatted +
                "; httponly";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", state.userName);
        jsonObject.put("password", state.password);
        String jsonBodyStr = JSON.toJSONString(jsonObject);
        String jsonStr = HttpRequest.post(url)
                .header("content-type", "application/json; charset=utf-8")
                .header("set-cookie", cookie)
                .timeout(10 * 60 * 1000)
                .body(jsonBodyStr)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        if (0 == (int) object.get("errcode")) {
            YapiInterface.cookie = cookie;
            return cookie;
        }
        return "";
    }

    private static String getCookie() {
        if (cookie == null) {
            return getToken();
        } else {
            return cookie;
        }
    }

    public static Map<String, String> getGroup() {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/group/list";
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(10 * 60 * 1000)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        JSONArray jsonArray = object.getJSONArray("data");
        Map<String, String> map = new HashMap<>(jsonArray.size());
        for (Object data : jsonArray) {
            JSONObject dataObject = (JSONObject) data;
            map.put(dataObject.getString("group_name"), dataObject.getString("_id"));
        }
        return map;
    }

    public static Map<String, String> getProject() {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String groupName = state.groupName;
        String groupId = getGroup().get(groupName);
        String url = state.url + "/api/project/list?group_id=" + groupId;
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(10 * 60 * 1000)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("list");
        Map<String, String> map = new HashMap<>(jsonArray.size());
        for (Object data : jsonArray) {
            JSONObject dataObject = (JSONObject) data;
            map.put(dataObject.getString("name"), dataObject.getString("_id"));
        }
        return map;
    }

    public static Map<String, String> getCategory() {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String projectName = state.projectName;
        String categoryId = getProject().get(projectName);
        String url = state.url + "/api/interface/list_menu?project_id=" + categoryId;
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(10 * 60 * 1000)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        JSONArray jsonArray = object.getJSONArray("data");
        Map<String, String> map = new HashMap<>(jsonArray.size());
        for (Object data : jsonArray) {
            JSONObject dataObject = (JSONObject) data;
            map.put(dataObject.getString("name"), dataObject.getString("_id"));
        }
        return map;
    }

    public static String getInterfaceId(String catId, String title, String path, String methodName) {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/interface/list_cat?catid=" + catId;
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(10 * 60 * 1000)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("list");
        Map<String, String> map = new HashMap<>(jsonArray.size());
        for (Object data : jsonArray) {
            JSONObject dataObject = (JSONObject) data;
            String key = dataObject.getString("title")
                    + dataObject.getString("path")
                    + dataObject.getString("method");
            map.put(key, dataObject.getString("_id"));
        }
        return map.get(title + path + methodName);
    }

    public static String createCategory() {
        return "";
    }

    public static String createGroup() {
        return "";
    }

    public static String createProject() {
        return "";
    }

    public static Map<String, String> createInterface(String methodName, String title, String path) {
        Map<String, String> resultMap = new HashMap<>(2);
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        if (StringUtils.isBlank(state.url)
                && StringUtils.isBlank(state.userName)
                && StringUtils.isBlank(state.password)
                && StringUtils.isBlank(state.groupName)
                && StringUtils.isBlank(state.projectName)
                && StringUtils.isBlank(state.categoryName)
        ) {
            resultMap.put("status", "fail");
            resultMap.put("errorCode", "10086");
            resultMap.put("errmsg", "Yapi相关配置存在未填写参数，请至Settings -> Tools -> Yapi_ApiKey处填写");
            return resultMap;
        }
        Map<String, String> map = getCategory();
        String url = state.url + "/api/interface/add";
        Map<String, String> projectMap = getProject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", methodName);
        jsonObject.put("catid", map.get(state.categoryName));
        jsonObject.put("title", title);
        jsonObject.put("path", path);
        jsonObject.put("project_id", projectMap.get(state.projectName));
        String jsonBodyStr = JSON.toJSONString(jsonObject);
        String jsonStr = HttpRequest.post(url)
                .header("content-type", "application/json;charset=UTF-8")
                .header("cookie", cookie)
                .timeout(10 * 60 * 1000)
                .body(jsonBodyStr)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        if (0 == (int) object.get("errcode")) {
            resultMap.put("status", "success");
            resultMap.put("id", object.getJSONObject("data").getInteger("_id").toString());
            resultMap.put("catId", map.get(state.categoryName));
            return resultMap;
        } else {
            resultMap.put("status", "fail");
            String id = getInterfaceId(map.get(state.categoryName), title, path, methodName);
            resultMap.put("errorCode", object.getInteger("errcode").toString());
            resultMap.put("errmsg", object.getString("errmsg"));
            resultMap.put("id", id);
            resultMap.put("catId", map.get(state.categoryName));
            //  已存在的接口 40022
            return resultMap;
        }

    }

    public static String updateInterface(String id, String catId, String methodName,
                                         String path, String body, String returnResult,
                                         JSONArray params, String title) {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/interface/up";
        JSONObject jsonObject = new JSONObject();
        if ("GET".equals(methodName)) {
            jsonObject.put("req_query", params);
        }
        jsonObject.put("api_opened", false);
        jsonObject.put("catid", catId);
        jsonObject.put("desc", "");
        jsonObject.put("id", id);
        jsonObject.put("markdown", "");

        jsonObject.put("method", methodName);
        jsonObject.put("path", path);
        jsonObject.put("req_body_form", new ArrayList<>());
        jsonObject.put("req_body_is_json_schema", true);
        jsonObject.put("req_body_other", body);

        jsonObject.put("req_body_type", "json");
//        jsonObject.put("req_headers", path);
        // 处理路径参数
        JSONArray jsonArray = new JSONArray();
        List<String> pathParams = extractPathParams(path);
        for (String param : pathParams) {
            JSONObject object = new JSONObject();
            object.put("name", param);
            object.put("desc", "");
            jsonArray.add(object);
        }
        jsonObject.put("req_params", jsonArray);

        jsonObject.put("res_body", returnResult);

        jsonObject.put("res_body_is_json_schema", true);
        jsonObject.put("res_body_type", "json");
        jsonObject.put("status", "undone");
        jsonObject.put("switch_notice", true);
        jsonObject.put("tag", new ArrayList<>());
        jsonObject.put("title", title);

        String jsonBodyStr = JSON.toJSONString(jsonObject);
        String jsonStr = HttpRequest.post(url)
                .header("content-type", "application/json;charset=UTF-8")
                .header("cookie", cookie)
                .timeout(10 * 60 * 1000)
                .body(jsonBodyStr)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        if (0 == (int) object.get("errcode")) {
            return "success";
        } else {
            return "fail";
        }
    }

    public static List<String> extractPathParams(String url) {
        List<String> params = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{(\\w+)}");
        Matcher matcher = pattern.matcher(url);

        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return params;
    }
}
