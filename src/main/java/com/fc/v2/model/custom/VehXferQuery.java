package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核签单台账屏查条件。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferQuery", description = "核签单台账屏查条件")
public class VehXferQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单代号（前缀匹配） */
    @ApiModelProperty(value = "核签单代号")
    private String billNo;

    /** 情形 0在核 1办讫 2驳回（不给则全摆） */
    @ApiModelProperty(value = "情形 0在核 1办讫 2驳回")
    private Integer status;

    /** 停在哪一段 0初审 1复核 2终审 */
    @ApiModelProperty(value = "停在哪一段")
    private Integer nodeNo;

    /** 页码（从 1 起） */
    @ApiModelProperty(value = "页码")
    private Integer page;

    /** 每页几条 */
    @ApiModelProperty(value = "每页几条")
    private Integer limit;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

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
}
