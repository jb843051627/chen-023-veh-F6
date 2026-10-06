package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 事后翻查一板：这一回点了几张办讫单、对上几本、差几笔，逐张明细挂下面。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferAuditBoard", description = "核签单事后翻查一板")
public class VehXferAuditBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 这一回点了几张办讫单 */
    @ApiModelProperty(value = "这一回点了几张")
    private Integer checkedCount;

    /** 对上几本（名册与回库现点一笔不差） */
    @ApiModelProperty(value = "对上几本")
    private Integer matchedCount;

    /** 差几本（指名道姓在明细里） */
    @ApiModelProperty(value = "差几本")
    private Integer mismatchedCount;

    /** 逐张明细 */
    @ApiModelProperty(value = "逐张明细")
    private List<VehXferAudit> rows = Collections.emptyList();

    public Integer getCheckedCount() {
        return checkedCount;
    }

    public void setCheckedCount(Integer checkedCount) {
        this.checkedCount = checkedCount;
    }

    public Integer getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(Integer matchedCount) {
        this.matchedCount = matchedCount;
    }

    public Integer getMismatchedCount() {
        return mismatchedCount;
    }

    public void setMismatchedCount(Integer mismatchedCount) {
        this.mismatchedCount = mismatchedCount;
    }

    public List<VehXferAudit> getRows() {
        return rows;
    }

    public void setRows(List<VehXferAudit> rows) {
        this.rows = rows;
    }
}
