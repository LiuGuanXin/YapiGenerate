package com.plugin.demo.model;

import java.util.Map;

/**
 * 表示字段的类型信息，包括类型、描述和属性（如果是对象类型）。
 * @author Liu Guangxin
 * @date 2025/2/19 14:49
 */
public class FieldTypeInfo {
    private String type;
    private String description;
    private Map<String, Object> properties;

    public FieldTypeInfo(String type, String description, Map<String, Object> properties) {
        this.type = type;
        this.description = description;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
