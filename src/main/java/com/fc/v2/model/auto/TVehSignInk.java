package com.fc.v2.model.auto;

import java.util.Date;

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

/**
 * 核签单落笔流水对象 t_veh_sign_ink。
 *
 * 一笔就是一行：系统顺着这张单的落笔先后，按节点、按轮次点在册作数的条数，
 * 当"已落几人"。判不予移交时只把本轮这一段的笔置 void 留痕，不物理删；
 * 更早各节点、更早各轮的旧笔原样存底。同一秒钟前后脚到的两笔各记各的行。

 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_veh_sign_ink")
@ApiModel(value = "TVehSignInk", description = "核签单落笔流水")
public class TVehSignInk implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属核签单 id */
    @TableField("bill_id")
    @ApiModelProperty(value = "所属核签单 id")
    private Long billId;

    /** 落在哪一段（照系统当下那一段，不睬口信） */
    @TableField("node_no")
    @ApiModelProperty(value = "落在哪一段 0初审 1复核 2终审")
    private Integer nodeNo;

    /** 该段第几轮（重走新起一轮，新旧份分开） */
    @TableField("round_no")
    @ApiModelProperty(value = "该段第几轮")
    private Integer roundNo;

    /** 落笔人代号 */
    @TableField("signer_no")
    @ApiModelProperty(value = "落笔人代号")
    private String signerNo;

    /** 这一笔的话 MOVE可移交/REJECT不予移交 */
    @TableField("verdict")
    @ApiModelProperty(value = "这一笔的话 MOVE/REJECT")
    private String verdict;

    /** 递单捎来的节点号（只当口信留痕，对不上不调和） */
    @TableField("claim_node")
    @ApiModelProperty(value = "递单捎来的节点号（只当口信）")
    private Integer claimNode;

    /** 这笔是哪次递单带进来的（幂等凭据） */
    @TableField("request_id")
    @ApiModelProperty(value = "这笔的幂等键")
    private String requestId;

    /** 这笔的情形 0在册作数 1本轮压回被抹（留痕不删） */
    @TableField("ink_state")
    @ApiModelProperty(value = "情形 0在册 1被抹留痕")
    private Integer inkState;

    /** 落在哪一刻（并发前后脚各是各的行） */
    @TableField("ink_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落在哪一刻")
    private Date inkAt;

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

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Integer getClaimNode() {
        return claimNode;
    }

    public void setClaimNode(Integer claimNode) {
        this.claimNode = claimNode;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Integer getInkState() {
        return inkState;
    }

    public void setInkState(Integer inkState) {
        this.inkState = inkState;
    }

    public Date getInkAt() {
        return inkAt;
    }

    public void setInkAt(Date inkAt) {
        this.inkAt = inkAt;
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
