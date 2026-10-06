package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 递单落笔回执。
 *
 * 回的全是系统数出来的那一份：当段、第几轮、应落几人、已落几人、齐没齐、单停在哪段。
 * 口信节点号只回挂"来意"，对不上时不作调和——那一笔当它没落，以这一处回的号为准。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferSignReceipt", description = "递单落笔回执")
public class VehXferSignReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单代号 */
    @ApiModelProperty(value = "核签单代号")
    private String billNo;

    /** 接没接（封卷、口信对不上、本段已齐的一律不接） */
    @ApiModelProperty(value = "接没接")
    private Boolean accepted;

    /** 这一笔落没落库（重复递交幂等：账上还是头一笔，不另起行） */
    @ApiModelProperty(value = "这一笔落没落库")
    private Boolean newLine;

    /** 系统数出来的当段 0初审 1复核 2终审（落笔前） */
    @ApiModelProperty(value = "系统数出来的当段")
    private Integer nodeNo;

    /** 递单自报节点号（只当来意） */
    @ApiModelProperty(value = "递单自报节点号")
    private Integer claimNode;

    /** 来意与系统段次一不一致（没带口信为 null） */
    @ApiModelProperty(value = "来意与系统段次是否一致")
    private Boolean claimMatched;

    /** 第几轮（落笔时） */
    @ApiModelProperty(value = "第几轮")
    private Integer roundNo;

    /** 当段应落几人 */
    @ApiModelProperty(value = "当段应落几人")
    private Integer needCount;

    /** 当段本轮已落几人（系统点出来的数） */
    @ApiModelProperty(value = "当段本轮已落几人")
    private Integer signedCount;

    /** 这一笔落完，当段过没过（齐了且话头过关） */
    @ApiModelProperty(value = "当段过没过")
    private Boolean nodeComplete;

    /** 当段是否挂在待定（两名同判没合上：不进下一段，也不就此终止） */
    @ApiModelProperty(value = "当段是否挂在待定")
    private Boolean nodePending;

    /** 落笔后整张单停在哪一段 */
    @ApiModelProperty(value = "落笔后单停在哪一段")
    private Integer resultNode;

    /** 落笔后单的情形 0在核 1办讫 2驳回 */
    @ApiModelProperty(value = "落笔后单的情形")
    private Integer status;

    /** 批注（不接的缘由/压回去向/待定挂法） */
    @ApiModelProperty(value = "批注")
    private String mark;

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

    public Boolean getNewLine() {
        return newLine;
    }

    public void setNewLine(Boolean newLine) {
        this.newLine = newLine;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
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

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(Integer signedCount) {
        this.signedCount = signedCount;
    }

    public Boolean getNodeComplete() {
        return nodeComplete;
    }

    public void setNodeComplete(Boolean nodeComplete) {
        this.nodeComplete = nodeComplete;
    }

    public Boolean getNodePending() {
        return nodePending;
    }

    public void setNodePending(Boolean nodePending) {
        this.nodePending = nodePending;
    }

    public Integer getResultNode() {
        return resultNode;
    }

    public void setResultNode(Integer resultNode) {
        this.resultNode = resultNode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
