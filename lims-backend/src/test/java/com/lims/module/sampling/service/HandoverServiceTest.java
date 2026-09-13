package com.lims.module.sampling.service;

import com.lims.common.core.BusinessException;
import com.lims.common.security.TestLogin;
import com.lims.common.util.CodeGenerator;
import com.lims.common.ws.NotifyService;
import com.lims.module.entrust.entity.EntrustOrder;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.entrust.service.EntrustOrderService;
import com.lims.module.sampling.dto.HandoverCreateDTO;
import com.lims.module.sampling.entity.FieldSample;
import com.lims.module.sampling.entity.SampleHandover;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.mapper.FieldSampleMapper;
import com.lims.module.sampling.mapper.SampleHandoverMapper;
import com.lims.module.sampling.mapper.SamplingPlanMapper;
import com.lims.module.sampling.mapper.SamplingTaskMapper;
import com.lims.module.system.mapper.SysFileMapper;
import com.lims.module.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 样品交接: 发起归属校验、重复发起拦截、接收/拒收状态流转。
 */
@ExtendWith(MockitoExtension.class)
class HandoverServiceTest {

    private static final Long SAMPLER = 6L;
    private static final Long OTHER = 99L;
    private static final Long RECEIVER = 7L;

    @Mock
    SampleHandoverMapper handoverMapper;
    @Mock
    SamplingTaskMapper taskMapper;
    @Mock
    SamplingPlanMapper planMapper;
    @Mock
    EntrustOrderMapper orderMapper;
    @Mock
    FieldSampleMapper sampleMapper;
    @Mock
    SysFileMapper fileMapper;
    @Mock
    SysUserMapper sysUserMapper;
    @Mock
    FieldSampleService fieldSampleService;
    @Mock
    EntrustOrderService entrustOrderService;
    @Mock
    CodeGenerator codeGenerator;
    @Mock
    NotifyService notifyService;

    HandoverService service;

    @BeforeEach
    void setUp() {
        service = new HandoverService(handoverMapper, taskMapper, planMapper, orderMapper,
                sampleMapper, fileMapper, sysUserMapper, fieldSampleService,
                entrustOrderService, codeGenerator, notifyService);
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

    private FieldSample collectedSample(Long id) {
        FieldSample s = new FieldSample();
        s.setId(id);
        s.setTaskId(100L);
        s.setStatus(SamplingStatus.SAMPLE_COLLECTED);
        return s;
    }

    private HandoverCreateDTO dtoOf(Long taskId) {
        HandoverCreateDTO d = new HandoverCreateDTO();
        d.setTaskId(taskId);
        return d;
    }

    // ---------------- 发起交接 ----------------

    @Test
    void create_rejectsNonAssignee() {
        TestLogin.loginAs(OTHER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dtoOf(100L)));
        assertEquals(403, ex.getCode());
        verify(handoverMapper, never()).insert(any());
    }

    @Test
    void create_rejectsWhenNoCollectedSamples() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED));
        when(sampleMapper.selectList(any())).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dtoOf(100L)));
        assertTrue(ex.getMessage().contains("没有可交接"));
        verify(handoverMapper, never()).insert(any());
    }

    @Test
    void create_rejectsDuplicatePendingHandover() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED));
        when(sampleMapper.selectList(any())).thenReturn(Collections.singletonList(collectedSample(1L)));
        when(handoverMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dtoOf(100L)));
        assertTrue(ex.getMessage().contains("请勿重复发起"));
        verify(handoverMapper, never()).insert(any());
    }

    @Test
    void create_assigneeCreatesPendingHandover() {
        TestLogin.loginAs(SAMPLER, "ROLE_SAMPLER");
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED);
        when(taskMapper.selectById(100L)).thenReturn(task);
        FieldSample s1 = collectedSample(1L);
        FieldSample s2 = collectedSample(2L);
        when(sampleMapper.selectList(any())).thenReturn(Arrays.asList(s1, s2));
        when(handoverMapper.selectCount(any())).thenReturn(0L);
        when(codeGenerator.next("JJ", false)).thenReturn("JJ20260913-0001");
        when(handoverMapper.insert(any())).thenAnswer(inv -> {
            ((SampleHandover) inv.getArgument(0)).setId(500L);
            return 1;
        });
        when(sysUserMapper.selectUserIdsByRole("ROLE_SAMPLE_MANAGER"))
                .thenReturn(Collections.singletonList(RECEIVER));

        Long id = service.create(dtoOf(100L));

        assertEquals(500L, id);
        ArgumentCaptor<SampleHandover> cap = ArgumentCaptor.forClass(SampleHandover.class);
        verify(handoverMapper).insert(cap.capture());
        SampleHandover h = cap.getValue();
        assertEquals(SamplingStatus.HANDOVER_PENDING, h.getStatus());
        assertEquals(2, h.getSampleCount());
        assertEquals(SAMPLER, h.getHandoverById());
        assertEquals("JJ20260913-0001", h.getCode());
        // 样品挂上交接单, 任务进入已提交待交接
        assertEquals(500L, s1.getHandoverId());
        assertEquals(500L, s2.getHandoverId());
        assertEquals(SamplingStatus.TASK_SUBMITTED, task.getStatus());
        verify(sampleMapper, times(2)).updateById(any());
        verify(notifyService).pushTodo(eq(RECEIVER), anyString(), anyString(), eq("SAMPLE_HANDOVER"), eq(500L));
    }

    // ---------------- 接收确认 ----------------

    @Test
    void confirm_rejectsNonPending() {
        TestLogin.loginAs(RECEIVER, "ROLE_SAMPLE_MANAGER");
        SampleHandover h = new SampleHandover();
        h.setId(500L);
        h.setStatus(SamplingStatus.HANDOVER_CONFIRMED);
        when(handoverMapper.selectById(500L)).thenReturn(h);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(500L, null));
        assertTrue(ex.getMessage().contains("仅待接收"));
    }

    @Test
    void confirm_receiverConfirmsAndProgresses() {
        TestLogin.loginAs(RECEIVER, "ROLE_SAMPLE_MANAGER");
        SampleHandover h = new SampleHandover();
        h.setId(500L);
        h.setTaskId(100L);
        h.setOrderId(200L);
        h.setHandoverById(SAMPLER);
        h.setStatus(SamplingStatus.HANDOVER_PENDING);
        when(handoverMapper.selectById(500L)).thenReturn(h);
        FieldSample s1 = collectedSample(1L);
        s1.setHandoverId(500L);
        when(sampleMapper.selectList(any())).thenReturn(Collections.singletonList(s1));
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED);
        when(taskMapper.selectById(100L)).thenReturn(task);
        EntrustOrder order = new EntrustOrder();
        order.setId(200L);
        order.setStatus("SAMPLING");
        when(orderMapper.selectById(200L)).thenReturn(order);

        service.confirm(500L, "核对无误");

        assertEquals(SamplingStatus.HANDOVER_CONFIRMED, h.getStatus());
        assertEquals(RECEIVER, h.getReceiverId());
        assertNotNull(h.getReceiveAt());
        assertEquals(SamplingStatus.SAMPLE_RECEIVED, s1.getStatus());
        assertEquals(SamplingStatus.TASK_HANDED, task.getStatus());
        // 委托单 SAMPLING -> TESTING
        verify(entrustOrderService).progress(eq(200L), eq("START_TESTING"), anyString());
        verify(notifyService).push(eq(SAMPLER), anyString(), anyString(), eq("SAMPLE_HANDOVER"), eq(500L));
    }

    // ---------------- 拒收 ----------------

    @Test
    void reject_releasesSamplesAndRevertsTask() {
        TestLogin.loginAs(RECEIVER, "ROLE_SAMPLE_MANAGER");
        SampleHandover h = new SampleHandover();
        h.setId(500L);
        h.setTaskId(100L);
        h.setOrderId(200L);
        h.setHandoverById(SAMPLER);
        h.setStatus(SamplingStatus.HANDOVER_PENDING);
        when(handoverMapper.selectById(500L)).thenReturn(h);
        FieldSample s1 = collectedSample(1L);
        s1.setHandoverId(500L);
        List<FieldSample> samples = Collections.singletonList(s1);
        when(sampleMapper.selectList(any())).thenReturn(samples);
        SamplingTask task = taskOf(SAMPLER, SamplingStatus.TASK_SUBMITTED);
        when(taskMapper.selectById(100L)).thenReturn(task);

        service.reject(500L, "包装破损");

        assertEquals(SamplingStatus.HANDOVER_REJECTED, h.getStatus());
        assertNull(s1.getHandoverId());
        assertEquals(SamplingStatus.TASK_ASSIGNED, task.getStatus());
        verify(notifyService).push(eq(SAMPLER), anyString(), anyString(), eq("SAMPLE_HANDOVER"), eq(500L));
    }
}
