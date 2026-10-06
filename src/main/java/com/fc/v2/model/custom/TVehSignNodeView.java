package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TVehSignInk;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一段节点的回挂：该几个人落笔、按什么口径算凑齐、当下系统点出几条、这一段齐没齐。
 *
 * "已落几人"不由谁手填：服务顺着落笔先后点 t_veh_sign_ink 在册作数那几条，
 * 条数与该节点应落人数持平才叫齐；屏上数字只是这一处点完的回显。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "TVehSignNodeView", description = "核签单逐节点落笔情形（点条数点出来的）")
public class TVehSignNodeView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点 0初审 1复核 2终审 */
    @ApiModelProperty(value = "节点 0初审 1复核 2终审")
    private Integer nodeNo;

    /** 节点名 */
    @ApiModelProperty(value = "节点名")
    private String nodeName;

    /** 算齐口径 FIRST一名先落即齐/FIRST_WINS两名取首笔定调/CONSENSUS两名须同判 */
    @ApiModelProperty(value = "算齐口径 FIRST/FIRST_WINS/CONSENSUS")
    private String mode;

    /** 这一节点该几个人落笔 */
    @ApiModelProperty(value = "应落人数")
    private Integer needCount;

    /** 系统顺着落笔先后点出来的在册条数（只算回显） */
    @ApiModelProperty(value = "已落笔数（系统点条数）")
    private Integer signedCount;

    /** 条数与应落人数持平且口径满足才算齐 */
    @ApiModelProperty(value = "本节点齐没齐")
    private Boolean ready;

    /** 这一段在第几轮（重走新份旧份分开放） */
    @ApiModelProperty(value = "本段轮次")
    private Integer roundNo;

    /** 这一节点当轮在册作数的各笔（两人各写了什么屏上都看得见；旧轮在 history 里） */
    @ApiModelProperty(value = "当轮各笔")
    private List<TVehSignInk> current = new ArrayList<TVehSignInk>();

    /** 更早各轮被压回抹下的旧笔（原样存底，哪天补的各有凭据） */
    @ApiModelProperty(value = "历轮旧笔（含被抹的）")
    private List<TVehSignInk> history = new ArrayList<TVehSignInk>();

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public List<TVehSignInk> getCurrent() {
        return current;
    }

    public void setCurrent(List<TVehSignInk> current) {
        this.current = current;
    }

    public List<TVehSignInk> getHistory() {
        return history;
    }

    public void setHistory(List<TVehSignInk> history) {
        this.history = history;
    }
}
