package com.lims.module.contract.service;

import cn.hutool.crypto.SecureUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OnlyOffice Document Server 编辑配置生成。
 * 前端 OnlyOffice Docs API 用该配置初始化编辑器:
 *   new DocsAPI.DocEditor("placeholder", config)
 * 文档保存由 OnlyOffice 通过 callbackUrl(status 2) 回调 /onlyoffice/callback/{bizType}/{bizId}。
 */
@Component
public class OnlyOfficeService {

    @Value("${lims.onlyoffice.doc-server-url}")
    private String docServerUrl;
    @Value("${lims.onlyoffice.api-callback-url}")
    private String apiCallbackUrl;

    public Map<String, Object> editorConfig(String bizType, Long bizId, String fileUrl,
                                            String fileName, String user, boolean edit) {
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "docx";
        String documentType = "word";
        String key = bizType + "-" + bizId + "-" + SecureUtil.md5(fileUrl + fileName).substring(0, 8);

        Map<String, Object> doc = new HashMap<>();
        doc.put("fileType", ext);
        doc.put("key", key);
        doc.put("title", fileName);
        doc.put("url", fileUrl.startsWith("http") ? fileUrl : apiCallbackUrl + fileUrl);
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("edit", edit);
        permissions.put("download", true);
        permissions.put("print", true);
        doc.put("permissions", permissions);

        Map<String, Object> userConf = new HashMap<>();
        userConf.put("id", user);
        userConf.put("name", user);

        Map<String, Object> conf = new HashMap<>();
        conf.put("mode", edit ? "edit" : "view");
        conf.put("lang", "zh-CN");
        conf.put("callbackUrl", apiCallbackUrl + "/onlyoffice/callback/" + bizType + "/" + bizId + "?key=" + key);
        conf.put("user", userConf);

        Map<String, Object> config = new HashMap<>();
        config.put("documentType", documentType);
        config.put("document", doc);
        config.put("editorConfig", conf);
        config.put("height", "100%");
        config.put("width", "100%");
        return config;
    }

    @Data
    public static class Callback {
        private Integer status;
        private String url;
        private String key;
        private java.util.List<Map<String, Object>> users;
    }
}
