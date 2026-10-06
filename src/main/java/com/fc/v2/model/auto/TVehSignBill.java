package com.fc.v2.model.auto;

import java.util.Date;
import java.util.List;

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

import com.fc.v2.model.custom.TVehSignNodeView;

import java.io.Serializable;

/**
 * 核签单卷对象 t_veh_sign_bill（三节点新走法，旧 t_veh_move_bill 那套原样不动）。
 *
 * 卷上只记停在哪一段、在这一段第几轮、整单情形；这一段已落几人不留手填格——
 * 系统顺着落笔先后一段一段点条数（见 t_veh_sign_ink），页面数字只算回显。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_veh_sign_bill")
@ApiModel(value = "TVehSignBill", description = "核签单卷（三节点新走法）")
public class TVehSignBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核签单号（系统排 HS+雪花，递单自带的不认） */
    @TableField("bill_no")
    @ApiModelProperty(value = "核签单号（系统排）")
    private String billNo;

    /** 督办线索代号 */
    @TableField("clue_no")
    @ApiModelProperty(value = "督办线索代号")
    private String clueNo;

    /** 停在哪一段 0初审 1复核 2终审（只由系统数，递单捎来的 node 只当口信） */
    @TableField("stage")
    @ApiModelProperty(value = "停在哪一段 0初审 1复核 2终审")
    private Integer stage;

    /** 在这一段重走第几轮（压回重走加一，新旧份分开放的凭据） */
    @TableField("round_no")
    @ApiModelProperty(value = "本段重走第几轮")
    private Integer roundNo;

    /** 情形 0在签 1已办讫 2已驳回 3待定（终审判分歧挂待定） */
    @TableField("status")
    @ApiModelProperty(value = "情形 0在签 1已办讫 2已驳回 3待定")
    private Integer status;

    /** 最近一次递单的幂等键（只记最近一回；认不认看流水同键头一笔） */
    @TableField("request_id")
    @ApiModelProperty(value = "最近一次递单的幂等键")
    private String requestId;

    /** 终审同判结论 MOVE/REJECT（只有终审两笔同判才落） */
    @TableField("final_decision")
    @ApiModelProperty(value = "终审同判结论 MOVE可移交/REJECT不予移交")
    private String finalDecision;

    /** 办讫/驳回那一刻 */
    @TableField("closed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "办讫/驳回那一刻")
    private Date closedAt;

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

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 三段逐节点回挂（落笔条数点出来的，不入库，回读现挂） */
    @TableField(exist = false)
    @ApiModelProperty(value = "三段逐节点点出来的落笔情形")
    private List<TVehSignNodeView> nodes;

    /** 全册在签张数（台账一条 SQL 同一快照带回，不入库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "全册在签张数（立案+1办讫-1）")
    private Integer boardActiveCount;

    /** 本屏条数（台账一条 SQL 同一快照带回，不入库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "本屏条数")
    private Long boardTotal;

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

    public String getClueNo() {
        return clueNo;
    }

    public void setClueNo(String clueNo) {
        this.clueNo = clueNo;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
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

    public List<TVehSignNodeView> getNodes() {
        return nodes;
    }

    public void setNodes(List<TVehSignNodeView> nodes) {
        this.nodes = nodes;
    }

    public Integer getBoardActiveCount() {
        return boardActiveCount;
    }

    public void setBoardActiveCount(Integer boardActiveCount) {
        this.boardActiveCount = boardActiveCount;
    }

    public Long getBoardTotal() {
        return boardTotal;
    }

    public void setBoardTotal(Long boardTotal) {
        this.boardTotal = boardTotal;
    }
}
