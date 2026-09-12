package com.lims.common.storage;

import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 对象存储。配置 lims.storage.type=minio 启用。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "lims.storage.type", havingValue = "minio")
public class MinioFileStorage implements FileStorage {

    private final StorageProperties props;
    private MinioClient client;

    public MinioFileStorage(StorageProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("MinIO bucket 已创建: {}", props.getBucket());
            }
        } catch (Exception e) {
            log.warn("MinIO 初始化检查失败(将在首次上传时重试): {}", e.getMessage());
        }
    }

    @Override
    public StoredFile upload(MultipartFile file, String bizDir) {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String objectName = (bizDir == null ? "common" : bizDir) + "/" + dateDir + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .contentType(file.getContentType())
                    .stream(in, file.getSize(), -1)
                    .build());
            String url = props.getPublicUrl() + "/" + props.getBucket() + "/" + objectName;
            return new StoredFile(objectName, url, file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("MinIO 上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public StoredFile uploadBytes(String objectName, byte[] bytes, String contentType) {
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .contentType(contentType)
                    .stream(in, bytes.length, -1)
                    .build());
            String url = props.getPublicUrl() + "/" + props.getBucket() + "/" + objectName;
            return new StoredFile(objectName, url, bytes.length, contentType);
        } catch (Exception e) {
            log.error("MinIO 字节写入失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public InputStream download(String objectName) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket()).object(objectName).build());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket()).object(objectName).build());
        } catch (Exception e) {
            log.warn("MinIO 删除失败 {}: {}", objectName, e.getMessage());
        }
    }
}
