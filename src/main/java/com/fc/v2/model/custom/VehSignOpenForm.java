package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核签单立案递来的话。线索代号必给——这张单为哪条督办两回没落地的线索而立。
 * 单号系统排（HS+雪花），单上自带号不认。表单照现行核签单那张，不另起式样。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehSignOpenForm", description = "核签单立案")
public class VehSignOpenForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 督办线索代号（必给） */
    @ApiModelProperty(value = "督办线索代号（必给）")
    private String clueNo;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    public String getClueNo() {
        return clueNo;
    }

    public void setClueNo(String clueNo) {
        this.clueNo = clueNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
