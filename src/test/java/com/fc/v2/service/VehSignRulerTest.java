package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TVehSignInk;

/**
 * 核签单尺子纯算用例。三段规矩互不相同，不许彼此抄：
 * 初审一名先落即齐；复核两名取先落笔那位的话为准；终审两名同判，分歧挂待定。
 * 点条数只点当轮在册(ink_state=0)的，被抹的与条数不够都当没齐。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class VehSignRulerTest {

    private TVehSignInk ink(String signer, String verdict, Date at, Integer state) {
        TVehSignInk n = new TVehSignInk();
        n.setSignerNo(signer);
        n.setVerdict(verdict);
        n.setInkAt(at);
        n.setInkState(state);
        return n;
    }

    private Date t(int sec) {
        return new Date(1000L * 60L * sec);
    }

    @Test
    public void nodeRulesAreDifferentAndFixed() {
        assertEquals(1, VehSignRuler.needCount(VehSignRuler.NODE_FIRST));
        assertEquals(2, VehSignRuler.needCount(VehSignRuler.NODE_FIRST_WINS));
        assertEquals(2, VehSignRuler.needCount(VehSignRuler.NODE_CONSENSUS));
        assertEquals("FIRST", VehSignRuler.modeOf(0).name());
        assertEquals("FIRST_WINS", VehSignRuler.modeOf(1).name());
        assertEquals("CONSENSUS", VehSignRuler.modeOf(2).name());
        assertThrows(IllegalArgumentException.class, () -> VehSignRuler.modeOf(3));
    }

    @Test
    public void firstNodeOneSignerSettles() {
        // 初审一名就够：可移交即过，不予移交即压回
        List<TVehSignInk> move = Arrays.asList(ink("监控岗甲", VehSignRuler.VERDICT_MOVE, t(1), 0));
        assertEquals(VehSignRuler.OUT_PASS, VehSignRuler.evaluate(0, move));
        List<TVehSignInk> reject = Arrays.asList(ink("监控岗甲", VehSignRuler.VERDICT_REJECT, t(1), 0));
        assertEquals(VehSignRuler.OUT_PRESS_BACK, VehSignRuler.evaluate(0, reject));
        // 一笔没落：没齐
        assertEquals(VehSignRuler.OUT_NOT_READY, VehSignRuler.evaluate(0, new ArrayList<TVehSignInk>()));
    }

    @Test
    public void reviewNodeTakesFirstInkAsTheCall() {
        // 复核两名取先落笔那位的话为准：先可后不可仍过；先不可后可压回；后一笔照样入账
        List<TVehSignInk> moveThenReject = Arrays.asList(
                ink("法规甲", VehSignRuler.VERDICT_MOVE, t(1), 0),
                ink("法规乙", VehSignRuler.VERDICT_REJECT, t(2), 0));
        assertEquals(VehSignRuler.OUT_PASS, VehSignRuler.evaluate(1, moveThenReject));
        List<TVehSignInk> rejectThenMove = Arrays.asList(
                ink("法规甲", VehSignRuler.VERDICT_REJECT, t(1), 0),
                ink("法规乙", VehSignRuler.VERDICT_MOVE, t(2), 0));
        assertEquals(VehSignRuler.OUT_PRESS_BACK, VehSignRuler.evaluate(1, rejectThenMove));
        // 两人都写不可：定论不予移交
        List<TVehSignInk> bothReject = Arrays.asList(
                ink("法规甲", VehSignRuler.VERDICT_REJECT, t(1), 0),
                ink("法规乙", VehSignRuler.VERDICT_REJECT, t(2), 0));
        assertEquals(VehSignRuler.OUT_PRESS_BACK, VehSignRuler.evaluate(1, bothReject));
        // 只落一名：条数没持平，没齐，不许拿初审一名的规矩来解
        assertEquals(VehSignRuler.OUT_NOT_READY,
                VehSignRuler.evaluate(1, Arrays.asList(ink("法规甲", VehSignRuler.VERDICT_MOVE, t(1), 0))));
    }

    @Test
    public void finalNodeNeedsTwoMoveAnythingElsePendingOrBack() {
        List<TVehSignInk> bothMove = Arrays.asList(
                ink("分管甲", VehSignRuler.VERDICT_MOVE, t(1), 0),
                ink("分管乙", VehSignRuler.VERDICT_MOVE, t(2), 0));
        assertEquals(VehSignRuler.OUT_PASS, VehSignRuler.evaluate(2, bothMove));

        // 一可一不可：挂待定，既不进下一段也不终止——不许拿复核"先落为准"的规矩压
        List<TVehSignInk> split1 = Arrays.asList(
                ink("分管甲", VehSignRuler.VERDICT_MOVE, t(1), 0),
                ink("分管乙", VehSignRuler.VERDICT_REJECT, t(2), 0));
        assertEquals(VehSignRuler.OUT_PENDING, VehSignRuler.evaluate(2, split1));
        List<TVehSignInk> split2 = Arrays.asList(
                ink("分管甲", VehSignRuler.VERDICT_REJECT, t(1), 0),
                ink("分管乙", VehSignRuler.VERDICT_MOVE, t(2), 0));
        assertEquals(VehSignRuler.OUT_PENDING, VehSignRuler.evaluate(2, split2));

        // 两笔都不可：这一节点定论不予移交，压回
        List<TVehSignInk> bothReject = Arrays.asList(
                ink("分管甲", VehSignRuler.VERDICT_REJECT, t(1), 0),
                ink("分管乙", VehSignRuler.VERDICT_REJECT, t(2), 0));
        assertEquals(VehSignRuler.OUT_PRESS_BACK, VehSignRuler.evaluate(2, bothReject));

        // 并发前后脚：只落一名仍没齐
        assertEquals(VehSignRuler.OUT_NOT_READY,
                VehSignRuler.evaluate(2, Arrays.asList(ink("分管甲", VehSignRuler.VERDICT_MOVE, t(1), 0))));
    }

    @Test
    public void firstInkMeansEarliestByOrderNotListPosition() {
        // 传进来的次序乱了也照落笔先后点：先落那位是"不可"，复核定论压回
        List<TVehSignInk> shuffled = Arrays.asList(
                ink("法规乙", VehSignRuler.VERDICT_MOVE, t(9), 0),
                ink("法规甲", VehSignRuler.VERDICT_REJECT, t(1), 0));
        assertEquals(VehSignRuler.OUT_PRESS_BACK, VehSignRuler.evaluate(1, shuffled));
    }

    @Test
    public void voidedInksAreNotCounted() {
        // 本轮被抹(state=1)的旧笔不计条数：只剩一名在册 → 复核没齐
        List<TVehSignInk> rows = Arrays.asList(
                ink("法规甲(旧轮被抹)", VehSignRuler.VERDICT_MOVE, t(1), 1),
                ink("法规甲", VehSignRuler.VERDICT_MOVE, t(2), 0));
        List<TVehSignInk> live = VehSignRuler.liveSigns(rows);
        assertEquals(1, live.size());
        assertEquals(VehSignRuler.OUT_NOT_READY, VehSignRuler.evaluate(1, rows));
        assertFalse(VehSignRuler.validVerdict(null));
        assertTrue(VehSignRuler.validVerdict(VehSignRuler.VERDICT_MOVE));
    }
}
