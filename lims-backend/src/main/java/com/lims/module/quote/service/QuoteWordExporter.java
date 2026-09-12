package com.lims.module.quote.service;

import com.lims.common.word.WordService;
import com.lims.module.customer.entity.Customer;
import com.lims.module.customer.mapper.CustomerMapper;
import com.lims.module.quote.dto.QuoteVO;
import com.lims.module.quote.entity.Quote;
import com.lims.module.quote.entity.QuoteItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报价单 Word 生成(Apache POI)。无自定义模板时由 WordService 生成标准版式;
 * 将 docx 模板放到资源目录可替换为贵司抬头模板(占位符见 vars)。
 */
@Service
@RequiredArgsConstructor
public class QuoteWordExporter {

    private final WordService wordService;
    private final QuoteService quoteService;
    private final CustomerMapper customerMapper;

    public byte[] export(Long quoteId, byte[] templateBytes) {
        QuoteVO q = quoteService.detail(quoteId);
        Customer c = customerMapper.selectById(q.getCustomerId());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        Map<String, String> vars = new HashMap<>();
        vars.put("title", "环境检测报价单");
        vars.put("code", q.getCode());
        vars.put("customer", c == null ? "" : c.getName());
        vars.put("contact", c == null ? "" : nullToEmpty(c.getContactPerson()) + " " + nullToEmpty(c.getContactPhone()));
        vars.put("date", java.time.LocalDate.now().format(fmt));
        vars.put("validUntil", q.getValidUntil() == null ? "" : q.getValidUntil().format(fmt));
        vars.put("total", WordService.money(q.getTotalAmount()));
        vars.put("discount", WordService.money(q.getDiscountAmount()));
        vars.put("finalAmount", WordService.money(q.getFinalAmount()));
        vars.put("amountCn", toChinese(q.getFinalAmount()));
        vars.put("remark", nullToEmpty(q.getRemark()));

        List<Map<String, String>> details = new ArrayList<>();
        int no = 1;
        for (QuoteItem item : q.getItems()) {
            Map<String, String> row = new HashMap<>();
            row.put("no", String.valueOf(no++));
            row.put("name", nullToEmpty(item.getItemName()));
            row.put("standard", nullToEmpty(item.getStandardCode()));
            row.put("spec", nullToEmpty(item.getSpec()));
            row.put("unit", nullToEmpty(item.getUnit()));
            row.put("qty", item.getQty() == null ? "" : item.getQty().stripTrailingZeros().toPlainString());
            row.put("unitPrice", WordService.money(item.getUnitPrice()));
            row.put("amount", WordService.money(item.getAmount()));
            details.add(row);
        }
        return wordService.renderDocx(templateBytes, vars, details);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 极简人民币大写转换(整数部分+两位小数) */
    public static String toChinese(BigDecimal amount) {
        if (amount == null) {
            return "零元整";
        }
        String[] digits = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] units = {"", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟"};
        long yuan = amount.setScale(2, java.math.RoundingMode.HALF_UP).longValue();
        int jiao = amount.multiply(new BigDecimal("10")).setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue() % 10;
        int fen = amount.multiply(new BigDecimal("100")).setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue() % 10;
        if (yuan == 0 && jiao == 0 && fen == 0) {
            return "零元整";
        }
        StringBuilder sb = new StringBuilder();
        String ys = String.valueOf(yuan);
        int len = ys.length();
        boolean zero = false;
        for (int i = 0; i < len; i++) {
            int d = ys.charAt(i) - '0';
            int unitIdx = len - 1 - i;
            if (d == 0) {
                zero = true;
                if (unitIdx == 4) {
                    sb.append("万");
                }
                if (unitIdx == 8) {
                    sb.append("亿");
                }
            } else {
                if (zero) {
                    sb.append("零");
                    zero = false;
                }
                sb.append(digits[d]).append(units[unitIdx]);
            }
        }
        if (yuan > 0) {
            sb.append("元");
        }
        if (jiao == 0 && fen == 0) {
            sb.append("整");
        } else {
            sb.append(jiao == 0 ? "零" : digits[jiao] + "角");
            if (fen != 0) {
                sb.append(digits[fen] + "分");
            }
        }
        return sb.toString();
    }
}
