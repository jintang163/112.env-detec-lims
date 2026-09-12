package com.lims.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lims.storage")
public class StorageProperties {
    /** local / minio */
    private String type = "local";
    private String localDir = "./lims-files";
    private String bucket = "lims";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String publicUrl;
}
