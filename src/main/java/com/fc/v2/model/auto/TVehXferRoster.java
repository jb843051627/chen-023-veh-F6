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
 * 移交执法名册对象 t_veh_xfer_roster
 *
 * 办讫一张进一行：终审结论与三段笔数快照，与单上结论同源一次回算出来，
 * 一处取数、不许手填。事后翻查从第三段一节点一节点数回第一段，与本册一笔不差。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_veh_xfer_roster")
@ApiModel(value = "TVehXferRoster", description = "移交执法名册")
public class TVehXferRoster implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核签单 id */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "核签单 id")
    private Long billId;

    /** 核签单代号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "核签单代号")
    private String billNo;

    /** 终审结论（MOVE可移交，与单上结论同一次回算） */
    @TableField("conclusion")
    @ApiModelProperty(value = "终审结论")
    private String conclusion;

    /** 第一段有效笔数（回算快照） */
    @TableField("node0_count")
    @ApiModelProperty(value = "第一段有效笔数")
    private Integer node0Count;

    /** 第二段有效笔数（回算快照） */
    @TableField("node1_count")
    @ApiModelProperty(value = "第二段有效笔数")
    private Integer node1Count;

    /** 第三段有效笔数（回算快照） */
    @TableField("node2_count")
    @ApiModelProperty(value = "第三段有效笔数")
    private Integer node2Count;

    /** 办讫时单子在第几轮 */
    @TableField("round_no")
    @ApiModelProperty(value = "办讫时在第几轮")
    private Integer roundNo;

    /** 办讫那一刻（与单上同一刻） */
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

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public Integer getNode0Count() {
        return node0Count;
    }

    public void setNode0Count(Integer node0Count) {
        this.node0Count = node0Count;
    }

    public Integer getNode1Count() {
        return node1Count;
    }

    public void setNode1Count(Integer node1Count) {
        this.node1Count = node1Count;
    }

    public Integer getNode2Count() {
        return node2Count;
    }

    public void setNode2Count(Integer node2Count) {
        this.node2Count = node2Count;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
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
}
