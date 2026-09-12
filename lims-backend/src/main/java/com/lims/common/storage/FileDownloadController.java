package com.lims.common.storage;

import com.lims.module.system.entity.SysFile;
import com.lims.module.system.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileService fileService;
    private final FileStorage storage;

    /** 按文件台账ID下载(带权限, 走认证) */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws Exception {
        SysFile f = fileService.get(id);
        if (f == null) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = fileService.downloadStream(f)) {
            return build(in.readAllBytes(), f.getOriginalName(), f.getContentType());
        }
    }

    /** 本地存储的相对路径直读(供 OnlyOffice 文档服务器等白名单场景取文件) */
    @GetMapping("/local/{objectName:.+}")
    public ResponseEntity<byte[]> local(@PathVariable String objectName) throws Exception {
        try (InputStream in = storage.download(objectName)) {
            String name = objectName.substring(objectName.lastIndexOf('/') + 1);
            return build(in.readAllBytes(), name, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }
    }

    private ResponseEntity<byte[]> build(byte[] bytes, String name, String contentType) {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .body(bytes);
    }
}
