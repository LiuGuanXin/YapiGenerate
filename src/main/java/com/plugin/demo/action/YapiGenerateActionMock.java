package com.plugin.demo.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.plugin.demo.business.ClassParse;
import com.plugin.demo.event.AnalysisTopics;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * 右键点击事件
 *
 * @author Liu Guangxin
 * @date 2025/2/17 8:31
 */
public class YapiGenerateActionMock extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 异步执行，不阻塞主线程
        Project project = e.getProject();
        sendMessageToWindows(project, "正在生成mock数据，请稍后.....");
        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                String result = ApplicationManager.getApplication().runReadAction(
                        (Computable<String>) () ->
                                ClassParse.getJsonResult(e, "Object", true)
                );
                sendMessageToWindows(project, result);
            });
        } catch (Exception ex) {
            sendMessageToWindows(project, "出现异常，请重试.....");
        }
    }

    private void sendMessageToWindows(Project project, String message) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("YapiShowWindow");
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
        project.getMessageBus().syncPublisher(AnalysisTopics.SHOW_TOPIC)
                .getCodeContent(message);
    }


}
