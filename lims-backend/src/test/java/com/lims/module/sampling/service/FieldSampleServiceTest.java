package com.lims.module.sampling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lims.common.core.BusinessException;
import com.lims.common.security.TestLogin;
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
import com.lims.module.system.mapper.SysFileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 现场样品服务: 任务归属校验、离线幂等重复提交、任务提交/下载/扫码越权。
 */
@ExtendWith(MockitoExtension.class)
class FieldSampleServiceTest {

    private static final Long SAMPLER = 6L;
    private static final Long OTHER = 99L;

    @Mock
    FieldSampleMapper sampleMapper;
    @Mock
    SamplingTaskMapper taskMapper;
    @Mock
    EntrustOrderMapper orderMapper;
    @Mock
    EntrustOrderService entrustOrderService;
    @Mock
    SysFileMapper fileMapper;
    @Mock
    FileService fileService;

    FieldSampleService service;

    @BeforeEach
    void setUp() {
        service = new FieldSampleService(sampleMapper, taskMapper, orderMapper,
                entrustOrderService, fileMapper, fileService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TestLogin.clear();
    }

    private SamplingTask taskOf(Long assigneeId, String status) {
        SamplingTask t = new SamplingTask();
        t.setId(100L);
        t.setCode("CYRW2026-0001");
        t.setPlanId(10L);
        t.setOrderId(200L);
        t.setAssigneeId(assigneeId);
        t.setAssigneeName("钱采样员");
        t.setStatus(status);
        return t;
    }

    private SampleSubmitDTO dtoOf(Long taskId, String uuid) {
        SampleSubmitDTO d = new SampleSubmitDTO();
        d.setTaskId(taskId);
        d.setClientUuid(uuid);
        d.setSampleName("地表水");
        d.setItemName("pH");
        return d;
    }

    // ---------------- 样品提交: 归属与幂等 ----------------

    @Test
    void submit_rejectsNonAssignee() {
        TestLogin.loginAs(OTHER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(dtoOf(100L, "u-1")));
        assertEquals(403, ex.getCode());
        verify(sampleMapper, never()).insert(any());
    }

    @Test
    void submit_idempotentReturnsExistingOnDuplicateClientUuid() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED));
        FieldSample exist = new FieldSample();
        exist.setId(555L);
        exist.setClientUuid("u-1");
        when(sampleMapper.selectOne(any())).thenReturn(exist);

        FieldSample r = service.submit(dtoOf(100L, "u-1"));

        assertSame(exist, r);
        verify(sampleMapper, never()).insert(any());
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    void submit_assigneeCreatesSampleAndStartsSampling() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED);
        when(taskMapper.selectById(100L)).thenReturn(task);
        when(sampleMapper.selectOne(any())).thenReturn(null);
        when(sampleMapper.selectCount(any())).thenReturn(0L);
        EntrustOrder order = new EntrustOrder();
        order.setId(200L);
        order.setStatus("ACCEPTED");
        when(orderMapper.selectById(200L)).thenReturn(order);

        FieldSample r = service.submit(dtoOf(100L, "u-1"));

        ArgumentCaptor<FieldSample> cap = ArgumentCaptor.forClass(FieldSample.class);
        verify(sampleMapper).insert(cap.capture());
        FieldSample saved = cap.getValue();
        assertEquals(SamplingStatus.SAMPLE_COLLECTED, saved.getStatus());
        assertEquals(SAMPLER, saved.getSamplerId());
        assertEquals("钱采样员", saved.getSamplerName());
        assertEquals(100L, saved.getTaskId());
        assertEquals("CYRW2026-0001-01", saved.getSampleCode());
        assertSame(saved, r);
        // 首件样品: 任务记录开始时间, 委托单 ACCEPTED -> SAMPLING
        assertNotNull(task.getStartedAt());
        verify(taskMapper).updateById(task);
        verify(entrustOrderService).progress(eq(200L), eq("START_SAMPLING"), contains("CYRW2026-0001"));
    }

    @Test
    void submit_adminMaySubmitForOthers() {
        TestLogin.loginAs(1L, "ROLE_ADMIN");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED));
        when(sampleMapper.selectOne(any())).thenReturn(null);
        when(sampleMapper.selectCount(any())).thenReturn(0L);

        FieldSample r = service.submit(dtoOf(100L, "u-9"));

        verify(sampleMapper).insert(any());
        // 样品责任人仍记录为任务采样员, 不被管理员身份覆盖
        assertEquals(SAMPLER, r.getSamplerId());
    }

    @Test
    void submit_rejectsWhenTaskHanded() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_HANDED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(dtoOf(100L, "u-1")));
        assertEquals(3001, ex.getCode());
        verify(sampleMapper, never()).insert(any());
    }

    // ---------------- 任务提交(标记完成) ----------------

    @Test
    void markTaskSubmitted_rejectsNonAssignee() {
        TestLogin.loginAs(OTHER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.markTaskSubmitted(100L, null));
        assertEquals(403, ex.getCode());
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    void markTaskSubmitted_rejectsWrongStatus() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.markTaskSubmitted(100L, null));
        assertEquals(3001, ex.getCode());
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    void markTaskSubmitted_assigneeSucceeds() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED);
        when(taskMapper.selectById(100L)).thenReturn(task);

        service.markTaskSubmitted(100L, "现场采样完成");

        assertEquals(SamplingStatus.TASK_SUBMITTED, task.getStatus());
        assertNotNull(task.getSubmittedAt());
        assertEquals("现场采样完成", task.getRemark());
        verify(taskMapper).updateById(task);
    }

    // ---------------- 离线下载确认 ----------------

    @Test
    void markDownloaded_rejectsNonAssignee() {
        TestLogin.loginAs(OTHER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.markDownloaded(100L));
        assertEquals(403, ex.getCode());
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    void markDownloaded_firstDownloadStampsTime() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED);
        when(taskMapper.selectById(100L)).thenReturn(task);

        service.markDownloaded(100L);

        assertNotNull(task.getDownloadedAt());
        verify(taskMapper).updateById(task);
    }

    @Test
    void markDownloaded_repeatIsIdempotent() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_ASSIGNED);
        task.setDownloadedAt(LocalDateTime.now().minusHours(1));
        when(taskMapper.selectById(100L)).thenReturn(task);

        service.markDownloaded(100L);

        verify(taskMapper, never()).updateById(any());
    }

    // ---------------- 扫码核验 ----------------

    @Test
    void scanByCode_rejectsNonOwner() {
        TestLogin.loginAs(OTHER, "ROLE_SAMPLER");
        FieldSample s = new FieldSample();
        s.setId(1L);
        s.setSamplerId(SAMPLER);
        when(sampleMapper.selectOne(any())).thenReturn(s);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getVOByCode("CYRW2026-0001-01"));
        assertEquals(403, ex.getCode());
    }

    @Test
    void scanByCode_allowsSampleManager() {
        TestLogin.loginAs(7L, "ROLE_SAMPLE_MANAGER");
        FieldSample s = new FieldSample();
        s.setId(1L);
        s.setSamplerId(SAMPLER);
        when(sampleMapper.selectOne(any())).thenReturn(s);
        when(fileMapper.selectList(any())).thenReturn(Collections.emptyList());

        SampleVO vo = service.getVOByCode("CYRW2026-0001-01");

        assertNotNull(vo);
        assertSame(s, vo.getSample());
    }
}
