package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.fc.v2.model.auto.TVehXferSign;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一段的回显：这一节点该几个人落笔、按哪个口径算凑齐、当下已落几人、各笔写了什么。
 * 已落几人系统顺着落笔留痕点出来，屏上那个数字只算回显。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "VehXferNodeView", description = "核签单分段回显")
public class VehXferNodeView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 段次 0初审 1复核 2终审 */
    @ApiModelProperty(value = "段次")
    private Integer nodeNo;

    /** 段名目 */
    @ApiModelProperty(value = "段名目")
    private String nodeName;

    /** 该几个人落笔 */
    @ApiModelProperty(value = "该几个人落笔")
    private Integer needCount;

    /** 按哪个口径算凑齐 */
    @ApiModelProperty(value = "算齐口径")
    private String modeText;

    /** 是不是整张单当下停的那一段 */
    @ApiModelProperty(value = "是否当下所停段")
    private Boolean current;

    /** 当下已落几人（系统点出来的数） */
    @ApiModelProperty(value = "当下已落几人")
    private Integer signedCount;

    /** 人齐没齐（条数与应落人数持平） */
    @ApiModelProperty(value = "人齐没齐")
    private Boolean full;

    /** 过没过（齐且话头过关） */
    @ApiModelProperty(value = "过没过")
    private Boolean passed;

    /** 是否挂在待定（两名同判没合上） */
    @ApiModelProperty(value = "是否挂在待定")
    private Boolean pending;

    /** 这一段有效那轮各笔（谁落的、写了什么、哪一刻，屏上都看得见） */
    @ApiModelProperty(value = "这一段有效那轮各笔")
    private List<TVehXferSign> signs = Collections.emptyList();

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public String getModeText() {
        return modeText;
    }

    public void setModeText(String modeText) {
        this.modeText = modeText;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    public Integer getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(Integer signedCount) {
        this.signedCount = signedCount;
    }

    public Boolean getFull() {
        return full;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean getPending() {
        return pending;
    }

    public void setPending(Boolean pending) {
        this.pending = pending;
    }

    public List<TVehXferSign> getSigns() {
        return signs;
    }

    public void setSigns(List<TVehXferSign> signs) {
        this.signs = signs;
    }
}
