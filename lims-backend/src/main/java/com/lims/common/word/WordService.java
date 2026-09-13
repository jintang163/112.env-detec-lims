package com.lims.common.word;

import com.lims.common.core.BusinessException;
import com.lims.common.core.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 基于 Apache POI 的 Word(.docx) 生成与解析工具。
 * 报价单/合同模板: 段落占位符 {{key}} 替换 + 明细表格动态行。
 */
@Slf4j
@Component
public class WordService {

    /**
     * 用模板生成文档: 模板里用 {{key}} 占位, 表格中可含明细列占位。
     *
     * @param templateBytes docx 模板字节(可为 null, 为空时生成简单文档)
     * @param vars          普通占位符变量
     * @param details       明细行(每个 Map 一行, key 为列占位名)
     */
    public byte[] renderDocx(byte[] templateBytes, Map<String, String> vars,
                             List<Map<String, String>> details) {
        try (XWPFDocument doc = templateBytes == null || templateBytes.length == 0
                ? new XWPFDocument()
                : new XWPFDocument(new java.io.ByteArrayInputStream(templateBytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (templateBytes == null || templateBytes.length == 0) {
                fillBlank(doc, vars, details);
            } else {
                replaceParagraphs(doc, vars);
                if (details != null && !details.isEmpty()) {
                    fillDetailTable(doc, details);
                }
            }
            doc.write(out);
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Word 生成失败", e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "Word生成失败: " + e.getMessage());
        }
    }

    private void fillBlank(XWPFDocument doc, Map<String, String> vars,
                           List<Map<String, String>> details) {
        String title = vars.getOrDefault("title", "单据");
        XWPFParagraph head = doc.createParagraph();
        head.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun hr = head.createRun();
        hr.setBold(true);
        hr.setFontSize(18);
        hr.setText(title);

        vars.forEach((k, v) -> {
            if ("title".equals(k)) {
                return;
            }
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText(k + ": " + v);
        });

        if (details != null && !details.isEmpty()) {
            List<String> cols = new java.util.ArrayList<>(details.get(0).keySet());
            XWPFTable table = doc.createTable(details.size() + 1, cols.size());
            setTableFullWidth(table);
            for (int c = 0; c < cols.size(); c++) {
                setCell(table.getRow(0).getCell(c), cols.get(c), true);
            }
            for (int r = 0; r < details.size(); r++) {
                for (int c = 0; c < cols.size(); c++) {
                    setCell(table.getRow(r + 1).getCell(c),
                            details.get(r).getOrDefault(cols.get(c), ""), false);
                }
            }
        }
    }

    /** 段落级 {{key}} 替换(支持占位符被拆到多个 run 的情况, 整段重写) */
    private void replaceParagraphs(XWPFDocument doc, Map<String, String> vars) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            replaceInParagraph(p, vars);
        }
        for (XWPFTable t : doc.getTables()) {
            for (XWPFTableRow row : t.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraph(p, vars);
                    }
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph p, Map<String, String> vars) {
        String text = p.getText();
        if (text == null || !text.contains("{{")) {
            return;
        }
        String replaced = text;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            replaced = replaced.replace("{{" + e.getKey() + "}}",
                    e.getValue() == null ? "" : e.getValue());
        }
        // 保留首个 run 的样式, 重写整段文本
        if (!replaced.equals(text)) {
            for (int i = p.getRuns().size() - 1; i >= 1; i--) {
                p.removeRun(i);
            }
            if (p.getRuns().isEmpty()) {
                p.createRun().setText(replaced);
            } else {
                p.getRuns().get(0).setText(replaced, 0);
            }
        }
    }

    /**
     * 找到含 {{item...}} 占位的表格行作为模板行, 按明细复制扩展。
     */
    private void fillDetailTable(XWPFDocument doc, List<Map<String, String>> details) {
        for (XWPFTable table : doc.getTables()) {
            int templateRowIdx = -1;
            XWPFTableRow templateRow = null;
            for (int i = 0; i < table.getNumberOfRows(); i++) {
                XWPFTableRow row = table.getRow(i);
                StringBuilder rowText = new StringBuilder();
                for (XWPFTableCell cell : row.getTableCells()) {
                    rowText.append(cell.getText() == null ? "" : cell.getText());
                }
                if (rowText.toString().contains("{{item.")) {
                    templateRowIdx = i;
                    templateRow = row;
                    break;
                }
            }
            if (templateRow == null) {
                continue;
            }
            // 复制模板行的 XML, 逐行替换
            for (int i = 0; i < details.size(); i++) {
                XWPFTableRow newRow;
                if (i == 0) {
                    newRow = templateRow;
                } else {
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow ctRow =
                            (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow)
                                    templateRow.getCtRow().copy();
                    newRow = new XWPFTableRow(ctRow, table);
                    table.addRow(newRow, templateRowIdx + i);
                }
                Map<String, String> rowVars = new java.util.HashMap<>();
                details.get(i).forEach((k, v) -> rowVars.put("item." + k, v));
                for (XWPFTableCell cell : newRow.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraph(p, rowVars);
                    }
                }
            }
        }
    }

    private void setTableFullWidth(XWPFTable table) {
        CTTblWidth width = table.getCTTbl().addNewTblPr().addNewTblW();
        width.setType(STTblWidth.PCT);
        width.setW(java.math.BigInteger.valueOf(5000)); // 100% (50*100)
    }

    private void setCell(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setBold(bold);
        run.setText(text);
    }

    /** 解析上传的 docx 为纯文本(合同/资质材料解析用) */
    public String parseDocxText(byte[] bytes) {
        try (XWPFDocument doc = new XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText() != null) {
                    sb.append(p.getText()).append('\n');
                }
            }
            for (XWPFTable t : doc.getTables()) {
                for (XWPFTableRow row : t.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append('\t');
                    }
                    sb.append('\n');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Word 解析失败", e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "Word解析失败: " + e.getMessage());
        }
    }

    public static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
