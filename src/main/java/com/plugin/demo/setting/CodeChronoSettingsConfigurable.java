package com.plugin.demo.setting;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * @author codechrono
 * Configurable 用于在 IDEA 设置中展示一个配置面板，它实现了 Configurable 接口。
 */
final class CodeChronoSettingsConfigurable implements Configurable {

    private CodeChronoSettingsComponent codeChronoSettingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Yapi Generate Setting";
    }

    /**
     * 获取首选聚焦组件。
     * @return
     */
    @Override
    public JComponent getPreferredFocusedComponent() {
        return codeChronoSettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        codeChronoSettingsComponent = new CodeChronoSettingsComponent();
        return codeChronoSettingsComponent.getPanel();
    }

    /**
     * 检查设置是否被修改。
     * @return
     */
    @Override
    public boolean isModified() {
        CodeChronoSettings.State state =
                Objects.requireNonNull(CodeChronoSettings.getInstance().getState());
        return !codeChronoSettingsComponent.isDefault(state);
    }

    @Override
    public void apply() {
        CodeChronoSettings.State state =
                Objects.requireNonNull(CodeChronoSettings.getInstance().getState());
        codeChronoSettingsComponent.apply(state);

    }

    @Override
    public void reset() {
        CodeChronoSettings.State state =
                Objects.requireNonNull(CodeChronoSettings.getInstance().getState());
        codeChronoSettingsComponent.reset(state);

    }

    @Override
    public void disposeUIResources() {
        codeChronoSettingsComponent = null;
    }

}