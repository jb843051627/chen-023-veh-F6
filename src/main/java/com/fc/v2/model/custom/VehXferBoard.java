package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.fc.v2.model.auto.TVehXferBill;

/**
 * 核签单台账一板：屏头"在核几张"与名下各单是同一条 SQL、同一回点算的产物。
 * 在核数立案加一、办讫减一；逐节点点出的三段张数与屏上总数对得齐，本就该出自同一回计算。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class VehXferBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本屏单数（筛选后，一张一张点出来的） */
    private long totalCount;

    /** 全册在核单数（屏上显的那个数，与回库点的同一次算） */
    private int activeCount;

    /** 在核单里停第一段的张数（逐节点点出） */
    private int node0Active;

    /** 在核单里停第二段的张数（逐节点点出） */
    private int node1Active;

    /** 在核单里停第三段的张数（逐节点点出） */
    private int node2Active;

    /** 名下各单（当段应落/已落、收不收笔都照唯一一把尺回挂） */
    private List<TVehXferBill> bills = Collections.emptyList();

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getNode0Active() {
        return node0Active;
    }

    public void setNode0Active(int node0Active) {
        this.node0Active = node0Active;
    }

    public int getNode1Active() {
        return node1Active;
    }

    public void setNode1Active(int node1Active) {
        this.node1Active = node1Active;
    }

    public int getNode2Active() {
        return node2Active;
    }

    public void setNode2Active(int node2Active) {
        this.node2Active = node2Active;
    }

    public List<TVehXferBill> getBills() {
        return bills;
    }

    public void setBills(List<TVehXferBill> bills) {
        this.bills = bills;
    }
}
