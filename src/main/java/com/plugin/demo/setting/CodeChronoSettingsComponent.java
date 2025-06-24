package com.plugin.demo.setting;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;


/**
 * @author codechrono
 * 组件类，用于构建和配置设置对话框中的面板。
 */
public class CodeChronoSettingsComponent {

    private final JPanel mainPanel;
    private final JBTextField zhipuAkiKeyText = new JBTextField();
    private final JBTextField aLiAkiKeyText = new JBTextField();
    private final JRadioButton zhipuButton = new JRadioButton("智谱");
    private final JRadioButton aLiButton = new JRadioButton("阿里");
    private final JBTextField zhipuModelNameText = new JBTextField();
    private final JBTextField aLiModelNameText = new JBTextField();
    private final JBTextField userName = new JBTextField();
    private final JBTextField password = new JBTextField();
    private final JBTextField groupName = new JBTextField();
    private final JBTextField projectName = new JBTextField();
    private final JBTextField categoryName = new JBTextField();
    private final JBTextField url = new JBTextField();
    private final JBTextArea jsonTextArea = new JBTextArea();
    private JBScrollPane scrollPane;
    private final ButtonGroup group = new ButtonGroup();


    public CodeChronoSettingsComponent() {
        initUIComponents();
        addListeners();
        mainPanel = getMainPanel();

    }

    /**
     * 获取首选的焦点组件
     */
    public JComponent getPreferredFocusedComponent() {
        return zhipuAkiKeyText;
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void reset(CodeChronoSettings.State state) {
        // 重置组件状态的代
        zhipuAkiKeyText.setText(state.zhiPuApiKey);
        aLiAkiKeyText.setText(state.aLiApiKey);
        zhipuModelNameText.setText(state.zhiPuModelName);
        aLiModelNameText.setText(state.aLiModelName);

        url.setText(state.url);
        userName.setText(state.userName);
        password.setText(state.password);
        groupName.setText(state.groupName);
        projectName.setText(state.projectName);
        categoryName.setText(state.categoryName);
        jsonTextArea.setText(state.jsonText);

        if (state.type == 0) {
            group.setSelected(zhipuButton.getModel(), true);
        } else {
            group.setSelected(aLiButton.getModel(), true);
        }
    }

    public void apply(CodeChronoSettings.State state) {
        // 这里可以添加应用组件状态的代码，例如：
        state.zhiPuApiKey = zhipuAkiKeyText.getText();
        state.aLiApiKey = aLiAkiKeyText.getText();
        state.zhiPuModelName = zhipuModelNameText.getText();
        state.aLiModelName = aLiModelNameText.getText();
        state.type = zhipuButton.isSelected() && !aLiButton.isSelected() ? 0 : 1;
        state.url = url.getText();
        state.userName = userName.getText();
        state.password = password.getText();
        state.groupName = groupName.getText();
        state.projectName = projectName.getText();
        state.categoryName = categoryName.getText();
        state.jsonText = jsonTextArea.getText();
        CodeChronoSettings.getInstance().loadState(state);

    }

    /**
     * 控件值是否为默认值
     *
     * @param state
     * @return
     */
    public boolean isDefault(CodeChronoSettings.State state) {
        if (zhipuAkiKeyText.getText().equals(state.zhiPuApiKey)
                && aLiAkiKeyText.getText().equals(state.aLiApiKey)
                && zhipuModelNameText.getText().equals(state.zhiPuModelName)
                && aLiModelNameText.getText().equals(state.aLiModelName)
                &&(zhipuButton.isSelected()
//                && (Objects.equals(url.getText(), state.url))
//                && (Objects.equals(userName.getText(), state.userName))
//                && (Objects.equals(password.getText(), state.password))
//                && (Objects.equals(groupName.getText(), state.groupName))
//                && (Objects.equals(projectName.getText(), state.projectName))
//                && (Objects.equals(categoryName.getText(), state.categoryName))
                && (Objects.equals(url.getText(), ""))
                && (Objects.equals(userName.getText(), ""))
                && (Objects.equals(password.getText(), ""))
                && (Objects.equals(groupName.getText(), ""))
                && (Objects.equals(projectName.getText(), ""))
                && (Objects.equals(categoryName.getText(), ""))
                && (Objects.equals(jsonTextArea.getText(), ""))
        )
        ) {
            return true;
        }
        return false;
    }

    /**
     * 初始化组件样式
     */
    private void initUIComponents() {
        jsonTextArea.setLineWrap(true);
        jsonTextArea.setWrapStyleWord(true);
        jsonTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        jsonTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        jsonTextArea.setText("{\n    \"key\": \"value\"\n}");
        scrollPane = new JBScrollPane(
                jsonTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setPreferredSize(new Dimension(600, 500));
        zhipuAkiKeyText.setToolTipText("请输入智谱的apikey");
        aLiAkiKeyText.setToolTipText("请输入阿里云的apikey");
        zhipuModelNameText.setToolTipText("请输入智谱的模型名称");
        aLiModelNameText.setToolTipText("请输入阿里云的模型名称");
        url.setToolTipText("请输入Yapi地址");
        userName.setToolTipText("请输入Yapi用户名");
        password.setToolTipText("请输入Yapi密码");
        groupName.setToolTipText("请输入项目组名称");
        projectName.setToolTipText("请输入项目名称");
        categoryName.setToolTipText("请输入类别名称");
        group.add(zhipuButton);
        group.add(aLiButton);
        group.setSelected(zhipuButton.getModel(), true);
    }

    private JPanel getMainPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(zhipuButton, 1)
                .addComponent(aLiButton, 1)
                .addLabeledComponent(new JBLabel("智谱apikey："), zhipuAkiKeyText, 1, false)
                .addLabeledComponent(new JBLabel("智谱模型名称："), zhipuModelNameText, 1, false)
                .addLabeledComponent(new JBLabel("阿里apikey："), aLiAkiKeyText, 1, false)
                .addLabeledComponent(new JBLabel("阿里模型名称："), aLiModelNameText, 1, false)
                .addLabeledComponent(new JBLabel("Yapi地址："), url, 1, false)
                .addLabeledComponent(new JBLabel("Yapi用户名："), userName, 1, false)
                .addLabeledComponent(new JBLabel("Yapi密码："), password, 1, false)
                .addLabeledComponent(new JBLabel("项目组名称："), groupName, 1, false)
                .addLabeledComponent(new JBLabel("项目名称："), projectName, 1, false)
                .addLabeledComponent(new JBLabel("类别名称："), categoryName, 1, false)
                .addLabeledComponent(new JBLabel("自定义附加参数："), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    /**
     * 组件添加监听事件
     */
    public void addListeners() {
        zhipuButton.addActionListener(e -> modelChangeAction());
        aLiButton.addActionListener(e -> modelChangeAction());
    }


    /**
     *
     */
    private void modelChangeAction() {
        if (zhipuButton.isSelected()) {
            Objects.requireNonNull(CodeChronoSettings.getInstance().getState()).type = 0;
        } else {
            Objects.requireNonNull(CodeChronoSettings.getInstance().getState()).type = 1;
        }
    }

}