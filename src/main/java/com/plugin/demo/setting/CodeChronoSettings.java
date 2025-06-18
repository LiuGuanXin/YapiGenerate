package com.plugin.demo.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

/**
 * 如果没有使用轻型服务 @Service，则必须在plugin.xml文件中将持久化数据类声明为ServiceEP
 * @author Codechrono
 * 持久化插件配置文件
 */
@State(
        name = "yapi-settings",
        storages = @Storage("yapi-settings.xml")
)
@Service(Service.Level.APP)
public final class CodeChronoSettings implements PersistentStateComponent<CodeChronoSettings.State> {

    public static class State {
        public String zhiPuApiKey = "82da19100cd0433d827c52c16771a251.4qqMYcNBx5WyLn92";
        public String aLiApiKey = "82da19100cd0433d827c52c16771a251.4qqMYcNBx5WyLn92";
        public String aLiModelName = "qwen-max";
        public String zhiPuModelName = "glm-4-plus";
        public Integer type = 0;
//        public String userName;
//        public String password;
//        public String groupName;
//        public String projectName;
//        public String categoryName;
//        public String url;
        public String userName = "xxxxx@qq.com";
        public String password = "xxxxx";
        public String groupName = "仪表平台项目组";
        public String projectName = "数典知识库";
        public String categoryName = "公共分类";
        public String url = "http://192.168.99.206:3000";
    }

    private State myState = new State();

    public static CodeChronoSettings getInstance() {
        return ApplicationManager.getApplication().getService(CodeChronoSettings.class);
    }

    /**
     * 点击OK的时候会执行该方法，用于获取当前的状态
     * 该方法返回的状态会被保存到对应的文件中
     * @return
     */
    @Override
    public State getState() {
        return myState;
    }

    /**
     * 该方法会在插件启动的时候执行，用于加载状态
     */
    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

}