package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核签单落笔递来的话。
 *
 * 递单捎来的节点号 {@link #claimNode} 只当口信：与系统数出来的当段对不上时不作调和，
 * 这一笔当没落，整单照旧停在系统那一段。同一请求重递带同一个 {@link #requestId}，按幂等处理。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehSignForm", description = "核签单落笔")
public class VehSignForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单号（必给，系统排的那张） */
    @ApiModelProperty(value = "核签单号")
    private String billNo;

    /** 递单捎来的节点号（只当口信，对不上只认系统当段，这笔不落） */
    @ApiModelProperty(value = "递单自报节点 0初审 1复核 2终审（只当口信）")
    private Integer claimNode;

    /** 落笔人代号（必给；同人在同节点同轮由唯一键挡，只落一笔） */
    @ApiModelProperty(value = "落笔人代号")
    private String signerNo;

    /** 这一笔的话 MOVE可移交 / REJECT不予移交（必给，只此两样） */
    @ApiModelProperty(value = "这一笔的话 MOVE/REJECT")
    private String verdict;

    /** 这次递交的幂等键（同键重递按头一回回，不再落第二笔） */
    @ApiModelProperty(value = "幂等键")
    private String requestId;

    /** 落在哪一刻（墙钟 yyyy-MM-dd HH:mm:ss，缺省按问话当下；并发前后脚各是各的行） */
    @ApiModelProperty(value = "落笔时刻 yyyy-MM-dd HH:mm:ss")
    private String at;

    /** 随笔的话（可空） */
    @ApiModelProperty(value = "随笔的话")
    private String remark;

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

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
