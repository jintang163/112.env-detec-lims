package com.lims.module.quote.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.core.PageResult;
import com.lims.common.core.Result;
import com.lims.common.storage.FileService;
import com.lims.module.quote.dto.QuoteQueryDTO;
import com.lims.module.quote.dto.QuoteSaveDTO;
import com.lims.module.quote.dto.QuoteVO;
import com.lims.module.quote.entity.PricingRule;
import com.lims.module.quote.entity.QuoteItem;
import com.lims.module.quote.mapper.PricingRuleMapper;
import com.lims.module.quote.service.PricingService;
import com.lims.module.quote.service.QuoteService;
import com.lims.module.quote.service.QuoteWordExporter;
import com.lims.module.system.entity.SysFile;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;
    private final PricingService pricingService;
    private final PricingRuleMapper ruleMapper;
    private final QuoteWordExporter wordExporter;
    private final FileService fileService;

    @GetMapping
    public Result<PageResult<QuoteVO>> page(QuoteQueryDTO query) {
        Page<QuoteVO> page = quoteService.page(query);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<QuoteVO> detail(@PathVariable Long id) {
        return Result.ok(quoteService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_quote:add')")
    public Result<Long> save(@Valid @RequestBody QuoteSaveDTO dto) {
        return Result.ok(quoteService.save(dto));
    }

    /** 实时试算 */
    @PostMapping("/calculate")
    public Result<List<QuoteItem>> calculate(@RequestBody CalcDTO dto) {
        return Result.ok(quoteService.calculate(dto.getItems(),
                dto.getUrgentFactor() == null ? BigDecimal.ONE : dto.getUrgentFactor()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('PERM_quote:add')")
    public Result<Void> submit(@PathVariable Long id) {
        quoteService.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public Result<Void> voidQuote(@PathVariable Long id) {
        quoteService.voidQuote(id);
        return Result.ok();
    }

    /** 导出/打印 Word 报价单 */
    @GetMapping("/{id}/export-word")
    @PreAuthorize("hasAuthority('PERM_quote:export')")
    public ResponseEntity<ByteArrayResource> exportWord(@PathVariable Long id,
                                                        @RequestParam(required = false) Long templateId) throws Exception {
        byte[] tpl = null;
        if (templateId != null) {
            SysFile tplFile = fileService.get(templateId);
            if (tplFile != null) {
                try (java.io.InputStream in = fileService.downloadStream(tplFile)) {
                    tpl = StreamUtils.copyToByteArray(in);
                }
            }
        }
        byte[] docx = wordExporter.export(id, tpl);
        QuoteVO q = quoteService.detail(id);
        String name = URLEncoder.encode(q.getCode() + "-报价单.docx", "UTF-8").replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(new ByteArrayResource(docx));
    }

    // ---------------- 计价规则维护 ----------------

    @GetMapping("/rules")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public Result<List<PricingRule>> rules() {
        return Result.ok(ruleMapper.selectList(Wrappers.<PricingRule>lambdaQuery()
                .orderByAsc(PricingRule::getPriority)));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public Result<Long> saveRule(@RequestBody PricingRule rule) {
        pricingService.validateRule(rule.getExpression());
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return Result.ok(rule.getId());
    }

    @lombok.Data
    public static class CalcDTO {
        private List<QuoteItem> items;
        private BigDecimal urgentFactor;
    }
}
