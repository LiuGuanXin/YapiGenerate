package com.plugin.demo.windows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.plugin.demo.event.AnalysisTopics;
import com.plugin.demo.listener.CodeAideListener;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;



/**
 * lgx
 * @author 14010
 */
public class YapiShowWindowFactory implements ToolWindowFactory {
    private JTextPane textPane;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        // Markdown 内容
        String markdownContent = "# 此页面生成Json文档。";

        // 将 Markdown 内容转换为 HTML
        String htmlContent = renderMarkdownToHtml(markdownContent);

        // 创建 JTextPane 用于显示 HTML 内容
        textPane = new JTextPane();
        textPane.setContentType("text/html");

        // 包装 HTML 内容，并添加换行的 CSS 样式
        String styledHtmlContent = "<html>" +
                "<body style='white-space: normal; word-wrap: break-word;'>"
                + htmlContent + "</body></html>";
        textPane.setText(styledHtmlContent);

        textPane.setEditable(false);
        textPane.setBackground(JBColor.WHITE);

        JScrollPane scrollPane = new JBScrollPane(textPane);
        // 设置 ToolWindow 的内容面板
        SimpleToolWindowPanel simpleToolWindowPanel = new SimpleToolWindowPanel(true, true);
        simpleToolWindowPanel.setContent(scrollPane);


        // 创建内容并添加到ToolWindow
        Content content = ContentFactory.getInstance()
                .createContent(simpleToolWindowPanel, "Yapi输出数据生成", false);
        toolWindow.getContentManager().addContent(content);


        project.getMessageBus().connect().subscribe(
                AnalysisTopics.SHOW_TOPIC,
                (CodeAideListener) text -> {
                    // 假设重新获取新的 Markdown 内容
                    String refreshText = renderMarkdownToHtml(text);

                    // 使用相同的样式重新设置 HTML 内容
                    String refreshContext = "<html><body style='white-space: normal; word-wrap: break-word;'>" + refreshText + "</body></html>";
                    textPane.setText(refreshContext);
                }
        );
    }

    /**
     * 使用 CommonMark 渲染 Markdown 为 HTML
     * @param markdown Markdown 文本
     * @return HTML 文本
     */
    private String renderMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

}
