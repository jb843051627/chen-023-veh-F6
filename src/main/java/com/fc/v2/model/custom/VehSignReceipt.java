package com.fc.v2.model.custom;

import java.io.Serializable;

import com.fc.v2.model.auto.TVehSignBill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 落笔收口的回执。当段、口信、一不一致、这一笔落没落、节点齐没齐、整单往哪走全回挂。
 * 不收的情形（查无/封档/待定/跳关/同人重笔/幂等头一笔）accepted=false 或 newInk=false，卷上不动。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehSignReceipt", description = "核签单落笔回执")
public class VehSignReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "核签单号")
    private String billNo;

    /** 这一笔收没收（false=这一笔当没落，卷上不动） */
    @ApiModelProperty(value = "这一笔收没收")
    private Boolean accepted;

    /** 有没有落新行（同人同节点重笔/幂等重递为 false，卷上还是头一笔） */
    @ApiModelProperty(value = "落没落新行")
    private Boolean newInk;

    /** 系统数出来的当段（回执只认这一号） */
    @ApiModelProperty(value = "系统当段")
    private Integer stage;

    /** 递单口信节点 */
    @ApiModelProperty(value = "递单口信节点")
    private Integer claimNode;

    /** 口信与系统当段一不一致 */
    @ApiModelProperty(value = "口信与系统当段一不一致")
    private Boolean claimMatched;

    /** 收口时本节点定论 NOT_READY/PASS/PRESS_BACK/PENDING */
    @ApiModelProperty(value = "本节点定论")
    private String outcome;

    /** 整单情形 0在签 1已办讫 2已驳回 3待定 */
    @ApiModelProperty(value = "整单情形")
    private Integer billStatus;

    /** 办讫的单子同次回算进了移交名册没有（终审同判可移交才 true） */
    @ApiModelProperty(value = "同次回算落移交名册没有")
    private Boolean registered;

    /** 一句话批注 */
    @ApiModelProperty(value = "批注")
    private String mark;

    /** 收口后的卷情（封档/待定时也照回） */
    @ApiModelProperty(value = "卷情回读")
    private TVehSignBill bill;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getNewInk() {
        return newInk;
    }

    public void setNewInk(Boolean newInk) {
        this.newInk = newInk;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getClaimNode() {
        return claimNode;
    }

    public void setClaimNode(Integer claimNode) {
        this.claimNode = claimNode;
    }

    public Boolean getClaimMatched() {
        return claimMatched;
    }

    public void setClaimMatched(Boolean claimMatched) {
        this.claimMatched = claimMatched;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Integer getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(Integer billStatus) {
        this.billStatus = billStatus;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public TVehSignBill getBill() {
        return bill;
    }

    public void setBill(TVehSignBill bill) {
        this.bill = bill;
    }
}
