package com.plugin.demo.codeinsight;

import com.intellij.openapi.ui.DialogWrapper;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Liu Guangxin
 * @date 2025/6/17 10:11
 */
public class PromptDialogWrapper extends DialogWrapper {
    private String content;
    public PromptDialogWrapper(String content) {
        super(true); // use current window as parent
        this.content = content;
        setTitle("提示");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        // 将换行符替换为 <br>，并包裹在 HTML 中
        JLabel label = new JLabel(content);
        label.setPreferredSize(new Dimension(150, 100));
        dialogPanel.add(label, BorderLayout.CENTER);

        return dialogPanel;
    }
}