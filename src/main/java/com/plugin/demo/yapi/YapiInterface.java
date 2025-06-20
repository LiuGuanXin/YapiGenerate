package com.plugin.demo.yapi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.plugin.demo.setting.CodeChronoSettings;
import org.apache.commons.lang3.StringUtils;

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
    private static String userName;
    private static String password;
    private static final Integer TIMEOUT = 5 * 1000;
    public static String getToken() {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/user/login";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", state.userName);
        jsonObject.put("password", state.password);
        String jsonBodyStr = JSON.toJSONString(jsonObject);
        HttpResponse httpResponse = HttpRequest.post(url)
                .header("content-type", "application/json; charset=utf-8")
                .timeout(TIMEOUT)
                .body(jsonBodyStr)
                .execute();
        String jsonStr = httpResponse.body();
        JSONObject object = JSON.parseObject(jsonStr);
        if (0 == (int) object.get("errcode")) {
            List<String> heads = httpResponse.headerList("set-cookie");
            String cookie = "";
            for (String head : heads) {
                if (head.startsWith("_yapi_token=")) {
                    cookie = head;
                }
            }
            userName = state.userName;
            password = state.password;
            YapiInterface.cookie = cookie;
            return cookie;
        }
        return "";
    }

    private static String getCookie() {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        if (cookie == null || cookie.isEmpty()
                || !ObjectUtil.equal(userName, state.userName)
                || !ObjectUtil.equal(password, state.password)) {
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
                .timeout(TIMEOUT)
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
                .timeout(TIMEOUT)
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
                .timeout(TIMEOUT)
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

    public static String getInterfaceId(String projectId, String path, String methodName) {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/interface/list?page=1&limit=10000&project_id=" + projectId;
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(TIMEOUT)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("list");
        String id = "";
        for (Object data : jsonArray) {
            JSONObject o = (JSONObject) data;
            if (path.equals(o.getString("path")) && methodName.equals(o.getString("method"))) {
                id = o.getString("_id");
            }
        }
        return id;
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
        if (StringUtils.isEmpty(state.url)
                || StringUtils.isEmpty(state.userName)
                || StringUtils.isEmpty(state.password)
                || StringUtils.isEmpty(state.groupName)
                || StringUtils.isEmpty(state.projectName)
                || StringUtils.isEmpty(state.categoryName)
        ) {
            resultMap.put("status", "fail");
            resultMap.put("errorCode", "10086");
            resultMap.put("errmsg", "Yapi相关配置存在未填写参数，请至Settings -> Tools -> Yapi_ApiKey处填写");
            return resultMap;
        }
        Map<String, String> map;
        try {
            map = getCategory();
        } catch (Exception e) {
            resultMap.put("status", "fail");
            resultMap.put("errorCode", "10086");
            resultMap.put("errmsg", "Yapi服务器访问出错，请检查配置文件内容，配置文件请至Settings -> Tools -> Yapi_ApiKey处填写");
            return resultMap;
        }
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
                .header("cookie", getCookie())
                .timeout(10 * 1000)
                .body(jsonBodyStr)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        if (0 == (int) object.get("errcode")) {
            resultMap.put("status", "success");
            resultMap.put("id", object.getJSONObject("data").getInteger("_id").toString());
        } else {
            resultMap.put("status", "fail");
            String id = getInterfaceId(projectMap.get(state.projectName), path, methodName);
            resultMap.put("errorCode", object.getInteger("errcode").toString());
            resultMap.put("errmsg", object.getString("errmsg"));
            resultMap.put("id", id);
            //  已存在的接口 40022
        }
        resultMap.put("catId", map.get(state.categoryName));
        resultMap.put("project_id", map.get(state.projectName));
        return resultMap;

    }

    public static String updateInterface(String id, String catId, String methodName,
                                         String path, String body, String returnResult,
                                         JSONArray params, List<JSONObject> pathVariable,String title) {
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
        if (ObjectUtil.isNotNull(pathVariable)) {
            jsonArray.addAll(pathVariable);
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
                .header("cookie", getCookie())
                .timeout(TIMEOUT)
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


    public static String getInterfacePath(String id) {
        // 获取接口名称
        Map<String, String> map = getInterface(id);
        String interfaceName = map.get("interfaceName");
        // 获取项目、类型名称
        Map<String, String> projectMap = getProject(map);
        return projectMap.get("projectName") + "\\" + projectMap.get("catName") + "\\" + interfaceName;
    }

    public static Map<String, String> getInterface(String id) {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/interface/get?id=" + id;
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(TIMEOUT)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        Map<String, String> map = new HashMap<>(3);
        map.put("interfaceName", object.getJSONObject("data").getString("title"));
        map.put("catId", object.getJSONObject("data").getInteger("catid").toString());
        map.put("projectId", object.getJSONObject("data").getInteger("project_id").toString());
        return map;
    }

    public static Map<String, String> getProject(Map<String, String> map) {
        CodeChronoSettings.State state = CodeChronoSettings.getInstance().getState();
        assert state != null;
        String url = state.url + "/api/project/get?id=" + map.get("projectId");
        String jsonStr = HttpRequest.get(url)
                .header("cookie", getCookie())
                .timeout(TIMEOUT)
                .execute()
                .body();
        JSONObject object = JSON.parseObject(jsonStr);
        Map<String, String> projectMap = new HashMap<>(2);
        projectMap.put("projectName",object.getJSONObject("data").getString("name"));
        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("cat");
        for (Object o : jsonArray) {
            if (((JSONObject) o).getInteger("_id").toString().equals(map.get("catId"))) {
                projectMap.put("catName", ((JSONObject) o).getString("name"));
            }
        }
        return projectMap;
    }
}
