package com.lims.common.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoredFile {
    private String objectName;
    private String url;
    private long size;
    private String contentType;
}
