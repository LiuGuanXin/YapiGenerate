package com.plugin.demo.event;

import com.intellij.util.messages.Topic;
import com.plugin.demo.listener.CodeAideListener;

/**
 * @author Liu Guangxin
 * @date 2025/2/17 9:03
 */
public class AnalysisTopics {

    public static final Topic<CodeAideListener> SHOW_TOPIC = new Topic<>("show.topic", CodeAideListener.class);

}