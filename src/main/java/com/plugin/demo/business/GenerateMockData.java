package com.plugin.demo.business;

import com.plugin.demo.setting.CodeChronoSettings;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.lang.System;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

/**
 * @author Liu Guangxin
 * @date 2025/2/28 10:49
 */
public class GenerateMockData {


    private static final String PROMPT = """
            我会给你一段json，这个json的包含字段和和字段的类型，
            key为字段名称，value为字段的类型。你需要根据字段名称和类型在josn中
            为每个字段添加属性填写模拟数据，使用模拟数据替换掉原来的类型
            （注意是在原有的json上替换掉后面的类型而不是新加一个类型字段type来填充数据），
            你需要十分注意生成模拟数据的类型和给出的类型是否一致。
            不要更改json的其他内容，你只需要替换模拟数据。模拟数据需要接近真实的情况，
            id相关字段为uuid（没有-相连），realName是中国人的名字，message使用success、status为0。
            如果存在identificationCode字段它的格式格式例如：
            88.163.12/3010012213405MA3D2XXA202106000008。
            示例：
            {
              "data":{
                "realName":"string",
                "password":"string"
              },
              "message":"string",
              "status":"number"
            }
            转换为：
            {
              "data":{
                "realName":"李煜",
                "password":"nantang666"
              },
              "message":"success",
              "status": 0
            }
            你只需要返回json的内容，不需要返回其他一切内容。json内容如下：""";

    public static void main(String[] args) {
        String json = """
            {
              "data":{
                "realName":"string",
                "password":"string"
              },
              "message":"string",
              "status":"number"
            }
            """;

        // 阿里云
//        try {
//            String result = callWithMessage(json,
//                    state.aLiApiKey, "qwen-max-latest");
//            System.out.println(result);
//        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
//            System.err.println("错误信息："+e.getMessage());
//        }

        // 智谱
//        ClientV4 client = new ClientV4.Builder(state.apiKey)
//                    .networkConfig(30, 30, 30, 30, TimeUnit.MINUTES)
//                    .build();
//        ChatMessage chatMessage = sendChatMessage(
//                client,
//                new ChatMessage(ChatMessageRole.SYSTEM.value(), PROMPT),
//                new ChatMessage(ChatMessageRole.USER.value(), json));
//        System.out.println(chatMessage.getContent());
    }

    public static String generateMock(String json) {
        CodeChronoSettings.State state = Objects.requireNonNull(CodeChronoSettings.getInstance().getState());
        if (state.type == 1) {
            try {
                return aLiGenerate(state, json);
            } catch (Exception e) {
                return "2";
            }
        } else {
            return zhiPuGenerate(state, json);
        }
    }



    private static String zhiPuGenerate(CodeChronoSettings.State state, String json) {
        if (state.zhiPuApiKey == null || state.zhiPuApiKey.isEmpty()) {
            System.out.println("请在设置中配置API_KEY");
            return "1";
        }
        ClientV4 client;
        try {
            client = new ClientV4.Builder(state.zhiPuApiKey)
                    .networkConfig(30, 30, 30, 30, TimeUnit.MINUTES)
                    .build();
        } catch (Exception e) {
            return "2";
        }
        // Constants.ModelChatGLM4Plus
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(state.zhiPuModelName)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(List.of(new ChatMessage(ChatMessageRole.SYSTEM.value(), PROMPT),
                        new ChatMessage(ChatMessageRole.USER.value(), json)))
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        ChatMessage message = null;
        if (invokeModelApiResp.isSuccess()) {
            ModelData modelData = invokeModelApiResp.getData();
            if (modelData != null) {
                List<Choice> choices = modelData.getChoices();
                message = choices.get(0).getMessage();
            }
        } else {
            System.out.println(invokeModelApiResp.getMsg());
        }
        return message.getContent().toString();
    }

    public static String aLiGenerate(CodeChronoSettings.State state, String userMessage) throws ApiException, NoApiKeyException, InputRequiredException {
        String apiKey = state.aLiApiKey;
        String modelName = state.aLiModelName;
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(PROMPT)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userMessage)
                .build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                .model(modelName)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        return gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
    }
}
