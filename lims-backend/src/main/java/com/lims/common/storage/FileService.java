package com.lims.common.storage;

import com.lims.module.system.entity.SysFile;
import com.lims.module.system.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件门面: 上传到具体存储实现 + 登记 sys_file 台账
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileStorage storage;
    private final SysFileMapper fileMapper;

    public SysFile upload(MultipartFile file, String bizType, Long bizId) {
        StoredFile stored = storage.upload(file, bizType == null ? "common" : bizType.toLowerCase());
        SysFile sf = new SysFile();
        sf.setBizType(bizType);
        sf.setBizId(bizId);
        sf.setOriginalName(file.getOriginalFilename());
        sf.setObjectName(stored.getObjectName());
        sf.setUrl(stored.getUrl());
        sf.setContentType(stored.getContentType());
        sf.setFileSize(stored.getSize());
        fileMapper.insert(sf);
        return sf;
    }

    public void bindBiz(Long fileId, String bizType, Long bizId) {
        SysFile f = fileMapper.selectById(fileId);
        if (f != null) {
            f.setBizType(bizType);
            f.setBizId(bizId);
            fileMapper.updateById(f);
        }
    }

    public SysFile get(Long id) {
        return fileMapper.selectById(id);
    }

    public java.io.InputStream downloadStream(SysFile file) {
        return storage.download(file.getObjectName());
    }
}
