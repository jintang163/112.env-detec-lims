package com.lims.module.contract.controller;

import com.lims.common.core.Result;
import com.lims.common.storage.FileStorage;
import com.lims.module.contract.entity.Contract;
import com.lims.module.contract.mapper.ContractMapper;
import com.lims.module.contract.service.OnlyOfficeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.Map;

/**
 * OnlyOffice 文档服务器回调(白名单免鉴权)。
 * status: 1正在编辑 2就绪可保存 4最后保存关闭 6强制保存 7强制保存出错
 */
@Slf4j
@RestController
@RequestMapping("/onlyoffice")
@RequiredArgsConstructor
public class OnlyOfficeCallbackController {

    private final ContractMapper contractMapper;
    private final FileStorage storage;
    private final OnlyOfficeService onlyOfficeService;

    @PostMapping("/callback/{bizType}/{bizId}")
    public Map<String, Object> callback(@PathVariable String bizType,
                                        @PathVariable Long bizId,
                                        @RequestBody OnlyOfficeService.Callback body) {
        log.info("OnlyOffice 回调 bizType={} bizId={} status={}", bizType, bizId, body.getStatus());
        int status = body.getStatus() == null ? 0 : body.getStatus();
        if ((status == 2 || status == 6) && body.getUrl() != null) {
            try (java.io.InputStream in = new URL(body.getUrl()).openStream()) {
                byte[] bytes = in.readAllBytes();
                String objectName = "contract/saved/" + bizType.toLowerCase() + "-" + bizId + ".docx";
                storage.uploadBytes(objectName, bytes,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                if ("CONTRACT".equals(bizType)) {
                    Contract c = contractMapper.selectById(bizId);
                    if (c != null) {
                        c.setFileUrl("/files/local/" + objectName);
                        contractMapper.updateById(c);
                    }
                }
            } catch (Exception e) {
                log.error("OnlyOffice 保存回调失败", e);
                return Map.of("error", 1);
            }
        }
        return Map.of("error", 0);
    }

    /** 获取编辑器配置 */
    @GetMapping("/editor-config/{bizType}/{bizId}")
    public Result<Map<String, Object>> editorConfig(@PathVariable String bizType,
                                                    @PathVariable Long bizId,
                                                    @RequestParam(defaultValue = "true") boolean edit) {
        Contract c = contractMapper.selectById(bizId);
        if (c == null || c.getFileUrl() == null) {
            return Result.fail("文档不存在或尚未上传合同正文");
        }
        String fileName = c.getCode() + "-合同.docx";
        return Result.ok(onlyOfficeService.editorConfig(bizType, bizId, c.getFileUrl(),
                fileName, "当前用户", edit));
    }

    @Data
    public static class EmptyBody {}
}
