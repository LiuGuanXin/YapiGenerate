package com.plugin.demo.listener;

/**
 * @author Liu Guangxin
 * @date 2025/2/17 8:53
 */
public interface CodeAideListener {

    /**
     * 获取代码内容
     * @param content 代码内容
     */
    void getCodeContent(String content);
}