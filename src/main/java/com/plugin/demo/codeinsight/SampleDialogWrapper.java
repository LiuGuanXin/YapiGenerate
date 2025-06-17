package com.plugin.demo.codeinsight;

import com.intellij.openapi.ui.DialogWrapper;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Liu Guangxin
 * @date 2025/6/17 10:11
 */
public class SampleDialogWrapper extends DialogWrapper {

    public SampleDialogWrapper() {
        super(true); // use current window as parent
        setTitle("接口覆盖提示");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("接口已存在，是否覆盖接口");
        label.setPreferredSize(new Dimension(100, 80));
        dialogPanel.add(label, BorderLayout.CENTER);

        return dialogPanel;
    }
}