package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核签单台账查询（照现行台账那张表的筛法，不另起式样）。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehSignQuery", description = "核签单台账查询")
public class VehSignQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "第几页")
    private Integer page;

    @ApiModelProperty(value = "每页几条")
    private Integer limit;

    @ApiModelProperty(value = "核签单号（前缀）")
    private String billNo;

    @ApiModelProperty(value = "督办线索代号（前缀）")
    private String clueNo;

    /** 情形 0在签 1已办讫 2已驳回 3待定 */
    @ApiModelProperty(value = "情形 0在签 1已办讫 2已驳回 3待定")
    private Integer status;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getClueNo() {
        return clueNo;
    }

    public void setClueNo(String clueNo) {
        this.clueNo = clueNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
