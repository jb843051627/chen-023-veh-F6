package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 移交核签单落笔留痕对象 t_veh_xfer_sign
 *
 * 一笔一行：哪一段、第几轮、谁落的、写了什么、哪一刻。主键自增即落笔先后，
 * 前后脚同时到也点得出谁先；唯一键挡住同一个人在同一节点本轮重复落笔。
 * 不予移交时本节点本轮的笔当场抹去（硬删不留），更早各节点的旧笔原样存底。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_veh_xfer_sign")
@ApiModel(value = "TVehXferSign", description = "移交核签单落笔留痕")
public class TVehXferSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键（自增，落笔先后只认这把尺） */
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属核签单 id */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属核签单 id")
    private Long billId;

    /** 落在哪一段（系统数出来的当段） */
    @TableField("node_no")
    @ApiModelProperty(value = "落在哪一段（系统数出来的当段）")
    private Integer nodeNo;

    /** 第几轮落的（新份旧份凭这轮次分开） */
    @TableField("round_no")
    @ApiModelProperty(value = "第几轮落的")
    private Integer roundNo;

    /** 落笔人代号 */
    @TableField("signer_no")
    @ApiModelProperty(value = "落笔人代号")
    private String signerNo;

    /** 落笔人姓名 */
    @TableField("signer_name")
    @ApiModelProperty(value = "落笔人姓名")
    private String signerName;

    /** 这一笔的话 MOVE可移交 HOLD不予移交 */
    @TableField("decision")
    @ApiModelProperty(value = "这一笔的话 MOVE可移交 HOLD不予移交")
    private String decision;

    /** 这一笔写的话 */
    @TableField("comment")
    @ApiModelProperty(value = "这一笔写的话")
    private String comment;

    /** 递单自报的节点号（只当口信留痕） */
    @TableField("claim_node")
    @ApiModelProperty(value = "递单自报的节点号（口信）")
    private Integer claimNode;

    /** 落在哪一刻（墙钟） */
    @TableField("signed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落在哪一刻")
    private Date signedAt;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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

    public Integer getClaimNode() {
        return claimNode;
    }

    public void setClaimNode(Integer claimNode) {
        this.claimNode = claimNode;
    }

    public Date getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Date signedAt) {
        this.signedAt = signedAt;
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
}
