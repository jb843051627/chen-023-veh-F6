package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 节点把整张单驳回的话：全单就此止住，后面各节点不再收笔。
 * 与"判不予移交→抹本轮笔压回重走"是两件事：驳回不留重走的路，钉成终态。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehSignDismissForm", description = "核签单整单驳回")
public class VehSignDismissForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单号（必给） */
    @ApiModelProperty(value = "核签单号")
    private String billNo;

    /** 递单捎来的节点号（只当口信，驳回也只认系统当段） */
    @ApiModelProperty(value = "递单自报节点（只当口信）")
    private Integer claimNode;

    /** 驳回人代号（必给） */
    @ApiModelProperty(value = "驳回人代号")
    private String signerNo;

    /** 驳回缘由（必给，日后翻查凭这句） */
    @ApiModelProperty(value = "驳回缘由")
    private String reason;

    /** 幂等键 */
    @ApiModelProperty(value = "幂等键")
    private String requestId;

    /** 驳回那一刻（墙钟，缺省按当下） */
    @ApiModelProperty(value = "驳回时刻 yyyy-MM-dd HH:mm:ss")
    private String at;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAt() {
        return at;
    }

    public void setAt(String at) {
        this.at = at;
    }
}
