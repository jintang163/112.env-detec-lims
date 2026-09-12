package com.lims.common.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorage {

    StoredFile upload(MultipartFile file, String bizDir);

    /** 以指定对象名直接写入字节(OnlyOffice 回调保存等场景) */
    StoredFile uploadBytes(String objectName, byte[] bytes, String contentType);

    InputStream download(String objectName);

    void delete(String objectName);
}
