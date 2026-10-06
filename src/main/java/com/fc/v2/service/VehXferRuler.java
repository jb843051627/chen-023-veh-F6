package com.fc.v2.service;

import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.auto.TVehXferSign;

/**
 * 移交执法核签单——"这一节点算不算齐、下一节点开不开、被压回来时清哪一段、
 * 点齐之后还收不收笔"四问唯一的一把尺（纯算，不碰库）。
 *
 * 三段规矩互不相同，不许彼此抄：
 *   第一段 初审（监督处监控岗）：一名即可，谁先落这一节点便算齐，那一笔的话就是本段的话；
 *   第二段 复核（市处法规科）：要两名，取先落笔那位的话为准，后一名照数入账、票次累加；
 *   第三段 终审（省中心分管负责人）：要两名同判——都写可移交才算过，都写不予移交即本段判
 *   不予移交，一可一不可挂在待定上（不进下一段，也不就此终止）。
 * 第二段不许拿第一段的规矩去解，第三段也不许拿第二段的规矩去压。
 *
 * 已落几人只认系统顺着落笔留痕点出来的条数，屏上那个数字只算回显；
 * 点出来的条数与该节点应落人数持平才叫齐，不持平就当没齐。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class VehXferRuler {

    /** 段次：初审（监督处监控岗） */
    public static final int NODE_FIRST = 0;
    /** 段次：复核（市处法规科） */
    public static final int NODE_SECOND = 1;
    /** 段次：终审（省中心分管负责人） */
    public static final int NODE_FINAL = 2;
    /** 末段 */
    public static final int MAX_NODE = NODE_FINAL;

    /** 情形：在核 */
    public static final int STATUS_RUNNING = 0;
    /** 情形：办讫（移交办讫，封卷只读） */
    public static final int STATUS_DONE = 1;
    /** 情形：驳回（全单止住，封卷只读） */
    public static final int STATUS_VETO = 2;

    /** 落笔的话：可移交 */
    public static final String DECISION_MOVE = "MOVE";
    /** 落笔的话：不予移交 */
    public static final String DECISION_HOLD = "HOLD";

    private VehXferRuler() {
    }

    /** 这一节点该几个人落笔：一段一名、二段两名、三段两名，系统定的，不许手填 */
    public static int needCount(int node) {
        return node == NODE_FIRST ? 1 : 2;
    }

    /** 段名目 */
    public static String nodeName(int node) {
        switch (node) {
            case NODE_FIRST: return "初审（监督处监控岗）";
            case NODE_SECOND: return "复核（市处法规科）";
            case NODE_FINAL: return "终审（省中心分管负责人）";
            default: return "第" + node + "段";
        }
    }

    /** 按哪个口径算凑齐（三段三套，互不抄） */
    public static String modeText(int node) {
        switch (node) {
            case NODE_FIRST: return "一名即可，谁先落这一节点便算齐";
            case NODE_SECOND: return "两名，取先落笔那位的话为准，后一名照数入账";
            case NODE_FINAL: return "两名同判：都可移交才算过，一可一不可挂待定";
            default: return "";
        }
    }

    /** 整张单停在哪一段：空着照头一段认 */
    public static int nodeOf(TVehXferBill b) {
        return b.getNodeNo() == null ? NODE_FIRST : b.getNodeNo();
    }

    /** 第几轮：空着照头一轮认 */
    public static int roundOf(TVehXferBill b) {
        return b.getRoundNo() == null || b.getRoundNo() < 1 ? 1 : b.getRoundNo();
    }

    /** 封没封卷（办讫/驳回都封） */
    public static boolean isTerminal(TVehXferBill b) {
        return b.getStatus() != null && b.getStatus() != STATUS_RUNNING;
    }

    /** 点齐之后还收不收笔（单这一层）：封了卷的一笔也不再收 */
    public static boolean acceptSign(TVehXferBill b) {
        return !isTerminal(b);
    }

    /** 被压回来时落到哪一段：收只收一格，头一段退无可退原地重走 */
    public static int pushBackTarget(int node) {
        return Math.max(NODE_FIRST, node - 1);
    }

    /** 本段本轮已落几人：系统顺着落笔留痕点出来的条数（入参须按落笔先后升序） */
    public static List<TVehXferSign> roundSigns(List<TVehXferSign> all, int node, int round) {
        List<TVehXferSign> out = new ArrayList<TVehXferSign>();
        if (all == null) {
            return out;
        }
        for (TVehXferSign s : all) {
            if (s.getNodeNo() != null && s.getNodeNo() == node
                    && s.getRoundNo() != null && s.getRoundNo() == round) {
                out.add(s);
            }
        }
        return out;
    }

    /** 本段有效那一轮（最后一轮）的笔：翻查旧账、名册回算都用这一份（入参须按落笔先后升序） */
    public static List<TVehXferSign> effectiveSigns(List<TVehXferSign> all, int node) {
        List<TVehXferSign> out = new ArrayList<TVehXferSign>();
        if (all == null) {
            return out;
        }
        int maxRound = -1;
        for (TVehXferSign s : all) {
            if (s.getNodeNo() != null && s.getNodeNo() == node
                    && s.getRoundNo() != null && s.getRoundNo() > maxRound) {
                maxRound = s.getRoundNo();
            }
        }
        if (maxRound < 0) {
            return out;
        }
        return roundSigns(all, node, maxRound);
    }

    /** 本段本轮已落几人（点出来的条数） */
    public static int signedCount(List<TVehXferSign> all, int node, int round) {
        return roundSigns(all, node, round).size();
    }

    /** 人齐没齐：点出来的条数与该节点应落人数持平才叫齐，不持平就当没齐 */
    public static boolean nodeFull(int node, List<TVehXferSign> roundSigns) {
        return roundSigns != null && roundSigns.size() >= needCount(node);
    }

    /**
     * 这一节点过没过（四问之一：算不算齐）——人齐且话头过关：
     * 头两段取先落笔那位的话（入参按落笔先后升序，头一笔即先落笔）；
     * 第三段两名同判，两笔都写可移交才算数。
     */
    public static boolean nodePassed(int node, List<TVehXferSign> roundSigns) {
        if (!nodeFull(node, roundSigns)) {
            return false;
        }
        if (node == NODE_FINAL) {
            for (TVehXferSign s : roundSigns) {
                if (!DECISION_MOVE.equals(s.getDecision())) {
                    return false;
                }
            }
            return true;
        }
        return DECISION_MOVE.equals(roundSigns.get(0).getDecision());
    }

    /** 本段是不是同判不予移交（仅第三段：两笔都写不予移交，本段判不予移交压回一格） */
    public static boolean nodeAllHold(int node, List<TVehXferSign> roundSigns) {
        if (node != NODE_FINAL || !nodeFull(node, roundSigns)) {
            return false;
        }
        for (TVehXferSign s : roundSigns) {
            if (!DECISION_HOLD.equals(s.getDecision())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 挂在待定上没有（仅第三段）：人齐了，既不是都可移交、也不是都不予移交——
     * 一可一不可，这一节点挂在待定上，既不进下一节点，也不就此终止。
     */
    public static boolean nodePending(int node, List<TVehXferSign> roundSigns) {
        return node == NODE_FINAL && nodeFull(node, roundSigns)
                && !nodePassed(node, roundSigns) && !nodeAllHold(node, roundSigns);
    }

    /** 下一节点此刻开不开（四问之二）：在核且当段本轮的笔按口径已过关，下一节点才算开 */
    public static boolean nextOpen(TVehXferBill b, List<TVehXferSign> all) {
        if (isTerminal(b)) {
            return false;
        }
        int node = nodeOf(b);
        return nodePassed(node, roundSigns(all, node, roundOf(b)));
    }

    /**
     * 当段此刻还收不收笔（四问之四）：封卷不收；本段人已满（只第三段挂待定会停在这情形）不收。
     * 返回 null 为还收；不收返回一句缘由，册面回读照原话挂。
     */
    public static String signBlockReason(TVehXferBill b, List<TVehXferSign> all) {
        return signBlockReason(b, signedCount(all, nodeOf(b), roundOf(b)));
    }

    /** 与 {@link #signBlockReason(TVehXferBill, List)} 同一个判法：台账屏按同回点出的已落人数问 */
    public static String signBlockReason(TVehXferBill b, int currentRoundSignedCount) {
        if (isTerminal(b)) {
            return b.getStatus() != null && b.getStatus() == STATUS_DONE
                    ? "这张单已移交办讫，封卷只读，一笔也加不进" : "这张单已驳回止住，一笔也加不进";
        }
        if (currentRoundSignedCount >= needCount(nodeOf(b))) {
            return "本段应落" + needCount(nodeOf(b)) + "人已齐，挂在待定上等处置，不再收笔";
        }
        return null;
    }
}
