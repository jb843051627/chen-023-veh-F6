package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fc.v2.model.custom.VehXferNodeView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 移交执法核签单（系统盯办卷）对象 t_veh_xfer_bill
 *
 * 卷上只记三件事：停在哪一段（node_no）、第几轮（round_no）、什么情形（status）。
 * 已落几人不留手填格——系统顺着落笔留痕一段一段点出来，屏上那个数只算回显。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_veh_xfer_bill")
@ApiModel(value = "TVehXferBill", description = "移交执法核签单（系统盯办卷）")
public class TVehXferBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核签单代号（系统排号，单上自带的不认） */
    @TableField("bill_no")
    @ApiModelProperty(value = "核签单代号")
    private String billNo;

    /** 整张单停在哪一段 0初审 1复核 2终审 */
    @TableField("node_no")
    @ApiModelProperty(value = "整张单停在哪一段 0初审 1复核 2终审")
    private Integer nodeNo;

    /** 第几轮落笔（不予移交压回一格即加一） */
    @TableField("round_no")
    @ApiModelProperty(value = "第几轮落笔")
    private Integer roundNo;

    /** 核签情形 0在核 1办讫 2驳回 */
    @TableField("status")
    @ApiModelProperty(value = "核签情形 0在核 1办讫 2驳回")
    private Integer status;

    /** 办讫那一刻（第三段点齐时钉上） */
    @TableField("done_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "办讫那一刻")
    private Date doneAt;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注（现行那张表单的老格，式样不动） */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    // ---------------------------------------------------------------- 以下全是回显格，不落本单表

    /** 当段应落几人（唯一一把尺给出，只回显不录入） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段应落几人（回显）")
    private Integer needCount;

    /** 当段本轮已落几人（系统点出来的数，只回显不录入） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段本轮已落几人（回显）")
    private Integer signedCount;

    /** 当段算齐口径（回显） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段算齐口径（回显）")
    private String modeText;

    /** 当段名目（回显） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段名目（回显）")
    private String nodeName;

    /** 当段此刻还收不收笔（唯一一处数出来，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段此刻还收不收笔")
    private Boolean signOpen;

    /** 不收笔的缘由（不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "不收笔的缘由")
    private String signBlockReason;

    /** 下一节点此刻开不开（唯一一处数出来，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "下一节点此刻开不开")
    private Boolean nextOpen;

    /** 当段是否挂在待定（两名同判没合上，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段是否挂在待定")
    private Boolean nodePending;

    /** 三段逐段回显（应落几人/口径/已落几人/各笔写了什么，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "三段逐段回显")
    private List<VehXferNodeView> nodeViews;

    /** 全卷落笔留痕（各轮各段，回读翻查用，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "全卷落笔留痕")
    private List<TVehXferSign> signs;

    /** 当段本轮已落几人（台账那条 SQL 顺带点出来的，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当段本轮已落几人（台账同回点算）")
    private Integer curSignCount;

    /** 全册单数（开屏同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "全册单数")
    private Integer boardTotal;

    /** 全册在核单数（立案加一、办讫减一，开屏同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "全册在核单数")
    private Integer boardActiveCount;

    /** 在核单里停第一段的张数（逐节点点出，同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "在核停第一段张数")
    private Integer boardNode0Active;

    /** 在核单里停第二段的张数（逐节点点出，同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "在核停第二段张数")
    private Integer boardNode1Active;

    /** 在核单里停第三段的张数（逐节点点出，同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "在核停第三段张数")
    private Integer boardNode2Active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getDoneAt() {
        return doneAt;
    }

    public void setDoneAt(Date doneAt) {
        this.doneAt = doneAt;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public String getModeText() {
        return modeText;
    }

    public void setModeText(String modeText) {
        this.modeText = modeText;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Boolean getSignOpen() {
        return signOpen;
    }

    public void setSignOpen(Boolean signOpen) {
        this.signOpen = signOpen;
    }

    public String getSignBlockReason() {
        return signBlockReason;
    }

    public void setSignBlockReason(String signBlockReason) {
        this.signBlockReason = signBlockReason;
    }

    public Boolean getNextOpen() {
        return nextOpen;
    }

    public void setNextOpen(Boolean nextOpen) {
        this.nextOpen = nextOpen;
    }

    public Boolean getNodePending() {
        return nodePending;
    }

    public void setNodePending(Boolean nodePending) {
        this.nodePending = nodePending;
    }

    public List<VehXferNodeView> getNodeViews() {
        return nodeViews;
    }

    public void setNodeViews(List<VehXferNodeView> nodeViews) {
        this.nodeViews = nodeViews;
    }

    public List<TVehXferSign> getSigns() {
        return signs;
    }

    public void setSigns(List<TVehXferSign> signs) {
        this.signs = signs;
    }

    public Integer getCurSignCount() {
        return curSignCount;
    }

    public void setCurSignCount(Integer curSignCount) {
        this.curSignCount = curSignCount;
    }

    public Integer getBoardTotal() {
        return boardTotal;
    }

    public void setBoardTotal(Integer boardTotal) {
        this.boardTotal = boardTotal;
    }

    public Integer getBoardActiveCount() {
        return boardActiveCount;
    }

    public void setBoardActiveCount(Integer boardActiveCount) {
        this.boardActiveCount = boardActiveCount;
    }

    public Integer getBoardNode0Active() {
        return boardNode0Active;
    }

    public void setBoardNode0Active(Integer boardNode0Active) {
        this.boardNode0Active = boardNode0Active;
    }

    public Integer getBoardNode1Active() {
        return boardNode1Active;
    }

    public void setBoardNode1Active(Integer boardNode1Active) {
        this.boardNode1Active = boardNode1Active;
    }

    public Integer getBoardNode2Active() {
        return boardNode2Active;
    }

    public void setBoardNode2Active(Integer boardNode2Active) {
        this.boardNode2Active = boardNode2Active;
    }
}
