package com.fc.v2.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fc.v2.model.auto.TVehSignBill;
import com.fc.v2.model.auto.TVehSignInk;

/**
 * 核签单（三节点新走法）"这一节点算不算齐、下一节点开不开、压回来清哪一段、点齐之后收不收笔"
 * 唯一的一把尺——纯算，不碰库。三段规矩互不相同，谁也不许拿别段的口径来压这一段：
 *
 * <ul>
 *   <li>初审 {@link #NODE_FIRST}：一名就够，{@link SignMode#FIRST}——谁先落这一节点便算齐；</li>
 *   <li>复核 {@link #NODE_FIRST_WINS}：要两名，{@link SignMode#FIRST_WINS}——取先落笔那位的话为准，
 *       后一名照数入账、票次累加，两人各写什么都看得见；</li>
 *   <li>终审 {@link #NODE_CONSENSUS}：要两名同判，{@link SignMode#CONSENSUS}——并发前后脚到，
 *       两人都写可移交才算数；一可一不可挂待定，不进下一段也不终止。</li>
 * </ul>
 *
 * "已落几人"不由谁手填：服务顺着落笔先后点在册作数那几条递进来，条数与应落人数持平才叫齐。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class VehSignRuler {

    /** 第一段：监督处监控岗初审 */
    public static final int NODE_FIRST = 0;
    /** 第二段：市处法规科复核 */
    public static final int NODE_FIRST_WINS = 1;
    /** 第三段：省中心分管负责人终审 */
    public static final int NODE_CONSENSUS = 2;
    /** 末段 */
    public static final int MAX_NODE = 2;

    /** 情形：在签 */
    public static final int STATUS_SIGNING = 0;
    /** 情形：已办讫（终审同判可移交，封档只读） */
    public static final int STATUS_DONE = 1;
    /** 情形：已驳回（某一节点把整张单驳回，全单止住，后面各节点不再收笔） */
    public static final int STATUS_DISMISSED = 2;
    /** 情形：待定（终审两名一可一不可，挂着：不进下一节点，也不就此终止） */
    public static final int STATUS_PENDING = 3;

    /** 落笔的话：可移交 */
    public static final String VERDICT_MOVE = "MOVE";
    /** 落笔的话：不予移交（本节点定论成这个，就压回重走，不是整单驳回） */
    public static final String VERDICT_REJECT = "REJECT";

    /** 流水这笔在册作数 */
    public static final int INK_STATE_LIVE = 0;
    /** 流水这笔本轮压回被抹（留痕不物理删） */
    public static final int INK_STATE_VOID = 1;

    /** 判定：还没齐（条数没与应落人数持平） */
    public static final String OUT_NOT_READY = "NOT_READY";
    /** 判定：齐了且定论可移交——进紧邻下一节点；终审即办讫 */
    public static final String OUT_PASS = "PASS";
    /** 判定：齐了但定论不予移交——本节点本轮笔抹去、压回（清只清这一格） */
    public static final String OUT_PRESS_BACK = "PRESS_BACK";
    /** 判定：终审两名一可一不可——挂待定，笔不抹，不进也不终止 */
    public static final String OUT_PENDING = "PENDING";

    private VehSignRuler() {
    }

    /** 三段口径各管各的，不许彼此抄 */
    public enum SignMode {
        /** 初审：一名即可，谁先落便算齐 */
        FIRST(NODE_FIRST, 1, "监督处监控岗初审"),
        /** 复核：两名，取先落笔那位的话为准 */
        FIRST_WINS(NODE_FIRST_WINS, 2, "市处法规科复核"),
        /** 终审：两名同判，都写可移交才算数，分歧挂待定 */
        CONSENSUS(NODE_CONSENSUS, 2, "省中心分管负责人终审");

        private final int node;
        private final int need;
        private final String title;

        SignMode(int node, int need, String title) {
            this.node = node;
            this.need = need;
            this.title = title;
        }

        public int node() {
            return node;
        }

        /** 这一节点该几个人落笔 */
        public int needCount() {
            return need;
        }

        public String title() {
            return title;
        }

        static SignMode of(int node) {
            for (SignMode m : values()) {
                if (m.node == node) {
                    return m;
                }
            }
            throw new IllegalArgumentException("没有第" + node + "段这一档，三段只到终审(2)");
        }
    }

    /** 该段口径（节点号越界照头一段认是不允许的——直接点明） */
    public static SignMode modeOf(int node) {
        return SignMode.of(node);
    }

    /** 这一节点该几个人落笔 */
    public static int needCount(int node) {
        return SignMode.of(node).needCount();
    }

    /** 系统数出来的当段：空着照初审认 */
    public static int stageOf(TVehSignBill b) {
        return b.getStage() == null ? NODE_FIRST : b.getStage();
    }

    /** 封口只认办讫/驳回两档；待定不算封口（不终止），但也挂着不收笔 */
    public static boolean isSealed(TVehSignBill b) {
        return b.getStatus() != null
                && (b.getStatus() == STATUS_DONE || b.getStatus() == STATUS_DISMISSED);
    }

    /** 终审分歧挂在待定：既不进下一段，也不就此终止 */
    public static boolean isPending(TVehSignBill b) {
        return b.getStatus() != null && b.getStatus() == STATUS_PENDING;
    }

    /** 这一段还收不收笔：封口不收，待定挂着也不收，其余只在当段收 */
    public static boolean acceptsInk(TVehSignBill b) {
        return !isSealed(b) && !isPending(b);
    }

    /** 是不是一句合规的落笔话（只许可移交/不予移交两样） */
    public static boolean validVerdict(String verdict) {
        return VERDICT_MOVE.equals(verdict) || VERDICT_REJECT.equals(verdict);
    }

    /**
     * 点这一节点当轮在册作数的签批笔：只点在册(ink_state=0)的，
     * 顺着落笔先后（落刻，再行号）排。被抹留痕的、更早各轮的由调用方按轮次隔开，不进这一遍点。
     */
    public static List<TVehSignInk> liveSigns(List<TVehSignInk> all) {
        List<TVehSignInk> live = new ArrayList<TVehSignInk>();
        if (all == null) {
            return live;
        }
        for (TVehSignInk n : all) {
            if (n == null) {
                continue;
            }
            boolean stateOk = n.getInkState() == null || n.getInkState() == INK_STATE_LIVE;
            if (stateOk && validVerdict(n.getVerdict())) {
                live.add(n);
            }
        }
        live.sort(Comparator
                .comparing(TVehSignInk::getInkAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(n -> n.getId() == null ? Long.MAX_VALUE : n.getId()));
        return live;
    }

    /**
     * 这一节点此刻怎么算——四问里"齐不齐/往哪走"的唯一答法。
     * 入参是当段当轮点出来的在册签批笔（已按落笔先后排好也行，本方法会再点一遍）。
     */
    public static String evaluate(int node, List<TVehSignInk> rows) {
        SignMode mode = SignMode.of(node);
        List<TVehSignInk> live = liveSigns(rows);
        int have = live.size();

        // 条数与该节点应落人数持平才叫齐；两下里不持平，这一节点就当没齐
        if (have < mode.needCount()) {
            return OUT_NOT_READY;
        }

        if (mode == SignMode.FIRST) {
            // 初审一名就够：头一笔（也只有一笔，同人重复落笔由唯一键挡）的话算
            return VERDICT_MOVE.equals(live.get(0).getVerdict()) ? OUT_PASS : OUT_PRESS_BACK;
        }

        if (mode == SignMode.FIRST_WINS) {
            // 复核两名取先落笔那位的话为准；后一笔照数入账，齐不齐只看条数平没平
            return VERDICT_MOVE.equals(live.get(0).getVerdict()) ? OUT_PASS : OUT_PRESS_BACK;
        }

        // 终审两名同判：都写可移交才算数；都写不可也是这一节点定论不予移交，压回；
        // 一可一不可挂待定——既不进下一节点，也不就此终止
        boolean firstMove = VERDICT_MOVE.equals(live.get(0).getVerdict());
        boolean secondMove = VERDICT_MOVE.equals(live.get(1).getVerdict());
        if (firstMove && secondMove) {
            return OUT_PASS;
        }
        if (!firstMove && !secondMove) {
            return OUT_PRESS_BACK;
        }
        return OUT_PENDING;
    }

    /** 点齐之后还动不动：过档只进紧邻的下一节点，终审齐了就是办讫，没有别处可去 */
    public static boolean isLastNode(int node) {
        return node >= MAX_NODE;
    }

    /** 节点台甫（回读挂名用） */
    public static String nodeName(int node) {
        return SignMode.of(node).title();
    }
}
