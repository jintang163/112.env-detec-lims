package com.lims.module.sampling.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class EquipmentSaveDTO {
    private Long id;
    @NotBlank(message = "设备名称不能为空")
    private String name;
    private String category = "DEVICE";
    private String spec;
    private String unit;
    private Integer qtyTotal;
    /** 编辑时调整可用量(新增时默认为总数量) */
    private Integer qtyAvailable;
    private String status = "NORMAL";
    private Long keeperId;
    private String keeperName;
    private LocalDate purchaseDate;
    private String remark;
}
