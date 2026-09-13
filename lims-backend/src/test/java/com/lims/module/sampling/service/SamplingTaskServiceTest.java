package com.lims.module.sampling.service;

import com.lims.common.core.BusinessException;
import com.lims.common.security.TestLogin;
import com.lims.module.entrust.mapper.EntrustOrderMapper;
import com.lims.module.sampling.dto.TaskDetailVO;
import com.lims.module.sampling.entity.SamplingPlan;
import com.lims.module.sampling.entity.SamplingTask;
import com.lims.module.sampling.mapper.SamplingPlanMapper;
import com.lims.module.sampling.mapper.SamplingTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 移动端任务详情: 仅任务采样员本人(或管理员)可读, 防止改ID越权读取。
 */
@ExtendWith(MockitoExtension.class)
class SamplingTaskServiceTest {

    @Mock
    SamplingTaskMapper taskMapper;
    @Mock
    SamplingPlanMapper planMapper;
    @Mock
    SamplingPlanService planService;
    @Mock
    EntrustOrderMapper orderMapper;
    @Mock
    FieldSampleService fieldSampleService;

    SamplingTaskService service;

    @BeforeEach
    void setUp() {
        service = new SamplingTaskService(taskMapper, planMapper, planService, orderMapper, fieldSampleService);
    }

    @AfterEach
    void tearDown() {
        TestLogin.clear();
    }

    private SamplingTask taskOf(Long assigneeId) {
        SamplingTask t = new SamplingTask();
        t.setId(100L);
        t.setCode("CYRW2026-0001");
        t.setPlanId(10L);
        t.setOrderId(200L);
        t.setAssigneeId(assigneeId);
        t.setAssigneeName("钱采样员");
        t.setStatus(SamplingStatus.TASK_ASSIGNED);
        return t;
    }

    private void stubFullBundle() {
        SamplingPlan plan = new SamplingPlan();
        plan.setId(10L);
        when(planService.mustGet(10L)).thenReturn(plan);
        when(planService.points(10L)).thenReturn(Collections.emptyList());
        when(planService.items(10L)).thenReturn(Collections.emptyList());
        when(planService.equipments(10L)).thenReturn(Collections.emptyList());
        when(fieldSampleService.listVOByTask(100L)).thenReturn(Collections.emptyList());
    }

    @Test
    void mobileDetail_rejectsNonAssignee() {
        TestLogin.loginAs(99L, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(6L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.mobileDetail(100L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void mobileDetail_assigneeGetsFullBundle() {
        TestLogin.loginAs(6L, "ROLE_SAMPLER");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(6L));
        stubFullBundle();

        TaskDetailVO vo = service.mobileDetail(100L);

        assertNotNull(vo);
        assertEquals(100L, vo.getTask().getId());
        assertEquals(10L, vo.getPlan().getId());
    }

    @Test
    void mobileDetail_adminGetsFullBundle() {
        TestLogin.loginAs(1L, "ROLE_ADMIN");
        when(taskMapper.selectById(100L)).thenReturn(taskOf(6L));
        stubFullBundle();

        TaskDetailVO vo = service.mobileDetail(100L);

        assertNotNull(vo);
        assertEquals(100L, vo.getTask().getId());
    }
}
