package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.auto.TVehXferSign;

/**
 * 核签单尺子纯算用例（不碰库）。
 * 钉的事：三段应落人数各是各；头两段取先落笔那位的话；第三段两名同判——
 * 都可才过、都不予移交即压回、一可一不可挂待定；人齐只认点出来的条数与应落持平；
 * 压回只收一格；封卷不再收笔。三段规矩互不抄。
 */
public class VehXferRulerTest {

    private TVehXferSign s(long id, int node, int round, String signer, String decision) {
        TVehXferSign x = new TVehXferSign();
        x.setId(id);
        x.setBillId(1L);
        x.setNodeNo(node);
        x.setRoundNo(round);
        x.setSignerNo(signer);
        x.setDecision(decision);
        return x;
    }

    private TVehXferBill bill(Integer node, Integer round, Integer status) {
        TVehXferBill b = new TVehXferBill();
        b.setId(1L);
        b.setNodeNo(node);
        b.setRoundNo(round);
        b.setStatus(status);
        return b;
    }

    @Test
    public void needCountDiffersPerNode() {
        // 三段该几个人说话互不相同，不许彼此抄
        assertEquals(1, VehXferRuler.needCount(0));
        assertEquals(2, VehXferRuler.needCount(1));
        assertEquals(2, VehXferRuler.needCount(2));
    }

    @Test
    public void firstNodeSinglePenDecides() {
        // 第一段一名即可：那一笔的话就是本段的话
        assertTrue(VehXferRuler.nodePassed(0, Arrays.asList(s(1, 0, 1, "甲", "MOVE"))));
        assertFalse(VehXferRuler.nodePassed(0, Arrays.asList(s(1, 0, 1, "甲", "HOLD"))));
        assertFalse(VehXferRuler.nodePassed(0, new ArrayList<TVehXferSign>()));
    }

    @Test
    public void secondNodeTakesFirstPenWord() {
        // 第二段取先落笔那位的话为准：先可后可→过；先不可后可→不过；只落一笔不算齐
        assertTrue(VehXferRuler.nodePassed(1,
                Arrays.asList(s(1, 1, 1, "甲", "MOVE"), s(2, 1, 1, "乙", "HOLD"))));
        assertFalse(VehXferRuler.nodePassed(1,
                Arrays.asList(s(1, 1, 1, "甲", "HOLD"), s(2, 1, 1, "乙", "MOVE"))));
        assertFalse(VehXferRuler.nodePassed(1, Arrays.asList(s(1, 1, 1, "甲", "MOVE"))));
    }

    @Test
    public void finalNodeNeedsUnanimousMove() {
        // 第三段两名同判：都可移交才算过；一可一不可不过；都不予移交也不过（走压回）
        assertTrue(VehXferRuler.nodePassed(2,
                Arrays.asList(s(1, 2, 1, "甲", "MOVE"), s(2, 2, 1, "乙", "MOVE"))));
        assertFalse(VehXferRuler.nodePassed(2,
                Arrays.asList(s(1, 2, 1, "甲", "MOVE"), s(2, 2, 1, "乙", "HOLD"))));
        assertFalse(VehXferRuler.nodePassed(2,
                Arrays.asList(s(1, 2, 1, "甲", "HOLD"), s(2, 2, 1, "乙", "HOLD"))));
        // 第三段不许拿第二段的规矩去压：先可后不可不算过
        assertFalse(VehXferRuler.nodePassed(2,
                Arrays.asList(s(1, 2, 1, "甲", "MOVE"), s(2, 2, 1, "乙", "HOLD"))));
    }

    @Test
    public void finalNodeAllHoldAndPending() {
        List<TVehXferSign> bothHold = Arrays.asList(s(1, 2, 1, "甲", "HOLD"), s(2, 2, 1, "乙", "HOLD"));
        List<TVehXferSign> mixed = Arrays.asList(s(1, 2, 1, "甲", "MOVE"), s(2, 2, 1, "乙", "HOLD"));
        List<TVehXferSign> bothMove = Arrays.asList(s(1, 2, 1, "甲", "MOVE"), s(2, 2, 1, "乙", "MOVE"));
        // 同判不予移交只认第三段
        assertTrue(VehXferRuler.nodeAllHold(2, bothHold));
        assertFalse(VehXferRuler.nodeAllHold(2, mixed));
        assertFalse(VehXferRuler.nodeAllHold(1,
                Arrays.asList(s(1, 1, 1, "甲", "HOLD"), s(2, 1, 1, "乙", "HOLD"))));
        // 待定：一可一不可挂着，既不进也不终
        assertTrue(VehXferRuler.nodePending(2, mixed));
        assertFalse(VehXferRuler.nodePending(2, bothMove));
        assertFalse(VehXferRuler.nodePending(2, bothHold));
        assertFalse(VehXferRuler.nodePending(1, mixed));
    }

    @Test
    public void fullOnlyWhenCountedEqualsNeeded() {
        // 点出来的条数与应落人数持平才叫齐，不持平就当没齐
        assertFalse(VehXferRuler.nodeFull(1, Arrays.asList(s(1, 1, 1, "甲", "MOVE"))));
        assertTrue(VehXferRuler.nodeFull(1,
                Arrays.asList(s(1, 1, 1, "甲", "MOVE"), s(2, 1, 1, "乙", "MOVE"))));
        assertTrue(VehXferRuler.nodeFull(0, Arrays.asList(s(1, 0, 1, "甲", "MOVE"))));
    }

    @Test
    public void roundAndEffectiveSignsSplitByRound() {
        // 新份旧份分开放：本段有效那轮只认最后一轮，旧轮存底不并数
        List<TVehXferSign> all = Arrays.asList(
                s(1, 0, 1, "甲", "MOVE"),
                s(2, 0, 2, "乙", "MOVE"),
                s(3, 1, 2, "丙", "MOVE"));
        assertEquals(1, VehXferRuler.roundSigns(all, 0, 1).size());
        assertEquals(1, VehXferRuler.roundSigns(all, 0, 2).size());
        assertEquals(1, VehXferRuler.effectiveSigns(all, 0).size());
        assertEquals("乙", VehXferRuler.effectiveSigns(all, 0).get(0).getSignerNo());
        assertEquals(1, VehXferRuler.effectiveSigns(all, 1).size());
        assertEquals(0, VehXferRuler.effectiveSigns(all, 2).size());
    }

    @Test
    public void pushBackTakesOnlyOneCell() {
        // 收只收一格；头一段退无可退原地重走
        assertEquals(1, VehXferRuler.pushBackTarget(2));
        assertEquals(0, VehXferRuler.pushBackTarget(1));
        assertEquals(0, VehXferRuler.pushBackTarget(0));
    }

    @Test
    public void terminalBillTakesNoMorePens() {
        // 点齐之后（办讫/驳回）还收不收笔：不收
        assertTrue(VehXferRuler.acceptSign(bill(0, 1, 0)));
        assertFalse(VehXferRuler.acceptSign(bill(2, 1, 1)));
        assertFalse(VehXferRuler.acceptSign(bill(1, 1, 2)));
        assertFalse(VehXferRuler.nextOpen(bill(2, 1, 1), new ArrayList<TVehXferSign>()));
    }

    @Test
    public void signBlockReasonSpeaksForTerminalAndFull() {
        // 封卷缘由与"人齐挂待定"缘由只认这一处的原话
        assertEquals("这张单已移交办讫，封卷只读，一笔也加不进",
                VehXferRuler.signBlockReason(bill(2, 1, 1), 0));
        assertEquals("这张单已驳回止住，一笔也加不进",
                VehXferRuler.signBlockReason(bill(1, 1, 2), 0));
        assertEquals("本段应落2人已齐，挂在待定上等处置，不再收笔",
                VehXferRuler.signBlockReason(bill(2, 1, 0), 2));
        assertNull(VehXferRuler.signBlockReason(bill(1, 1, 0), 1));
    }

    @Test
    public void nextOpenFollowsCurrentNodePens() {
        // 下一节点开不开：当段本轮的笔按口径过关才算开
        TVehXferBill b = bill(1, 1, 0);
        List<TVehXferSign> notYet = Arrays.asList(s(1, 1, 1, "甲", "MOVE"));
        assertFalse(VehXferRuler.nextOpen(b, notYet));
        List<TVehXferSign> passed = Arrays.asList(s(1, 1, 1, "甲", "MOVE"), s(2, 1, 1, "乙", "HOLD"));
        assertTrue(VehXferRuler.nextOpen(b, passed));
    }
}
