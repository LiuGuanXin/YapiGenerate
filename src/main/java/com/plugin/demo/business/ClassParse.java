package com.plugin.demo.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;


import com.plugin.demo.model.FieldInfo;
import com.plugin.demo.model.FieldTypeInfo;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 读取光标所在的类信息
 *
 * @author Liu Guangxin
 * @date 2025/2/17 15:55
 */
public class ClassParse {
    private static final Logger log = LoggerFactory.getLogger(ClassParse.class);

    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_LIST = "array";
    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_NUMBER = "number";
    private static final String JSON_PREFIX = """
            {
            "message": "提示信息",\s
            "status": 200,\s
            "data": {}
            }""";

    private static final String PREFIX_SCHEMA = """
            {
                "$schema": "http://json-schema.org/draft-04/schema#",
                "type": "object",
                "properties": {
                    "status": {
                        "type": "number",
                        "description": "响应码"
                    },
                    "message": {
                        "type": "string",
                        "description": "提示信息"
                    },
                    "data": {
                        "type": "object",
                        "description": "返回记录",
                        "properties": {}
                    }
                }
            }
            """;


    private static final String PREFIX_SCHEMA_LIST = """
            {
                    "$schema": "http://json-schema.org/draft-04/schema#",
                    "type": "object",
                    "properties": {
                        "status": {
                            "type": "number",
                            "description": "响应码"
                        },
                        "message": {
                            "type": "string",
                            "description": "提示信息"
                        },
                        "data": {
                            "type": "array",
                            "description": "返回记录",
                            "items": {
                                "type": "object",
                                "properties": {}
                            }
                        }
                    }
                }
            """;


    private static final String PREFIX_SCHEMA_PAGE = """
			{
			  "type":"object",
			  "title":"empty object",
			  "properties":{
				"status":{
				  "type":"integer",
				  "description":"响应码"
				},
				"message":{
				  "type":"string",
				  "description":"提示信息"
				},
				"data":{
				  "type":"object",
				  "properties":{
					"records":{
					  "type":"array",
					  "items":{
						"type":"object",
						"properties":{ },
						"description":"空数据对象"
					  },
					  "description":"分页数据"
					},
					"total":{
					  "type":"integer",
					  "description":"总数据量"
					},
					"current":{
					  "type":"integer",
					  "description":"当前页"
					},
					"size":{
					  "type":"integer",
					  "description":"页面数据量"
					}
				  },
				  "description":"数据"
				}
			  }
			}
            """;


    /**
     * 根据传入的 AnActionEvent 对象和布尔值，获取一个包含字符串键和 JSONObject 值的映射。
     *
     * @param e      触发此方法的 AnActionEvent 对象，通常用于获取当前上下文信息。
     * @param isList 布尔值，指示是否返回列表形式的 JSON 对象。
     * @return 一个包含字符串键和 JSONObject 值的映射，其中每个 JSONObject 代表一个字段信息。
     */
    public static String getJsonResult(AnActionEvent e, Boolean isList, Boolean getMockData) {
        List<FieldInfo> fieldInfos = ClassParse.parseFieldsWithComments(e);
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, JSONObject> map = dealPrefix(jsonObject, isList, getMockData);
        if (map.get("exception") != null) {
            return map.get("exception").getString("exception");
        }
        return "返回结果的json格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(map.get("json"), SerializerFeature.PrettyFormat) +
                "\n```\n" +
                "返回元数据格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(map.get("schema"), SerializerFeature.PrettyFormat) +
                "\n```\n";
    }


    /**
     * 根据传入的 AnActionEvent 对象和布尔值，获取一个包含字符串键和 JSONObject 值的映射。
     *
     * @param e      触发此方法的 AnActionEvent 对象，通常用于获取当前上下文信息。
     * @return 一个包含字符串键和 JSONObject 值的映射，其中每个 JSONObject 代表一个字段信息。
     */
    public static String getRequestBody(AnActionEvent e, Boolean getMockData) {
        List<FieldInfo> fieldInfos = ClassParse.parseFieldsWithComments(e);
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, JSONObject> map = dealPrefix(jsonObject, false, getMockData);
        if (map.get("exception") != null) {
            return map.get("exception").getString("exception");
        }
        JSONObject schema = map.get("schema");
        JSONObject result = schema.getJSONObject("properties")
                .getJSONObject("data");
        result.put("description", "根节点");
        return "返回元数据格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(result, SerializerFeature.PrettyFormat) +
                "\n```\n";
    }


    public static String getRequestBody(List<FieldInfo> fieldInfos, Boolean isList, Boolean getMockData) {
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, JSONObject> map = dealPrefix(jsonObject, isList, getMockData);
        if (map.get("exception") != null) {
            return map.get("exception").getString("exception");
        }
        JSONObject schema = map.get("schema");
        JSONObject result = schema.getJSONObject("properties")
                .getJSONObject("data");
        result.put("description", "根节点");
        return "返回元数据格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(result, SerializerFeature.PrettyFormat) +
                "\n```\n";
    }

    public static String getRequestBodyMeta(List<FieldInfo> fieldInfos, Boolean isList, Boolean getMockData) {
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, JSONObject> map = dealPrefix(jsonObject, isList, getMockData);
        if (map.get("exception") != null) {
            return map.get("exception").getString("exception");
        }
        JSONObject schema = map.get("schema");
        JSONObject result = schema.getJSONObject("properties")
                .getJSONObject("data");
        result.put("description", "根节点");
        return JSON.toJSONString(result, SerializerFeature.PrettyFormat);
    }

    /**
     * 处理前缀信息，将字段信息转换为json格式
     * @param dataJson 字段信息json格式
     * @param isList 布尔值，指示是否返回列表形式的 JSON 对象。
     * @return json格式字符串
     */
    public static Map<String, JSONObject> dealPrefix(JSONObject dataJson, Boolean isList, Boolean getMockData) {
        Map<String, Object> map = parseDict(dataJson);

        JSONObject json = JSON.parseObject(JSON_PREFIX);
        json.put("data", JSON.toJSON(map.get("commentDict")));
        JSONObject schema;
        if (Boolean.TRUE.equals(isList)) {
            schema = JSON.parseObject(PREFIX_SCHEMA_LIST);
            schema.getJSONObject("properties")
                    .getJSONObject("data")
                    .getJSONObject("items")
                    .put("properties", JSON.toJSON(map.get("typeDict")));
        } else {
            schema = JSON.parseObject(PREFIX_SCHEMA);
            schema.getJSONObject("properties")
                    .getJSONObject("data")
                    .put("properties", JSON.toJSON(map.get("typeDict")));
        }
        Map<String, JSONObject> rs = new HashMap<>(2);
        rs.put("json", json);
        if (Boolean.TRUE.equals(getMockData)) {
            JSONObject mockData = generateMockData(schema, isList);
            if (mockData.getString("exception") != null) {
                rs.put("exception", mockData);
            }
            System.out.println(JSON.toJSONString(mockData, SerializerFeature.PrettyFormat));
            rs.put("schema", mockData);
        } else {
            rs.put("schema", schema);
        }
        return rs;
    }


    public static String getJsonResult(List<FieldInfo> fieldInfos, String type, Boolean getMockData) {
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, JSONObject> map = dealPrefix(jsonObject, type, getMockData);
        if (map.get("exception") != null) {
            return map.get("exception").getString("exception");
        }
        return "返回结果的json格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(map.get("json"), SerializerFeature.PrettyFormat) +
                "\n```\n" +
                "返回元数据格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(map.get("schema"), SerializerFeature.PrettyFormat) +
                "\n```\n";
    }

    public static String getJsonResultMeta(List<FieldInfo> fieldInfos, String type, Boolean getMockData) {
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, JSONObject> map = dealPrefix(jsonObject, type, getMockData);
        if (map.get("exception") != null) {
            return map.get("exception").getString("exception");
        }
        return JSON.toJSONString(map.get("schema"), SerializerFeature.PrettyFormat);
    }


    public static Map<String, JSONObject> dealPrefix(JSONObject dataJson, String type, Boolean getMockData) {
        Map<String, Object> map = parseDict(dataJson);

        JSONObject json = JSON.parseObject(JSON_PREFIX);
        json.put("data", JSON.toJSON(map.get("commentDict")));
        JSONObject schema;
        if ("List".equals(type)) {
            schema = JSON.parseObject(PREFIX_SCHEMA_LIST);
            schema.getJSONObject("properties")
                    .getJSONObject("data")
                    .getJSONObject("items")
                    .put("properties", JSON.toJSON(map.get("typeDict")));
        } else if ("Page".equals(type)) {
            schema = JSON.parseObject(PREFIX_SCHEMA_PAGE);
            schema.getJSONObject("properties")
                    .getJSONObject("data")
                    .getJSONObject("properties")
                    .getJSONObject("records")
                    .getJSONObject("items")
                    .put("properties", JSON.toJSON(map.get("typeDict")));
        }else {
            schema = JSON.parseObject(PREFIX_SCHEMA);
            schema.getJSONObject("properties")
                    .getJSONObject("data")
                    .put("properties", JSON.toJSON(map.get("typeDict")));
        }
        Map<String, JSONObject> rs = new HashMap<>(2);
        rs.put("json", json);
        if (Boolean.TRUE.equals(getMockData)) {
            JSONObject mockData = generateMockData(schema, type);
            if (mockData.getString("exception") != null) {
                rs.put("exception", mockData);
            }
            rs.put("schema", mockData);
        } else {
            rs.put("schema", schema);
        }
        return rs;
    }

    private static JSONObject generateMockData(JSONObject jsonSchema, String type) {
        String convertData = schema2jsonTypeAndDesc(jsonSchema);
        if (isJson(convertData)) {
            return new JSONObject();
        }
        JSONObject object = new JSONObject();
        String mock = GenerateMockData.generateMock(convertData);
        if ("1".equals(mock)) {
            object.put("exception", "apikey为空，请填写智谱apikey");
            return object;
        } else if ("2".equals(mock)) {
            object.put("exception", "apikey错误，请检查");
            return object;
        }
        mock = mock.replaceAll("```json", "");
        mock = mock.replaceAll("```", "");
        if (isJson(mock)) {
            return new JSONObject();
        }
        jsonSchemaConverter(jsonSchema, mockToMap(mock, type), "");
        return jsonSchema;
    }

    private static Map<String, Object> mockToMap(String mockJson, String type) {
        JSONObject jsonObject = JSON.parseObject(mockJson);
        if ("List".equals(type)) {
            JSONArray data = jsonObject.getJSONArray("data");
            jsonObject.put("data", data.getJSONObject(0));
        } if ("Page".equals(type)) {
            JSONArray data = jsonObject.getJSONObject("data").getJSONArray("records");
            jsonObject.getJSONObject("data").put("records", data.getJSONObject(0));
        }
        Map<String, Object> resultMap = new HashMap<>(10);
        flattenJson(jsonObject, "", resultMap);
        return resultMap;
    }


    /**
     * 将字段信息转换为json格式
     *
     * @param fieldInfos 字段信息
     * @return json格式字符串
     */
    public static JSONObject transToJson(List<FieldInfo> fieldInfos) {
        JSONObject jsonObject = new JSONObject();
        for (FieldInfo fieldInfo : fieldInfos) {
            JSONObject field = new JSONObject();
            field.put("type", fieldInfo.getType());
            field.put("description", fieldInfo.getDescription());
            // 当前对象是列表对象
            if (fieldInfo.getProperties() != null && !fieldInfo.getProperties().isEmpty()) {
                if (TYPE_LIST.equals(fieldInfo.getType())) {
                    JSONObject item = new JSONObject();
                    item.put("type", TYPE_OBJECT);
                    item.put("description", fieldInfo.getDescription());
                    item.put("properties", transToJson(fieldInfo.getProperties()));
                    field.put("items", item);
                } else if (TYPE_OBJECT.equals(fieldInfo.getType())) {
                    field.put("properties", transToJson(fieldInfo.getProperties()));
                }

            }
            jsonObject.put(fieldInfo.getName(), field);
        }
        return jsonObject;
    }


    /**
     * 将字段类型转换为json schema的类型
     * @param dataJson 字段信息json格式
     * @return json格式字符串
     */
    public static Map<String, Object> parseDict(JSONObject dataJson) {
        if (dataJson == null) {
            return null;
        }
        Map<String, Object> commentDict = new HashMap<>(2);
        Map<String, Object> typeDict = new HashMap<>(2);

        for (String key : dataJson.keySet()) {
            JSONObject value = dataJson.getJSONObject(key);

            String type = value.getString("type");
            String description = value.getString("description");

            if (TYPE_OBJECT.equals(type)) {
                Map<String, Object> subTypeDict;
                Map<String, Object> subCommentDict;
                Map<String, Object> result = parseDict(value.getJSONObject("properties"));
                if (result != null) {
                    subTypeDict = (Map<String, Object>) result.get("typeDict");
                    subCommentDict = (Map<String, Object>) result.get("commentDict");
                    // 使用 FieldTypeInfo 类来构造字段信息
                    typeDict.put(key, new FieldTypeInfo(TYPE_OBJECT, description, subTypeDict));
                    commentDict.put(key, JSON.toJSON(subCommentDict));
                }
            } else if (TYPE_LIST.equals(type)) {
                JSONObject items = value.getJSONObject("items");
                String itemType = items.getString("type");

                if (TYPE_OBJECT.equals(itemType)) {
                    Map<String, Object> subTypeDict = new HashMap<>(2);
                    Map<String, String> subCommentDict  = new HashMap<>(2);
                    Map<String, Object> result = parseDict(items.getJSONObject("properties"));
                    if (result != null) {
                        subTypeDict = (Map<String, Object>) result.get("typeDict");
                        subCommentDict = (Map<String, String>) result.get("commentDict");
                    }
                    Map<String, Object> properties = new HashMap<>(3);
                    properties.put("type", TYPE_OBJECT);
                    properties.put("description", description);
                    properties.put("properties", subTypeDict);
                    Map<String, Object> map = new HashMap<>(2);
                    map.put("type", TYPE_LIST);
                    map.put("items", properties);
                    typeDict.put(key, map);


                    List<Object> list = new ArrayList<>();
                    list.add(subCommentDict);
                    commentDict.put(key, list);
                } else {
                    typeDict.put(key, new FieldTypeInfo("array", description, new HashMap<>(3) {{
                        put("type", typeTransform(items.getString("type")));
                        put("description", description);
                    }}));
                    commentDict.put(key, description);
                }
            } else {
                typeDict.put(key, new FieldTypeInfo(typeTransform(type), description, null));
                commentDict.put(key, description);
            }
        }

        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("typeDict", typeDict);
        resultMap.put("commentDict", commentDict);
        return resultMap;
    }



    /**
     * 类型转换
     *
     * @param typeStr 原始类型字符串
     * @return 转换后的类型字符串
     */
    public static String typeTransform(String typeStr) {
        return switch (typeStr) {
            case "int", "Integer", "Long", "long" -> TYPE_INTEGER;
            case "String", "char", "Character" -> TYPE_STRING;
            case "float", "double", "Float", "Double" -> TYPE_NUMBER;
            default -> typeStr;
        };
    }


    /**
     * 读取光标所在的类信息
     *
     * @param e 事件对象，包含当前编辑器、项目等信息
     */
    public static void readCursorClass(AnActionEvent e) {
        // 获取光标所在的类（如果有）
        PsiClass psiClass = getProjectByAnActionEvent(e);
        if (psiClass != null) {
            String packageName = psiClass.getQualifiedName();
            log.info("Package Name: {}", packageName);
            PsiField[] fields = psiClass.getAllFields();
            for (PsiField field : fields) {
                log.info("Field Type: {}", field.getType());
                log.info("Field Name: {}", field.getName());
                log.info("Field NameIdentifier: {}", field.getNameIdentifier());
                log.info("Field Initializer: {}", field.getInitializer());
                log.info("Field ContainingClass: {}", field.getContainingClass());
            }
            PsiMethod[] psiMethods = psiClass.getAllMethods();
            for (PsiMethod method : psiMethods) {
                log.info("Method Name: {}", method.getName());
                log.info("Method NameIdentifier: {}", method.getNameIdentifier());
                log.info("Method ReturnType: {}", method.getReturnType());
                log.info("Method ParameterList: {}", method.getParameterList());
                log.info("Method ThrowsList: {}", method.getThrowsList());
                log.info("Method Body: {}", method.getBody());
            }
        } else {
            log.info("The cursor is not inside a class.");
        }
    }


    /**
     * 获取光标所在的类（如果有）
     *
     * @param e 事件对象，包含当前编辑器、项目等信息
     * @return 光标所在的类（如果有）或 null 如果没有找到对应的 PsiClass 对象。
     */
    public static PsiClass getProjectByAnActionEvent(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return null;
        }
        // 获取当前打开的编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }
        // 获取光标所在的位置
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int offset = caret.getOffset();

        // 获取光标所在的文件
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return null;
        }
        // 获取光标位置对应的 PsiElement
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        if (elementAtCaret == null) {
            return null;
        }
        // 获取光标所在的类（如果有）
        return PsiTreeUtil.getParentOfType(elementAtCaret, PsiClass.class);
    }

    public static List<FieldInfo> parseFieldsWithComments(AnActionEvent e) {
        PsiClass psiClass = getProjectByAnActionEvent(e);
        if (psiClass == null) {
            return new ArrayList<>();
        }
        return parseFieldsWithComments(psiClass);
    }


    /**
     * 解析类中的所有字段及其注释，并返回一个包含字段信息的列表。
     *
     * @param psiClass 需要解析的PsiClass对象
     * @return 包含字段信息的列表，如果psiClass为null，则返回一个空列表
     */
    public static List<FieldInfo> parseFieldsWithComments(PsiClass psiClass) {
        if (psiClass == null) {
            return new ArrayList<>();
        }
        List<FieldInfo> fieldInfos = new ArrayList<>();
        // 1. 遍历类的所有字段
        for (PsiField field : psiClass.getFields()) {
            // 获取字段名称
            String fieldName = field.getName();
            // 排除序列化属性
            if ("serialVersionUID".equals(fieldName)) {
                continue;
            }
            // 获取字段类型
            PsiType fieldType = field.getType();
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.setName(fieldName);
            fieldInfo.setType(fieldType.getPresentableText());
            fieldInfo.setDescription(getCommentText(field));
            // 处理对象类型
            if (isCustomType(fieldType)) {
                // 对象类型解析
                PsiClassType classType = (PsiClassType) fieldType;
                PsiClass psiClassType = classType.resolve();
                if (psiClassType != null && psiClassType != psiClass) {
                    fieldInfo.setType("object");
                    fieldInfo.setProperties(parseFieldsWithComments(psiClassType));
                }
            }
            // 处理对象列表类型
            if (fieldType.getPresentableText().startsWith("List<")) {
                // 列表类型解析
                log.info(" 对象类型: {}", psiClass.getQualifiedName());
                // 泛型参数解析
                PsiClassType classType = (PsiClassType) fieldType;
                PsiType[] generics = classType.getParameters();
                PsiClass genericClass = ((PsiClassType) generics[0]).resolve();
                if (genericClass != null && isCustomType(generics[0]) && genericClass != psiClass) {
                    fieldInfo.setType("array");
                    fieldInfo.setProperties(parseFieldsWithComments(genericClass));
                }
            }
            fieldInfos.add(fieldInfo);
        }
        return fieldInfos;
    }

    /**
     * 获取字段注释文本
     * @param field 字段对象
     * @return 注释文本，如果没有则返回空字符串
     */
    public static String getCommentText(PsiField field) {
        // 3. 获取 JavaDoc 注释（结构化文档）
        PsiDocComment docComment = field.getDocComment();
        String javaDocText = (docComment != null) ?
                docComment.getText().replace("/**", "").replace("*/", "").replace("*", "").trim() : "";

        // 4. 获取代码旁的行/块注释（非 JavaDoc）
        List<PsiComment> inlineComments = new ArrayList<>();
        PsiElement prevSibling = field.getPrevSibling();
        while (prevSibling instanceof PsiComment || prevSibling instanceof PsiWhiteSpace) {
            if (prevSibling instanceof PsiComment) {
                inlineComments.add((PsiComment) prevSibling);
            }
            prevSibling = prevSibling.getPrevSibling();
        }
        if (!javaDocText.isEmpty()) {
            return javaDocText;
        } else if (!inlineComments.isEmpty()) {
            return inlineComments.stream().map(PsiComment::getText).collect(Collectors.joining("  | "));
        }
        return "";
    }

    /**
     * 获取方法注释文本
     * @param psiMethod 方法对象
     * @return 注释文本，如果没有则返回空字符串
     */
    public static String getCommentText(PsiMethod psiMethod) {
        // 3. 获取 JavaDoc 注释（结构化文档）
        PsiDocComment docComment = psiMethod.getDocComment();
        String javaDocText = (docComment != null) ?
                docComment.getText().replace("/**", "")
                        .replace("*/", "").replace("*", "").trim() : "";
        javaDocText = javaDocText.replace(" ", "").replace("\n", "");
        if (!javaDocText.isEmpty()) {
            if (javaDocText.contains("@")) {
                javaDocText = javaDocText.substring(0, javaDocText.indexOf('@'));
            }
            return javaDocText;
        }
        return "默认的接口名称";
    }

    /**
     * 判断是否为自定义类型
     * @param classType 字段类型
     * @return 是否为自定义类型
     */
    public static boolean isCustomType(PsiClassType classType) {
        // 获取对应的 PsiClass
        PsiClass psiClass = classType.resolve();
        if (psiClass != null) {
            // 判断类是否属于标准库（例如 java.lang 包）
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName != null) {
                // 判断是否为自定义类型，通常排除标准库类型
                String javaPrefix = "java.";
                String javaxPrefix = "javax.";
                String orgApachePrefix = "org.apache.";
                String comIntellijPrefix = "com.intellij.";
                // 认为是自定义类型
                return !qualifiedName.startsWith(javaPrefix) && !qualifiedName.startsWith(javaxPrefix) &&
                        !qualifiedName.startsWith(orgApachePrefix) && !qualifiedName.startsWith(comIntellijPrefix);
            }
        }
        return false;
    }

    /**
     * 判断是否为自定义类型
     * @param type 字段类型
     * @return 是否为自定义类型
     */
    public static boolean isCustomType(PsiType type) {
        // 判断类型是否为类类型 (PsiClassType)
        if (type instanceof PsiClassType classType) {
            return isCustomType(classType);
        }
        // 不是自定义类型
        return false;
    }

    private static JSONObject generateMockData(JSONObject jsonSchema, Boolean isList) {
        String convertData = schema2jsonTypeAndDesc(jsonSchema);
        if (isJson(convertData)) {
            return new JSONObject();
        }
        JSONObject object = new JSONObject();
        String mock = GenerateMockData.generateMock(convertData);
        if ("1".equals(mock)) {
            object.put("exception", "apikey为空，请填写智谱apikey");
            return object;
        } else if ("2".equals(mock)) {
            object.put("exception", "apikey错误，请检查");
            return object;
        }
        mock = mock.replaceAll("```json", "");
        mock = mock.replaceAll("```", "");
        if (isJson(mock)) {
            return new JSONObject();
        }
        jsonSchemaConverter(jsonSchema, mockToMap(mock, isList), "");
        return jsonSchema;
    }

    private static Map<String, Object> mockToMap(String mockJson, Boolean isList) {
        JSONObject jsonObject = JSON.parseObject(mockJson);
        if (Boolean.TRUE.equals(isList)) {
            JSONArray data = jsonObject.getJSONArray("data");
            jsonObject.put("data", data.getJSONObject(0));
        }
        Map<String, Object> resultMap = new HashMap<>(10);
        flattenJson(jsonObject, "", resultMap);
        return resultMap;
    }

    /**
     * 将嵌套的JSON对象扁平化并存储到Map中。
     *
     * @param jsonObject JSON对象
     * @param prefix 当前节点的前缀，用于构建键名
     * @param resultMap 存储扁平化后的键值对的Map
     */
    private static void flattenJson(JSONObject jsonObject, String prefix, Map<String, Object> resultMap) {
        if (jsonObject == null || resultMap == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value == null) {
                resultMap.put(newKey, null);
            } else if (value instanceof JSONObject) {
                flattenJson((JSONObject) value, newKey, resultMap);
            } else if (value instanceof JSONArray array) {
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        flattenJson((JSONObject) item, newKey + "[" + i + "]", resultMap);
                    } else {
                        resultMap.put(newKey + "[" + i + "]", item);
                    }
                }
            } else {
                resultMap.put(newKey, value);
            }
        }
    }


    /**
     * 数据转换
     * @param schema schema json
     * @param mockMap mock json
     */
    public static void jsonSchemaConverter(JSONObject schema, Map<String, Object> mockMap, String prefix) {
        if (schema == null || mockMap == null) {
            return;
        }
        JSONObject properties = schema.getJSONObject("properties");
        if (properties == null) {
            return;
        }
        for (String key : properties.keySet()) {
            String prefixNext = prefix.isEmpty() ? key : prefix + "." + key;
            JSONObject property = properties.getJSONObject(key);
            if (mockMap.containsKey(prefixNext)) {
                JSONObject mock = new JSONObject();
                mock.put("mock", mockMap.get(prefixNext));
                property.put("mock", mock);
            }
            if ("object".equals(property.getString("type"))) {
                jsonSchemaConverter(property, mockMap, prefixNext);
            } else if ("array".equals(property.getString("type"))) {
                jsonSchemaConverter(property.getJSONObject("items"), mockMap, prefixNext);
            }
            properties.put(key, property);
        }
        schema.put("properties", properties);
    }

    /**
     * 判断给定的字符串是否为非法的JSON格式。
     *
     * @param str 待判断的字符串
     * @return 如果字符串不是合法的JSON格式，则返回true；否则返回false。
     * @throws JSONException 如果在解析过程中遇到JSON格式错误，将抛出此异常。
     */
    public static boolean isJson(String str) {
        try {
            // 尝试解析整个字符串
            JSON.parse(str);
            return false;
        } catch (JSONException e) {
            return true;
        }
    }

    /**
     * 将 JSON Schema 转换为 JSON 字符串
     *
     * @param schemaJson 包含 Schema 信息的 JSONObject 对象
     * @return 转换后的 JSON 字符串
     */
    private static String schema2jsonTypeAndDesc(JSONObject schemaJson) {
        JSONObject object = generateJsonDataTypeAndDesc(schemaJson);
        if (object.isEmpty()) {
            return "{}";
        }
        return object.toJSONString();
    }

    /**
     * 根据 JSON Schema 生成 JSON 数据
     *
     * @param schema 包含 Schema 信息的 JSONObject 对象
     * @return 根据 Schema 生成的 JSON 数据
     */
    private static JSONObject generateJsonDataTypeAndDesc(JSONObject schema) {
        JSONObject result = new JSONObject();
        JSONObject properties = schema.getJSONObject("properties");
        if (properties == null) {
            return result;
        }
        for (String key : properties.keySet()) {
            JSONObject property = properties.getJSONObject(key);
            String type = property.getString("type");
            result.put(key, StringUtils.isNotEmpty(type) ? type : "");
            String desc = property.getString("description");
            result.put(key, StringUtils.isNotEmpty(desc) ? desc : "");
            JSONObject values = new JSONObject();
            values.put("type", StringUtils.isNotEmpty(type) ? type : "");
            values.put("description", StringUtils.isNotEmpty(desc) ? desc : "");
            result.put(key, values);
            if ("object".equals(property.getString("type"))) {
                result.put(key, generateJsonDataTypeAndDesc(property));
            } else if ("array".equals(property.getString("type"))) {
                JSONArray array = new JSONArray();
                JSONObject itemSchema = property.getJSONObject("items");
                array.add(generateJsonDataTypeAndDesc(itemSchema));
                result.put(key, array);
            }
        }
        return result;
    }
}
