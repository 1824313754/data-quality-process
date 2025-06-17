package org.battery.quality.model;

import java.io.Serializable;

/**
 * 规则信息类，用于在Flink节点间传输规则信息，避免直接序列化规则对象
 * 解决动态编译生成类的序列化问题
 */
public class RuleInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;           // 规则ID
    private String name;         // 规则类名
    private String sourceCode;   // 规则源代码
    private int version;         // 规则版本
    private String md5Hash;      // 规则MD5哈希
    
    public RuleInfo() {
    }
    
    public RuleInfo(String id, String name, String sourceCode, int version, String md5Hash) {
        this.id = id;
        this.name = name;
        this.sourceCode = sourceCode;
        this.version = version;
        this.md5Hash = md5Hash;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSourceCode() {
        return sourceCode;
    }
    
    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getMd5Hash() {
        return md5Hash;
    }
    
    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }
    
    @Override
    public String toString() {
        return "RuleInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", md5Hash='" + md5Hash + '\'' +
                '}';
    }
} 