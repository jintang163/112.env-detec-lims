package com.lims.common.storage;

import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储(默认): 文件写入 lims.storage.local-dir, 通过 /files/** 下载。
 * 适合演示/内网; 生产可切换 lims.storage.type=minio。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lims.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final StorageProperties props;

    @Override
    public StoredFile upload(MultipartFile file, String bizDir) {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String safeName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.')) : "";
        String objectName = (bizDir == null ? "common" : bizDir) + "/" + dateDir + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path target = Paths.get(props.getLocalDir(), objectName);
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = "/files/local/" + objectName;
            return new StoredFile(objectName, url, file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("本地文件保存失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public StoredFile uploadBytes(String objectName, byte[] bytes, String contentType) {
        try {
            Path target = Paths.get(props.getLocalDir(), objectName);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new StoredFile(objectName, "/files/local/" + objectName, bytes.length, contentType);
        } catch (IOException e) {
            log.error("本地文件写入失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public InputStream download(String objectName) {
        try {
            return Files.newInputStream(Paths.get(props.getLocalDir(), objectName));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            Files.deleteIfExists(Paths.get(props.getLocalDir(), objectName));
        } catch (IOException e) {
            log.warn("删除本地文件失败 {}: {}", objectName, e.getMessage());
        }
    }
}
