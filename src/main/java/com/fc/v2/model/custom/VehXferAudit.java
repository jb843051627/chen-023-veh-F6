package com.fc.v2.model.custom;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 事后翻查一行：一张办讫单，名册快照三数与回库现点三数，一笔不差才算对得上。
 * 两回本就该出自同一回计算；对不上指名道姓摆出来。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferAudit", description = "核签单事后翻查一行")
public class VehXferAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单代号 */
    @ApiModelProperty(value = "核签单代号")
    private String billNo;

    /** 办讫那一刻 */
    @ApiModelProperty(value = "办讫那一刻")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date doneAt;

    /** 名册上的终审结论 */
    @ApiModelProperty(value = "名册上的终审结论")
    private String rosterConclusion;

    /** 回库现点的终审结论（第三段两名同判可移交才算） */
    @ApiModelProperty(value = "回库现点的终审结论")
    private String recountConclusion;

    /** 名册快照：第一段笔数 */
    @ApiModelProperty(value = "名册快照第一段笔数")
    private Integer rosterNode0;

    /** 名册快照：第二段笔数 */
    @ApiModelProperty(value = "名册快照第二段笔数")
    private Integer rosterNode1;

    /** 名册快照：第三段笔数 */
    @ApiModelProperty(value = "名册快照第三段笔数")
    private Integer rosterNode2;

    /** 回库现点：第一段笔数 */
    @ApiModelProperty(value = "回库现点第一段笔数")
    private Integer recountNode0;

    /** 回库现点：第二段笔数 */
    @ApiModelProperty(value = "回库现点第二段笔数")
    private Integer recountNode1;

    /** 回库现点：第三段笔数 */
    @ApiModelProperty(value = "回库现点第三段笔数")
    private Integer recountNode2;

    /** 两本对没对上 */
    @ApiModelProperty(value = "两本对没对上")
    private Boolean matched;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Date getDoneAt() {
        return doneAt;
    }

    public void setDoneAt(Date doneAt) {
        this.doneAt = doneAt;
    }

    public String getRosterConclusion() {
        return rosterConclusion;
    }

    public void setRosterConclusion(String rosterConclusion) {
        this.rosterConclusion = rosterConclusion;
    }

    public String getRecountConclusion() {
        return recountConclusion;
    }

    public void setRecountConclusion(String recountConclusion) {
        this.recountConclusion = recountConclusion;
    }

    public Integer getRosterNode0() {
        return rosterNode0;
    }

    public void setRosterNode0(Integer rosterNode0) {
        this.rosterNode0 = rosterNode0;
    }

    public Integer getRosterNode1() {
        return rosterNode1;
    }

    public void setRosterNode1(Integer rosterNode1) {
        this.rosterNode1 = rosterNode1;
    }

    public Integer getRosterNode2() {
        return rosterNode2;
    }

    public void setRosterNode2(Integer rosterNode2) {
        this.rosterNode2 = rosterNode2;
    }

    public Integer getRecountNode0() {
        return recountNode0;
    }

    public void setRecountNode0(Integer recountNode0) {
        this.recountNode0 = recountNode0;
    }

    public Integer getRecountNode1() {
        return recountNode1;
    }

    public void setRecountNode1(Integer recountNode1) {
        this.recountNode1 = recountNode1;
    }

    public Integer getRecountNode2() {
        return recountNode2;
    }

    public void setRecountNode2(Integer recountNode2) {
        this.recountNode2 = recountNode2;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }
}
