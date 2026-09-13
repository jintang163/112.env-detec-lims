package com.lims.module.sampling.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import com.lims.common.security.SecurityUtils;
import com.lims.common.storage.FileService;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.entrust.service.EntrustOrderService;
import com.lims.module.sampling.dto.SampleSubmitDTO;
import com.lims.module.sampling.dto.SampleVO;
import com.lims.module.sampling.entity.FieldSample;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.mapper.FieldSampleMapper;
import com.lims.module.sampling.mapper.SamplingTaskMapper;
import com.lims.module.system.entity.SysFile;
import com.lims.module.system.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 现场样品: 幂等提交(clientUuid)、照片晚绑定、扫码核验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldSampleService {

    public static final String BIZ_SAMPLE_PHOTO = "SAMPLE_PHOTO";
    private static final DateTimeFormatter CODE_TS = DateTimeFormatter.ofPattern("HHmmss");

    private final FieldSampleMapper sampleMapper;
    private final SamplingTaskMapper taskMapper;
    private final EntrustOrderMapper orderMapper;
    private final EntrustOrderService entrustOrderService;
    private final SysFileMapper fileMapper;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    /**
     * 提交(或离线重传)样品。同 clientUuid 直接返回已有样品, 保证幂等。
     */
    @Transactional
    public FieldSample submit(SampleSubmitDTO dto) {
        SamplingTask task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException("采样任务不存在");
        }
        // 任务归属: 仅任务采样员本人(或管理员)可写入, 防止越权向他人任务提交样品
        SecurityUtils.checkOwnerOrAdmin(task.getAssigneeId(), "仅任务采样员可提交该任务的样品");
        if (SamplingStatus.TASK_HANDED.equals(task.getStatus()) || SamplingStatus.TASK_CANCELLED.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "任务已交接/取消, 不能再提交样品");
        }

        FieldSample exist = sampleMapper.selectOne(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getClientUuid, dto.getClientUuid()).last("LIMIT 1"));
        if (exist != null) {
            bindPhotos(exist.getId(), dto.getPhotoFileIds());
            return exist;
        }

        FieldSample s = new FieldSample();
        org.springframework.beans.BeanUtils.copyProperties(dto, s,
                "clientUuid", "taskId", "params", "photoFileIds");
        s.setClientUuid(dto.getClientUuid());
        s.setTaskId(task.getId());
        s.setPlanId(task.getPlanId());
        s.setOrderId(task.getOrderId());
        s.setIsQc(dto.getIsQc() == null ? 0 : dto.getIsQc());
        if (s.getSamplingTime() == null) {
            s.setSamplingTime(LocalDateTime.now());
        }
        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            try {
                s.setParamsJson(objectMapper.writeValueAsString(dto.getParams()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("现场参数格式错误");
            }
        }
        s.setStatus(SamplingStatus.SAMPLE_COLLECTED);
        s.setSamplerId(task.getAssigneeId());
        s.setSamplerName(task.getAssigneeName());
        s.setSampleCode(generateCode(task, dto.getSampleCode()));
        sampleMapper.insert(s);

        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            // 首件样品采集: 委托单 ACCEPTED -> SAMPLING
            EntrustOrder order = orderMapper.selectById(task.getOrderId());
            if (order != null && "ACCEPTED".equals(order.getStatus())) {
                entrustOrderService.progress(order.getId(), "START_SAMPLING",
                        "采样任务 " + task.getCode() + " 首件样品采集");
            }
        }
        bindPhotos(s.getId(), dto.getPhotoFileIds());
        return s;
    }

    /** 样品编号: 优先用端上建议号, 冲突时追加时分秒后缀 */
    private String generateCode(SamplingTask task, String suggested) {
        String base = StringUtils.hasText(suggested) ? suggested.trim()
                : task.getCode() + "-" + String.format("%02d", countByTask(task.getId()) + 1);
        if (sampleMapper.selectCount(Wrappers.<FieldSample>lambdaQuery().eq(FieldSample::getSampleCode, base)) == 0) {
            return base;
        }
        String fallback = base + "-" + LocalDateTime.now().format(CODE_TS);
        int n = 1;
        while (sampleMapper.selectCount(Wrappers.<FieldSample>lambdaQuery().eq(FieldSample::getSampleCode, fallback)) > 0) {
            fallback = base + "-" + LocalDateTime.now().format(CODE_TS) + n++;
        }
        return fallback;
    }

    private void bindPhotos(Long sampleId, List<Long> photoFileIds) {
        if (photoFileIds == null) {
            return;
        }
        for (Long fid : photoFileIds) {
            if (fid != null) {
                fileService.bindBiz(fid, BIZ_SAMPLE_PHOTO, sampleId);
            }
        }
    }

    public long countByTask(Long taskId) {
        Long c = sampleMapper.selectCount(Wrappers.<FieldSample>lambdaQuery().eq(FieldSample::getTaskId, taskId));
        return c == null ? 0 : c;
    }

    public List<FieldSample> listByTask(Long taskId) {
        return sampleMapper.selectList(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getTaskId, taskId).orderByAsc(FieldSample::getId));
    }

    public List<SampleVO> listVOByTask(Long taskId) {
        return wrapPhotos(listByTask(taskId));
    }

    public List<SampleVO> wrapPhotos(List<FieldSample> samples) {
        if (samples.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = samples.stream().map(FieldSample::getId).collect(Collectors.toList());
        List<SysFile> photos = fileMapper.selectList(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getBizType, BIZ_SAMPLE_PHOTO)
                .in(SysFile::getBizId, ids)
                .orderByAsc(SysFile::getId));
        return samples.stream().map(s -> {
            SampleVO vo = new SampleVO();
            vo.setSample(s);
            vo.setPhotos(photos.stream().filter(f -> s.getId().equals(f.getBizId())).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    public FieldSample getByCode(String code) {
        return sampleMapper.selectOne(Wrappers.<FieldSample>lambdaQuery()
                .eq(FieldSample::getSampleCode, code).last("LIMIT 1"));
    }

    public SampleVO getVOByCode(String code) {
        FieldSample s = getByCode(code);
        if (s == null) {
            return null;
        }
        // 扫码核验: 样品管理员接收时可查任意样品, 其余登录用户仅限本人所采(或管理员)
        if (!SecurityUtils.hasRole("ROLE_SAMPLE_MANAGER")) {
            SecurityUtils.checkOwnerOrAdmin(s.getSamplerId(), "仅任务采样员可查看该样品");
        }
        List<SampleVO> v = wrapPhotos(Collections.singletonList(s));
        return v.isEmpty() ? null : v.get(0);
    }
    @Transactional
    public void markTaskSubmitted(Long taskId, String remark) {
        SamplingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SecurityUtils.checkOwnerOrAdmin(task.getAssigneeId(), "仅任务采样员可提交该任务");
        if (!SamplingStatus.TASK_ASSIGNED.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_NOT_ALLOWED.getCode(), "仅已分配任务可标记完成");
        }
        task.setStatus(SamplingStatus.TASK_SUBMITTED);
        task.setSubmittedAt(LocalDateTime.now());
        if (StringUtils.hasText(remark)) {
            task.setRemark(remark);
        }
        taskMapper.updateById(task);
    }

    @Transactional
    public void markDownloaded(Long taskId) {
        SamplingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SecurityUtils.checkOwnerOrAdmin(task.getAssigneeId(), "仅任务采样员可下载该任务");
        if (task.getDownloadedAt() == null) {
            task.setDownloadedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }
}
