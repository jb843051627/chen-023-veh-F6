package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核签单立案表单——照现行那张，不另起式样。
 * 单代号系统排，单上自带号递来不认；三段应落几人与算齐口径是系统定的，不在表单上填。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferOpenForm", description = "核签单立案表单")
public class VehXferOpenForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 事由记要（现行表单的备注老格） */
    @ApiModelProperty(value = "事由记要")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
