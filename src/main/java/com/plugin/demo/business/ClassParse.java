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
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiTreeUtil;


import com.plugin.demo.model.FieldInfo;
import com.plugin.demo.model.FieldTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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


    private static final String META_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-04/schema#",
              "type": "object"
            }
    """;

    public static final String META_SCHEMA_LIST = """
            {
              "$schema": "http://json-schema.org/draft-04/schema#",
              "type": "array",
              "items": {
                "type": "object",
              }
            }
    """;


    /**
     * 根据传入的 AnActionEvent 对象和布尔值，获取一个包含字符串键和 JSONObject 值的映射。
     *
     * @param e      触发此方法的 AnActionEvent 对象，通常用于获取当前上下文信息。
     * @param type   指示是否返回列表形式的 JSON 对象。
     * @return 一个包含字符串键和 JSONObject 值的映射，其中每个 JSONObject 代表一个字段信息。
     */
    public static String getJsonResult(AnActionEvent e, String type, Boolean getMockData) {
        List<FieldInfo> fieldInfos = ClassParse.parseFieldsWithComments(e);
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, Object> map = parseDict(jsonObject);
        return "返回结果的json格式如下：\n" +
                "```json\n" +
                JSON.toJSONString(map.get("commentDict"), SerializerFeature.PrettyFormat) +
                "\n```\n" +
                "返回元数据格式如下：\n" +
                "```json\n" +
                getJsonResultMeta(fieldInfos, type, getMockData) +
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
        return "返回元数据格式如下：\n" +
                "```json\n" +
                getRequestBodyMeta(fieldInfos, "Object", getMockData) +
                "\n```\n";
    }

    public static String getRequestBodyMeta(List<FieldInfo> fieldInfos, String type, Boolean getMockData) {
        JSONObject jsonObject = transToJson(fieldInfos);
        Map<String, Object> map = parseDict(jsonObject);
        JSONObject schema;
        if ("List".equals(type)) {
            schema = JSON.parseObject(META_SCHEMA_LIST);
            schema.getJSONObject("items")
                    .put("properties", JSON.toJSON(map.get("typeDict")));
        } else {
            schema = JSON.parseObject(META_SCHEMA);
            schema.put("properties", JSON.toJSON(map.get("typeDict")));
        }
        if (Boolean.TRUE.equals(getMockData)) {
            schema = generateMockData(schema);
        }
        return JSON.toJSONString(schema, SerializerFeature.PrettyFormat);
    }


    public static String getJsonResultMeta(List<FieldInfo> fieldInfos, String type, Boolean getMockData) {
        JSONObject jsonObject = transToJson(fieldInfos);
        dealDljdInfo(jsonObject);
        Map<String, Object> map = parseDict(jsonObject);
        JSONObject schema;
        if ("Object".equals(type)) {
            schema = JSON.parseObject(META_SCHEMA);
            schema.put("properties", JSON.toJSON(map.get("typeDict")));
        } else  if ("List".equals(type)) {
            schema = JSON.parseObject(META_SCHEMA_LIST);
            schema.getJSONObject("items").put("properties", JSON.toJSON(map.get("typeDict")));
        } else {
            schema = (JSONObject) JSON.toJSON(map.get("typeDict"));
        }
        if (Boolean.TRUE.equals(getMockData)) {
            schema = generateMockData(schema);
        }
        return JSON.toJSONString(schema, SerializerFeature.PrettyFormat);
    }



    private static void dealDljdInfo(JSONObject jsonObject) {
        if (jsonObject.getJSONObject("status") != null) {
            jsonObject.getJSONObject("status").put("description", "响应码");
        }
        if (jsonObject.getJSONObject("message") != null) {
            jsonObject.getJSONObject("message").put("description", "提示信息");

        }
        if (jsonObject.getJSONObject("data") != null
                && jsonObject.getJSONObject("data").getJSONObject("properties") != null ) {
            jsonObject.getJSONObject("data").put("description", "返回记录");
            if (jsonObject.getJSONObject("data").getJSONObject("properties").getJSONObject("total") != null
                    && jsonObject.getJSONObject("data").getJSONObject("properties").getJSONObject("current") != null
                    && jsonObject.getJSONObject("data").getJSONObject("properties").getJSONObject("size") != null ) {
                jsonObject.getJSONObject("data").getJSONObject("properties").getJSONObject("total").put("description", "总数");
                jsonObject.getJSONObject("data").getJSONObject("properties").getJSONObject("current").put("description", "当前页");
                jsonObject.getJSONObject("data").getJSONObject("properties").getJSONObject("size").put("description", "数量");

            }
        }
    }


    private static JSONObject generateMockData(JSONObject jsonSchema) {
        String mock = GenerateMockData.generateMock(JSON.toJSONString(jsonSchema));
        JSONObject object = new JSONObject();
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
            return object;
        }
        object = JSON.parseObject(mock);
        return object;
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
     * 解析类中的所有字段及其注释，并返回一个包含字段信息的列表。
     *
     * @param psiClass 需要解析的psiClass对象
	 * @param substitutor 需要解析的substitutor对象
     * @return 包含字段信息的列表，如果psiClass为null，则返回一个空列表
     */
	public static List<FieldInfo> parseFieldsWithComments(PsiClass psiClass, PsiSubstitutor substitutor) {
		if (psiClass == null) {
			return new ArrayList<>();
		}
		List<FieldInfo> fieldInfos = new ArrayList<>();

		for (PsiField field : psiClass.getFields()) {
			String fieldName = field.getName();
			if ("serialVersionUID".equals(fieldName)) {
				continue;
			}

			PsiType fieldType = field.getType();

			// 如果字段是泛型类型 T/U 等，替换为实际类型
			if (substitutor != null) {
				PsiType substituted = substitutor.substitute(fieldType);
				if (substituted != null && substituted != fieldType) {
					fieldType = substituted;
				}
			}

			FieldInfo fieldInfo = new FieldInfo();
			fieldInfo.setName(fieldName);
			fieldInfo.setType(fieldType.getPresentableText());
			fieldInfo.setDescription(getCommentText(field));

			// 如果字段是自定义对象类型
			if (isCustomType(fieldType) && fieldType instanceof PsiClassType) {
				PsiClassType classType = (PsiClassType) fieldType;
				PsiClass psiClassType = classType.resolve();
				PsiSubstitutor nestedSubstitutor = classType.resolveGenerics().getSubstitutor();

				if (psiClassType != null && psiClassType != psiClass) {
					fieldInfo.setType("object");
					fieldInfo.setProperties(parseFieldsWithComments(psiClassType, nestedSubstitutor));
				}
			}

			// 如果字段是 List<T> 这种类型
			if (fieldType instanceof PsiClassType) {
				PsiClassType classType = (PsiClassType) fieldType;
				PsiClass resolvedClass = classType.resolve();
				if (resolvedClass != null && "java.util.List".equals(resolvedClass.getQualifiedName())) {
					PsiType[] generics = classType.getParameters();
					if (generics.length > 0 && generics[0] instanceof PsiClassType) {
						PsiClass genericClass = ((PsiClassType) generics[0]).resolve();
						if (genericClass != null && isCustomType(generics[0]) && genericClass != psiClass) {
							fieldInfo.setType("array");

							PsiSubstitutor nestedSubstitutor = ((PsiClassType) generics[0]).resolveGenerics().getSubstitutor();
							fieldInfo.setProperties(parseFieldsWithComments(genericClass, nestedSubstitutor));
						}
					}
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
            if (!javaDocText.isEmpty()) {
                return javaDocText;
            }
        }
        return psiMethod.getName();
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

    public static Map<String, Object> getParams(PsiMethod psiMethod, Map<String, String> methodMap, Boolean mock) {
		Map<String, Object> map = new HashMap<>(2);
		// 获取参数  如果仅有一个参数且该参数为自定义对象，则需要生成请求体参数
		PsiParameter[] parameter = psiMethod.getParameterList().getParameters();
		String body = "";
		// 请求体的请求
		if (!isGetMapping(psiMethod)) {
			for (PsiParameter psiParameter: parameter) {
				if (ClassParse.isCustomType(psiParameter.getType())) {
                    PsiType fieldType = psiParameter.getType();
                    PsiClassType classType = (PsiClassType) fieldType;
					body = getBody(classType, mock);
				}
			}
		}
		// 获取 注释中的参数备注
		Map<String, String> paramComments = getParamComments(psiMethod);
		List<JSONObject> pathVariable = new ArrayList<>();
        if (methodMap.get("path") != null) {
            List<String> pathParams = extractPathParams(methodMap.get("path"));
            if (!pathParams.isEmpty()) {
                // 处理路径参数
                for (String param : pathParams) {
                    JSONObject object = new JSONObject();
                    object.put("name", param);
                    object.put("desc", paramComments.getOrDefault(param, ""));
                    pathVariable.add(object);
                }
                map.put("pathVariable", pathVariable);

            }
        }

		if (isGetMapping(psiMethod)) {
			JSONArray params = getRequestParam(parameter, paramComments, mock);
			map.put("params", params);
		}
        if (!body.isEmpty()) {
			map.put("body", body);
        }
        // 获取返回值
        if (psiMethod.getReturnType() != null) {
			PsiClassType classType = (PsiClassType) psiMethod.getReturnType();
			PsiClass psiClassType = classType.resolve();
			if (psiClassType == null) {
				return map;
			}
			String resultResult = getReturn(classType, psiClassType, mock);
            if (!resultResult.isEmpty()) {
				map.put("return", resultResult);
            }
        }
        return map;
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

	private static JSONArray getRequestParam(PsiParameter[] parameter, Map<String, String> paramComments, Boolean mock) {
		JSONArray jsonArray = new JSONArray();
		outer:
		for (PsiParameter psiParameter: parameter) {
			// 1. 排除注解为路径参数的参数
			PsiAnnotation[] annotations = psiParameter.getAnnotations();
			for (PsiAnnotation annotation : annotations) {
				if (Objects.requireNonNull(annotation.getQualifiedName()).contains("PathVariable")) {
					continue outer;
				}
			}
			// 2. 如果是基本类型，直接添加
			if (!ClassParse.isCustomType(psiParameter.getType())) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", psiParameter.getName());
				jsonObject.put("required", "1");
				jsonObject.put("example", "");
				jsonObject.put("desc", paramComments.getOrDefault(psiParameter.getName(), ""));
				jsonArray.add(jsonObject);
			} else if (psiParameter.getType().getPresentableText().startsWith("Page<")) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", "current");
				jsonObject.put("required", "0");
				jsonObject.put("example", "1");
				jsonObject.put("desc", "当前页");
				jsonArray.add(jsonObject);
				JSONObject jsonObject1 = new JSONObject();
				jsonObject1.put("name", "size");
				jsonObject1.put("required", "0");
				jsonObject1.put("example", "10");
				jsonObject1.put("desc", "每页数量");
				jsonArray.add(jsonObject1);
			} else if (ClassParse.isCustomType(psiParameter.getType())) {
				// 3. 如果是自定义对象类型， 解析后添加
				// 1. 遍历类的所有字段
				PsiClassType  psiClassType = (PsiClassType) psiParameter.getType();
				PsiClass psiClass = psiClassType.resolve();
				List<FieldInfo> fieldInfos = ClassParse.parseFieldsWithComments(psiClass);
				for (FieldInfo fieldInfo : fieldInfos) {
					// 不处理对象类型和列表类型
					if (!"object".equals(fieldInfo.getType())
							&& !"array".equals(fieldInfo.getType())) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("name", fieldInfo.getName());
						jsonObject.put("required", "0");
						jsonObject.put("example", "");
						jsonObject.put("desc", fieldInfo.getDescription());
						jsonArray.add(jsonObject);
					}
				}
			}
		}
		if (mock) {
			// 生成描述
			Map<String, String> nameMap = new HashMap<>(10);
			for (Object o : jsonArray) {
				JSONObject object = (JSONObject) o;
				if ((object.getString("desc")).isEmpty()) {
					nameMap.put(object.getString("name"), "");
				}
			}
			// 生成mock数据
			String result = GenerateMockData.generateDesc(JSON.toJSONString(nameMap));
			result = result.replace("```json", "");
			result = result.replace("```", "");
			JSONObject jsonObject = JSON.parseObject(result);
			Map<String, String> descMap = new HashMap<>(10);
			for (Object o : jsonArray) {
				JSONObject object = (JSONObject) o;
				if ((object.getString("desc")).isEmpty()) {
					((JSONObject) o).put("desc", jsonObject.getString(object.getString("name")));
				}
				descMap.put(object.getString("name"), object.getString("desc"));
			}
			String mockData = GenerateMockData.generateRequestParamsMock(JSON.toJSONString(descMap));
			mockData = mockData.replace("```json", "");
			mockData = mockData.replace("```", "");
			JSONObject mockJsonObject = JSON.parseObject(mockData);
			for (Object o : jsonArray) {
				((JSONObject) o).put("example", mockJsonObject.getString(((JSONObject) o).getString("name")));
			}
		}
		return jsonArray;
	}

	private static Map<String, String> getParamComments(PsiMethod psiMethod) {
		Map<String, String> paramCommentMap = new HashMap<>();

		PsiDocComment docComment = psiMethod.getDocComment();
		if (docComment != null) {
			for (PsiDocTag docTag : docComment.findTagsByName("param")) {
				PsiElement[] dataElements = docTag.getDataElements();
				if (dataElements.length > 0) {
					String paramName = dataElements[0].getText();
					// 获取 @param 标签之后的文本内容作为注释
					String commentText = docTag.getText().replaceFirst("@param\\s+" + paramName, "").trim();
					commentText = commentText.replace("\n", "");
					commentText = commentText.replace("*", "").trim();
					paramCommentMap.put(paramName, commentText);
				}
			}
		}

		return paramCommentMap;
	}




	private static String getBody(PsiClassType classType, Boolean mock) {
		List<FieldInfo> fieldInfos;
		String body = "";
		PsiClass psiClassType = classType.resolve();
		if (CommonClassNames.JAVA_UTIL_LIST.equals(psiClassType.getQualifiedName())) {
			PsiType[] parameters = classType.getParameters();
			if (parameters.length == 1 && parameters[0] instanceof PsiClassType) {
				PsiClass elementClass = ((PsiClassType) parameters[0]).resolve();
				if (elementClass != null) {
					fieldInfos = ClassParse.parseFieldsWithComments(elementClass);
					body = ClassParse.getRequestBodyMeta(fieldInfos, "List", mock);

				}
			}
		} else if (ClassParse.isCustomType(classType))  {
			fieldInfos = ClassParse.parseFieldsWithComments(psiClassType);
			body = ClassParse.getRequestBodyMeta(fieldInfos, "Object", mock);
		}
		return body;
	}

	private static String getReturn(PsiClassType classType, PsiClass psiClass, Boolean mock) {
		// 获取实际的泛型类型替换信息
		PsiClassType.ClassResolveResult classResolveResult = classType.resolveGenerics();
		PsiSubstitutor substitutor = classResolveResult.getSubstitutor();
        String resultResult = "";

		if (ClassParse.isCustomType(classType)) {
			// 如果是对象类型
			// 解析返回值
			List<FieldInfo> returnObjList = ClassParse.parseFieldsWithComments(psiClass, substitutor);
			resultResult = ClassParse.getJsonResultMeta(returnObjList, "Object", mock);
		} else if (Objects.requireNonNull(psiClass.getQualifiedName()).startsWith("java.lang.")) {
			// 如果是基本数据类型
			String type = ClassParse.typeTransform(psiClass.getQualifiedName()
					.substring(psiClass.getQualifiedName().lastIndexOf('.') + 1));
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("type", type);
			resultResult = JSON.toJSONString(jsonObject);
		} else if (Objects.requireNonNull(psiClass.getQualifiedName()).startsWith("java.util.List") ||
				Objects.requireNonNull(psiClass.getQualifiedName()).startsWith("java.util.Set")) {
			// 如果是列表类型
			// 获取列表的泛型参数
			PsiType substituted = substitutor.substitute(classType);
			PsiClassType psiClassType = (PsiClassType) substituted;
			PsiClass subPsiClass = psiClassType.resolve();
			if (subPsiClass != null && ClassParse.isCustomType(psiClassType)) {
				List<FieldInfo> returnObjList = ClassParse.parseFieldsWithComments(subPsiClass, substitutor);
				resultResult = ClassParse.getJsonResultMeta(returnObjList, "List", mock);
			} else if (subPsiClass != null) {
				PsiType[] generics = classType.getParameters();
				if (generics.length > 0 && generics[0] instanceof PsiClassType) {
					PsiClass genericClass = ((PsiClassType) generics[0]).resolve();
					String type = ClassParse.typeTransform(genericClass.getQualifiedName()
							.substring(genericClass.getQualifiedName().lastIndexOf('.') + 1));
					JSONObject schema = JSON.parseObject(ClassParse.META_SCHEMA_LIST);
					schema.getJSONObject("items").put("type",type);
					resultResult = JSON.toJSONString(schema, SerializerFeature.PrettyFormat);
				}
			}
		} else {
			// 如果是MAP类型 不处理
		}
		return resultResult;
	}

    private static boolean isGetMapping(PsiMethod method) {
        // 检查方法注解
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains("GetMapping")) {
                return true;
            }
        }
        return false;
    }
	
	public static Map<String, String> getUrlPath(PsiMethod psiMethod) {
		Map<String, String> map = new HashMap<>(2);
		PsiClass containingClass = psiMethod.getContainingClass();
		String classPath = "";

		// 获取类上的 @RequestMapping 注解路径
		if (containingClass != null) {
			for (PsiAnnotation annotation : containingClass.getAnnotations()) {
				String qualifiedName = annotation.getQualifiedName();
				if (qualifiedName != null && qualifiedName.endsWith("RequestMapping")) {
					classPath = getAnnotationValue(annotation);
					break;
				}
			}
		}

		// 获取方法上的注解和路径
		for (PsiAnnotation annotation : psiMethod.getAnnotations()) {
			String qualifiedName = annotation.getQualifiedName();
			if (qualifiedName == null) continue;

			String method = null;
			if (qualifiedName.contains("GetMapping")) {
				method = "GET";
			} else if (qualifiedName.contains("PostMapping")) {
				method = "POST";
			} else if (qualifiedName.contains("PutMapping")) {
				method = "PUT";
			} else if (qualifiedName.contains("DeleteMapping")) {
				method = "DELETE";
			} else if (qualifiedName.contains("RequestMapping")) {
				// 判断 method 属性
				PsiAnnotationMemberValue methodValue = annotation.findDeclaredAttributeValue("method");
				if (methodValue != null) {
					String text = methodValue.getText();
					if (text.contains("GET")) method = "GET";
					else if (text.contains("POST")) method = "POST";
					else if (text.contains("PUT")) method = "PUT";
					else if (text.contains("DELETE")) method = "DELETE";
				}
			}

			if (method != null) {
				String methodPath = getAnnotationValue(annotation);
				String fullPath = joinPaths(classPath, methodPath);
				map.put("method", method);
				map.put("path", fullPath);
				break;
			}
		}

		return map;
	}

	/**
	 * 提取注解中的路径值（value 或 path）
	 */
	private static String getAnnotationValue(PsiAnnotation annotation) {
		PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
		if (value == null) {
			value = annotation.findDeclaredAttributeValue("path");
		}
		if (value != null) {
			String text = value.getText();
			// 去掉双引号
			if (text.startsWith("\"") && text.endsWith("\"")) {
				return text.substring(1, text.length() - 1);
			}
			return text;
		}
		return "";
	}

	/**
	 * 拼接路径，处理多余的 /
	 */
	private static String joinPaths(String basePath, String subPath) {
		if (basePath == null) basePath = "";
		if (subPath == null) subPath = "";
		String full = ("/" + basePath + "/" + subPath).replaceAll("/+", "/");
		return full.endsWith("/") && full.length() > 1 ? full.substring(0, full.length() - 1) : full;
	}

}
