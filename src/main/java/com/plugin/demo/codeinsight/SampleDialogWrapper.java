package com.plugin.demo.codeinsight;

import com.intellij.openapi.ui.DialogWrapper;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import kotlinx.html.S;

import javax.swing.*;
import java.awt.*;

/**
 * @author Liu Guangxin
 * @date 2025/6/17 10:11
 */
public class SampleDialogWrapper extends DialogWrapper {
    private String content = "接口已存在，是否覆盖接口？";
    public SampleDialogWrapper(String content) {
        super(true); // use current window as parent
        this.content = this.content + "\n" + "接口路径：" + "\n" + content;
        setTitle("接口覆盖提示");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        // 将换行符替换为 <br>，并包裹在 HTML 中
        String htmlContent = "<html>" + this.content.replace("\n", "<br>") + "</html>";
        JLabel label = new JLabel(htmlContent);
        label.setPreferredSize(new Dimension(150, 100));
        dialogPanel.add(label, BorderLayout.CENTER);

        return dialogPanel;
    }
}