package com.plugin.demo.codeinsight;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.plugin.demo.business.ClassParse;
import com.plugin.demo.event.AnalysisTopics;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import java.util.HashMap;
import java.awt.event.MouseEvent;
import java.util.Map;


/**
 * 
 *
 * @author Liu Guangxin
 * @date 2025/6/13 8:18
 */
class WindowsClickHandler implements Function2<MouseEvent, Editor, Unit> {
    private final PsiMethod psiMethod;
    private final Boolean mock;
    public WindowsClickHandler(PsiMethod psiMethod, Boolean mock) {
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
        Map<String, Object> map = ClassParse.getParams(psiMethod, new HashMap<>(), mock);
        String returnString = (String) map.get("return");
        String body = (String) map.get("body");
        JSONArray params = (JSONArray) map.get("params");
        String result = "";
        Map<String, String> pathMap = ClassParse.getUrlPath(psiMethod);
        if (!pathMap.isEmpty()) {
            result = result + "请求路径如下：\n" +
                    "请求方式：" + pathMap.get("method") + "\n请求路径："
                    + pathMap.get("path") + "\n";
        }
        if (params != null) {
            String paramsString = JSON.toJSONString(params, SerializerFeature.PrettyFormat);
            result = result + "请求参数格式如下：\n" +
                    "```json\n" + paramsString + "\n```\n";
        }
        if (body != null) {
            result = result + "请求体的元数据格式如下：\n" +
                    "```json\n" + body + "\n```\n";
        }
        if (returnString != null) {
            result = result + "返回结果的元数据格式如下：\n" +
                    "```json\n" + returnString + "\n```\n";
        }
        return result;
    }
}