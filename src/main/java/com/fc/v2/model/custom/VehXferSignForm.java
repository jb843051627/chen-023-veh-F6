package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 递单落笔报文。
 *
 * 带进来的节点号（claimNode）只当口信：与系统数出来的当段对不上时，那一笔当它没落，
 * 不作调和，只认系统那一处回来的段次；谁也不许在旁处直接把号按下去、把先后重排。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferSignForm", description = "递单落笔报文")
public class VehXferSignForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单代号 */
    @ApiModelProperty(value = "核签单代号")
    private String billNo;

    /** 递单自报的节点号（只当口信，可空） */
    @ApiModelProperty(value = "递单自报的节点号（口信）")
    private Integer claimNode;

    /** 落笔人代号 */
    @ApiModelProperty(value = "落笔人代号")
    private String signerNo;

    /** 落笔人姓名 */
    @ApiModelProperty(value = "落笔人姓名")
    private String signerName;

    /** 这一笔的话 MOVE可移交 HOLD不予移交 */
    @ApiModelProperty(value = "这一笔的话 MOVE可移交 HOLD不予移交")
    private String decision;

    /** 这一笔写的话 */
    @ApiModelProperty(value = "这一笔写的话")
    private String comment;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getClaimNode() {
        return claimNode;
    }

    public void setClaimNode(Integer claimNode) {
        this.claimNode = claimNode;
    }

    public String getSignerNo() {
        return signerNo;
    }

    public void setSignerNo(String signerNo) {
        this.signerNo = signerNo;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
