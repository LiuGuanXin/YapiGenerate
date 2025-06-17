package com.plugin.demo.codeinsight;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.plugin.demo.business.ClassParse;
import com.plugin.demo.event.AnalysisTopics;
import com.plugin.demo.model.FieldInfo;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseEvent;


/**
 * 
 *
 * @author Liu Guangxin
 * @date 2025/6/13 8:18
 */
class MyClickHandler implements Function2<MouseEvent, Editor, Unit> {
    private final PsiMethod psiMethod;
    private final Boolean mock;
    public MyClickHandler (PsiMethod psiMethod, Boolean mock) {
        this.psiMethod = psiMethod;
        this.mock = mock;
    }

	public Unit invoke(MouseEvent event, Editor editor) {
		if (mock) {
			// 异步执行，不阻塞主线程
            Project project = editor.getProject();
            sendMessageToWindows(project, "正在生成mock数据，请稍后.....");
            try {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    String result = ApplicationManager.getApplication().runReadAction(
                            (Computable<String>) () -> deal(this.psiMethod)
                    );
                    sendMessageToWindows(project, result);
                });
            } catch (Exception e) {
                sendMessageToWindows(project, "出现异常，请重试.....");
            }

		} else {
			String result = deal(this.psiMethod);
			Project project = editor.getProject();
			if (project != null) {
                sendMessageToWindows(project, result);
			}
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

    private String deal(PsiMethod psiMethod) {
        // 获取参数  如果仅有一个参数且该参数为自定义对象，则需要生成请求体参数
        PsiParameter[] parameter = psiMethod.getParameterList().getParameters();
        List<FieldInfo> fieldInfos;
        String body = "";
        // 请求体的请求
        if (!isGetMapping(psiMethod)) {
            for (PsiParameter psiParameter: parameter) {
                if (ClassParse.isCustomType(psiParameter.getType())) {
                    PsiType fieldType = psiParameter.getType();
                    PsiClassType classType = (PsiClassType) fieldType;
                    PsiClass psiClassType = classType.resolve();
                    if (CommonClassNames.JAVA_UTIL_LIST.equals(psiClassType.getQualifiedName())) {
                        PsiType[] parameters = classType.getParameters();
                        if (parameters.length == 1 && parameters[0] instanceof PsiClassType) {
                            PsiClass elementClass = ((PsiClassType) parameters[0]).resolve();
                            if (elementClass != null) {
                                fieldInfos = ClassParse.parseFieldsWithComments(elementClass);
                                body = ClassParse.getRequestBody(fieldInfos, true, mock);

                            }
                        }
                    } else if (ClassParse.isCustomType(fieldType))  {
                        fieldInfos = ClassParse.parseFieldsWithComments(psiClassType);
                        body = ClassParse.getRequestBody(fieldInfos, false, mock);
                    }
                    break;
                }
            }
        }
        // 打印请求体的json
        System.out.println(body);
        String result = "";
        if (!body.isEmpty()) {
            result = "请求体" + body + "\n\n\n\n\n";
        }
        // 获取返回值
        if (psiMethod.getReturnType() != null) {
            PsiClassType classType = (PsiClassType) psiMethod.getReturnType();
            PsiClass psiClassType = classType.resolve();
            fieldInfos = ClassParse.parseFieldsWithComments(psiClassType);
            for (FieldInfo fieldInfo : fieldInfos){
                System.out.println(fieldInfo);
            }

			// 获取实际的泛型类型替换信息
			PsiSubstitutor substitutor = classType.resolveGenerics().getSubstitutor();
            List<FieldInfo> subFieldInfos;
            String resultResult = "";
            if (psiClassType == null) {
                return resultResult;
            }
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
                                    resultResult = ClassParse.getJsonResult(subFieldInfos, "List", mock);
								}
							}
						} else if (resolved != null && resolved.getQualifiedName().endsWith(".PageDTO")) {
                            PsiType[] parameters = actualClassType.getParameters();
                            PsiClass elementClass = ((PsiClassType) parameters[0]).resolve();
                            if (elementClass != null) {
                                subFieldInfos = ClassParse.parseFieldsWithComments(elementClass);
                                resultResult = ClassParse.getJsonResult(subFieldInfos, "Page", mock);
                            }
                        } else if (resolved != null && resolved.getQualifiedName().endsWith("Void")) {
                            // 非 List 的直接类型
                            resultResult = ClassParse.getJsonResult(new ArrayList<>(), "Object", mock);
                        } else if (resolved != null) {
							// 非 List 的直接类型
							subFieldInfos = ClassParse.parseFieldsWithComments(resolved);
                            resultResult = ClassParse.getJsonResult(subFieldInfos, "Object", mock);
                        }
					}
				}
			}
            if (!resultResult.isEmpty()) {
                result = result + "方法返回值" + resultResult;
            }
        }
        return result;
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
}