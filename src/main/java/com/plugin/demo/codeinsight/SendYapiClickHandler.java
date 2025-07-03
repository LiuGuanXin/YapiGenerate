package com.plugin.demo.codeinsight;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.plugin.demo.business.ClassParse;
import com.plugin.demo.event.AnalysisTopics;
import com.plugin.demo.yapi.YapiInterface;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

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
		// 获取到请求路径
		Map<String, String> map = ClassParse.getUrlPath(psiMethod);
		String info = "确认是否生成（防止误触确认）";
		if (mock) {
			info = "点击确认后开始生成Mock数据，生成速度慢，请稍后，不要重复点击。";
		}
		boolean flag = new PromptDialogWrapper(info).showAndGet();
		if (!flag) {
			return null;
		}
		Map<String, String> resultMap = YapiInterface.createInterface(map.get("method"), methodDoc, map.get("path"));
		if ("fail".equals(resultMap.get("status"))) {
			if ("40022".equals(resultMap.get("errorCode"))) {
				// 当前接口路径  需要获取 组名称、项目名称、分类名称
				// 需要弹窗提示接口已存在是否覆盖
				String interfacePath = YapiInterface.getInterfacePath(resultMap.get("id"));
				boolean confirm = new SampleDialogWrapper(interfacePath).showAndGet();
				if (confirm) {
					// 更新数据
					update(editor.getProject(), resultMap, map, methodDoc);
				}
			} else {
				sendMessageToWindows(editor.getProject(), resultMap.get("errmsg"));
			}
		}
		if ("success".equals(resultMap.get("status"))) {
			// 更新数据
			update(editor.getProject(), resultMap, map, methodDoc);
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

	private void update(Project project, Map<String, String> resultMap, Map<String, String> map, String methodDoc) {
		if (mock) {
			// 异步执行，不阻塞主线程
			try {
				// 更新数据
				ApplicationManager.getApplication().executeOnPooledThread(() -> {
					ApplicationManager.getApplication().runReadAction(() -> {
						Map<String, Object> paramsMap = ClassParse.getParams(psiMethod, map, mock);
						Map<String, String> result = YapiInterface.updateInterface(
								resultMap.get("id"), resultMap.get("catId"),
								map.get("method"), map.get("path"),
								(String) paramsMap.get("body"),
								(String) paramsMap.get("return"),
								(JSONArray) paramsMap.get("params"),
								(List<JSONObject>) paramsMap.get("pathVariable"), methodDoc
						);
						if (ObjectUtil.isNotNull(result.get("errorMsg"))) {
							// UI 相关部分在 EDT 中执行
							sendMessageToWindows(project, result.get("errorMsg"));
						}
						// UI 相关部分在 EDT 中执行
						ApplicationManager.getApplication().invokeLater(() -> {
							new FinishDialogWrapper().showAndGet();
						});
					});
				});

			} catch (Exception e) {
				new PromptDialogWrapper("处理过程出现异常，请重试.....").showAndGet();
			}

		} else {
			// 更新数据
			Map<String, Object> paramsMap = ClassParse.getParams(psiMethod, map, mock);
			Map<String, String> result = YapiInterface.updateInterface(
					resultMap.get("id"), resultMap.get("catId"),
					map.get("method"), map.get("path"),
					(String) paramsMap.get("body"),
					(String) paramsMap.get("return"),
					(JSONArray) paramsMap.get("params"),
                    (List<JSONObject>) paramsMap.get("pathVariable"), methodDoc);
			if (ObjectUtil.isNotNull(result.get("errorMsg"))) {
				// UI 相关部分在 EDT 中执行
				sendMessageToWindows(project, result.get("errorMsg"));
			}
			new FinishDialogWrapper().showAndGet();
		}
	}
}