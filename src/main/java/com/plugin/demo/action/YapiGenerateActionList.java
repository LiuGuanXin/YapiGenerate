package com.plugin.demo.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.plugin.demo.business.ClassParse;
import com.plugin.demo.event.AnalysisTopics;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * 右键点击事件
 *
 * @author Liu Guangxin
 * @date 2025/2/17 8:31
 */
public class YapiGenerateActionList extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        String result = ClassParse.getJsonResult(e, "List", false);
        sendMessageToWindows(e.getProject(), result);

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
