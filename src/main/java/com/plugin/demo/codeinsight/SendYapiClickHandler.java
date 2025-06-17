package com.plugin.demo.codeinsight;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.plugin.demo.business.ClassParse;
import com.plugin.demo.event.AnalysisTopics;
import com.plugin.demo.model.FieldInfo;
import com.plugin.demo.yapi.YapiInterface;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlinx.html.S;

import java.awt.event.MouseEvent;
import java.util.*;


/**
 * 
 *
 * @author Liu Guangxin
 * @date 2025/6/13 8:18
 */
class SendYapiClickHandler implements Function2<MouseEvent, Editor, Unit> {
    private final PsiMethod psiMethod;
    private final Boolean mock;
    public SendYapiClickHandler(PsiMethod psiMethod, Boolean mock) {
        this.psiMethod = psiMethod;
        this.mock = mock;
    }

	public Unit invoke(MouseEvent event, Editor editor) {
        // 登录 yapi
        // 创建接口
        // 获取到方法上的备注
		String methodDoc = ClassParse.getCommentText(psiMethod);
		System.out.println(methodDoc);
		// 获取到请求路径
		Map<String, String> map = getUrlPath(psiMethod);
		System.out.println(map.get("method"));
		System.out.println(map.get("path"));
		Map<String, String> resultMap = YapiInterface.createInterface(map.get("method"), methodDoc, map.get("path"));
		if ("fail".equals(resultMap.get("status"))) {
			if ("40022".equals(resultMap.get("errorCode"))) {
				// 需要弹窗提示接口已存在是否覆盖
				boolean flag = new SampleDialogWrapper().showAndGet();
				if (flag) {
					// 更新数据
					update(resultMap, map, methodDoc);
				}
			} else {
				sendMessageToWindows(editor.getProject(), resultMap.get("errmsg"));
			}
		}
		if ("success".equals(resultMap.get("status"))) {
			// 更新数据
			update(resultMap, map, methodDoc);
		}
		return null;
	}

	private void sendMessageToWindows(Project project, String message) {
		ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("YapiShowWindow");
		if (toolWindow != null) {
			toolWindow.activate(null);
		}
		project.getMessageBus().syncPublisher(AnalysisTopics.SHOW_TOPIC)
				.getCodeContent(message);
	}

	private void update(Map<String, String> resultMap, Map<String, String> map, String methodDoc) {
		// 更新数据
		Map<String, Object> paramsMap = getParams(psiMethod);
		YapiInterface.updateInterface(
				resultMap.get("id"), resultMap.get("catId"),
				map.get("method"), map.get("path"),
                (String) paramsMap.get("body"),
				(String) paramsMap.get("return"),
				(JSONArray) paramsMap.get("params"),
				methodDoc);
	}

    private Map<String, Object> getParams(PsiMethod psiMethod) {
		Map<String, Object> map = new HashMap<>(2);
		// 获取参数  如果仅有一个参数且该参数为自定义对象，则需要生成请求体参数
		PsiParameter[] parameter = psiMethod.getParameterList().getParameters();
		String body = "";
		// 请求体的请求
		if (!isGetMapping(psiMethod)) {
			for (PsiParameter psiParameter: parameter) {
				if (ClassParse.isCustomType(psiParameter.getType())) {
					body = getBody(psiParameter);
				}
			}
		}
		if (isGetMapping(psiMethod)) {
			JSONArray params = getRequestParam(parameter);
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
			String resultResult = getReturn(classType, psiClassType);
            if (!resultResult.isEmpty()) {
				map.put("return", resultResult);
            }
        }
        return map;
    }
	private JSONArray getRequestParam(PsiParameter[] parameter) {
		JSONArray jsonArray = new JSONArray();
		for (PsiParameter psiParameter: parameter) {
			// 1. 排除注解为路径参数的参数
			PsiAnnotation[] annotations = psiParameter.getAnnotations();
			for (PsiAnnotation annotation : annotations) {
				if (Objects.requireNonNull(annotation.getQualifiedName()).contains("PathVariable")) {
					break;
				}
			}
			// 2. 如果是基本类型，直接添加
			if (!ClassParse.isCustomType(psiParameter.getType())) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", psiParameter.getName());
				jsonObject.put("required", "1");
				jsonObject.put("example", "");
				jsonObject.put("desc", "");
				jsonArray.add(jsonObject);
			} else if (psiParameter.getType().getPresentableText().startsWith("Page<")) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", "current");
				jsonObject.put("required", "1");
				jsonObject.put("example", "");
				jsonObject.put("desc", "");
				jsonArray.add(jsonObject);
				JSONObject jsonObject1 = new JSONObject();
				jsonObject1.put("name", "size");
				jsonObject1.put("required", "1");
				jsonObject1.put("example", "");
				jsonObject1.put("desc", "");
				jsonArray.add(jsonObject1);
			} else if (ClassParse.isCustomType(psiParameter.getType())) {
				// 3. 如果是自定义对象类型， 解析后添加
				// 1. 遍历类的所有字段
				PsiClassType  psiClassType = (PsiClassType) psiParameter.getType();
				PsiClass psiClass = psiClassType.resolve();
				for (PsiField field : psiClass.getFields()) {
					// 获取字段名称
					String fieldName = field.getName();
					// 排除序列化属性
					if ("serialVersionUID".equals(fieldName)) {
						continue;
					}
					// 不处理对象类型
					if (!ClassParse.isCustomType(field.getType())) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("name", fieldName);
						jsonObject.put("required", "1");
						jsonObject.put("example", "");
						jsonObject.put("desc", "");
						jsonArray.add(jsonObject);
					}
				}
			}
		}
		return jsonArray;
	}
	private String getBody(PsiParameter parameter) {
		List<FieldInfo> fieldInfos;
		String body = "";
		PsiType fieldType = parameter.getType();
		PsiClassType classType = (PsiClassType) fieldType;
		PsiClass psiClassType = classType.resolve();
		if (CommonClassNames.JAVA_UTIL_LIST.equals(psiClassType.getQualifiedName())) {
			PsiType[] parameters = classType.getParameters();
			if (parameters.length == 1 && parameters[0] instanceof PsiClassType) {
				PsiClass elementClass = ((PsiClassType) parameters[0]).resolve();
				if (elementClass != null) {
					fieldInfos = ClassParse.parseFieldsWithComments(elementClass);
					body = ClassParse.getRequestBodyMeta(fieldInfos, true, mock);

				}
			}
		} else if (ClassParse.isCustomType(fieldType))  {
			fieldInfos = ClassParse.parseFieldsWithComments(psiClassType);
			body = ClassParse.getRequestBodyMeta(fieldInfos, false, mock);
		}
		return body;
	}

	private String getReturn(PsiClassType classType, PsiClass psiClassType) {

		// 获取实际的泛型类型替换信息
		PsiSubstitutor substitutor = classType.resolveGenerics().getSubstitutor();
        List<FieldInfo> subFieldInfos;
        String resultResult = "";
		for (PsiField field : psiClassType.getAllFields()) {
			if ("data".equals(field.getName())) {
				PsiType rawType = field.getType();
				PsiType actualType = substitutor.substitute(rawType); // 替换 T -> List<FrontKnowledgeBaseEntityResponseDTO>
				if (actualType instanceof PsiClassType) {
					PsiClassType actualClassType = (PsiClassType) actualType;
					PsiClass resolved = actualClassType.resolve();
					// 如果是 List 类型，还需要进一步提取元素类型
					if (resolved != null && CommonClassNames.JAVA_UTIL_LIST.equals(resolved.getQualifiedName())) {
						PsiType[] parameters = actualClassType.getParameters();
						if (parameters.length == 1 && parameters[0] instanceof PsiClassType) {
							PsiClass elementClass = ((PsiClassType) parameters[0]).resolve();
							if (elementClass != null) {
								subFieldInfos = ClassParse.parseFieldsWithComments(elementClass);
                                 resultResult = ClassParse.getJsonResultMeta(subFieldInfos, "List", mock);
							}
						}
					} else if (resolved != null && resolved.getQualifiedName().endsWith(".PageDTO")) {
                        PsiType[] parameters = actualClassType.getParameters();
                        PsiClass elementClass = ((PsiClassType) parameters[0]).resolve();
                        if (elementClass != null) {
                            subFieldInfos = ClassParse.parseFieldsWithComments(elementClass);
                            resultResult = ClassParse.getJsonResultMeta(subFieldInfos, "Page", mock);
                        }
                    } else if (resolved != null && resolved.getQualifiedName().endsWith("Void")) {
                        // 非 List 的直接类型
                        resultResult = ClassParse.getJsonResultMeta(new ArrayList<>(), "Object", mock);
                    } else if (resolved != null && ClassParse.isCustomType(actualClassType)) {
						// 非 List 的直接类型
						subFieldInfos = ClassParse.parseFieldsWithComments(resolved);
                        resultResult = ClassParse.getJsonResultMeta(subFieldInfos, "Object", mock);
                    } else if (resolved != null && !ClassParse.isCustomType(actualClassType)) {
						String type = ClassParse.typeTransform(resolved.getQualifiedName()
									.substring(resolved.getQualifiedName().lastIndexOf('.') + 1));
						resultResult = ClassParse.getJsonResultMeta(new ArrayList<>(), "Object", mock);
						JSONObject jsonObject = JSON.parseObject(resultResult);
						jsonObject.getJSONObject("properties").getJSONObject("data").put("type", type);
						resultResult = JSON.toJSONString(jsonObject);
					}
				}
			}
		}
		return resultResult;
	}

    private boolean isGetMapping(PsiMethod method) {
        // 检查方法注解
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains("GetMapping")) {
                return true;
            }
        }
        return false;
    }
	
	private Map<String, String> getUrlPath(PsiMethod psiMethod) {
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
	private String getAnnotationValue(PsiAnnotation annotation) {
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
	private String joinPaths(String basePath, String subPath) {
		if (basePath == null) basePath = "";
		if (subPath == null) subPath = "";
		String full = ("/" + basePath + "/" + subPath).replaceAll("/+", "/");
		return full.endsWith("/") && full.length() > 1 ? full.substring(0, full.length() - 1) : full;
	}

}