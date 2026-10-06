package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TVehSignBill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核签单值班台账：名单一页与屏头在签张数同一条 SQL 同一快照算成。
 * 在签张数=立案加一、办讫/驳回减一（驳回是终态）；待定挂着仍在办，照样占数。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehSignBoard", description = "核签单值班台账")
public class VehSignBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 名单分页（每条都回挂三段点条数的情形） */
    @ApiModelProperty(value = "台账名单")
    private List<TVehSignBill> bills = new ArrayList<TVehSignBill>();

    /** 本屏条数 */
    @ApiModelProperty(value = "本屏条数")
    private Long totalCount;

    /** 全册在签张数（与名单同一次算，逐张点也对得齐） */
    @ApiModelProperty(value = "全册在签张数")
    private Integer activeCount;

    public List<TVehSignBill> getBills() {
        return bills;
    }

    public void setBills(List<TVehSignBill> bills) {
        this.bills = bills;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Integer activeCount) {
        this.activeCount = activeCount;
    }
}
