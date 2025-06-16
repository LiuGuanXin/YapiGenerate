package com.plugin.demo.model;


import com.alibaba.fastjson.annotation.JSONField;


import java.util.ArrayList;
import java.util.List;

/**
 * 字段信息类
 * @author Liu Guangxin
 * @date 2025/2/19 10:18
 */
public class FieldInfo {

    /**
     * 字段名
     */
    @JSONField(serialize = false)
    private String name;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段注释
     */
    private String description;


    /**
     * 子字段信息
     */
    List<FieldInfo> properties = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public List<FieldInfo> getProperties() {
        return properties;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setProperties(List<FieldInfo> properties) {
        this.properties = properties;
    }
}
