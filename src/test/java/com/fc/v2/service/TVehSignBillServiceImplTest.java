package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.VehSignRejectException;
import com.fc.v2.mapper.auto.TVehSignBillMapper;
import com.fc.v2.mapper.auto.TVehSignInkMapper;
import com.fc.v2.mapper.auto.TVehTransferRegisterMapper;
import com.fc.v2.mapper.custom.TVehSignBillBoardMapper;
import com.fc.v2.model.auto.TVehSignBill;
import com.fc.v2.model.auto.TVehSignInk;
import com.fc.v2.model.auto.TVehTransferRegister;
import com.fc.v2.model.custom.TVehSignNodeView;
import com.fc.v2.model.custom.VehSignDismissForm;
import com.fc.v2.model.custom.VehSignForm;
import com.fc.v2.model.custom.VehSignOpenForm;
import com.fc.v2.model.custom.VehSignQuery;
import com.fc.v2.model.custom.VehSignReceipt;
import com.fc.v2.service.impl.TVehSignBillServiceImpl;

/**
 * 核签单（三节点新走法）全口径测试（不启 Spring，桩掉数据层，内存里端到端走）。
 * 钉的事：三段口径互不相同；走只进紧邻、跳关那笔当没落；判不予移交只抹本轮笔并压回，
 * 更早各节点旧笔存底，新旧轮分开；终审一可一不可挂待定不收笔；同人同轮唯一键挡、重递幂等；
 * 驳回全单止住；办讫封档、终审与名册同次回算；在签张数立案+1办讫-1。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ExtendWith(MockitoExtension.class)
public class TVehSignBillServiceImplTest {

    @Mock
    private TVehSignBillMapper billMapper;
    @Mock
    private TVehSignInkMapper inkMapper;
    @Mock
    private TVehTransferRegisterMapper registerMapper;
    @Mock
    private TVehSignBillBoardMapper boardMapper;

    @InjectMocks
    private TVehSignBillServiceImpl service;

    private final Map<Long, TVehSignBill> bills = new LinkedHashMap<Long, TVehSignBill>();
    private final Map<Long, TVehSignInk> inks = new LinkedHashMap<Long, TVehSignInk>();
    private final Map<Long, TVehTransferRegister> registers = new LinkedHashMap<Long, TVehTransferRegister>();
    private long seq = 100;

    @BeforeEach
    public void wire() {
        org.mockito.Mockito.lenient().when(billMapper.insert(any())).thenAnswer(inv -> {
            TVehSignBill b = inv.getArgument(0);
            if (b == null) {
                return 0;
            }
            if (b.getId() == null) {
                b.setId(++seq);
            }
            bills.put(b.getId(), b);
            return 1;
        });
        org.mockito.Mockito.lenient().when(billMapper.updateById(any())).thenAnswer(inv -> {
            TVehSignBill b = inv.getArgument(0);
            if (b == null) {
                return 0;
            }
            bills.put(b.getId(), b);
            return 1;
        });
        org.mockito.Mockito.lenient().when(billMapper.selectById(any())).thenAnswer(
                inv -> bills.get(inv.getArgument(0)));
        org.mockito.Mockito.lenient().when(billMapper.selectList(any())).thenAnswer(
                inv -> filter(bills.values(), inv.getArgument(0), TVehSignBill.class));
        org.mockito.Mockito.lenient().when(billMapper.selectCount(any())).thenAnswer(
                inv -> filter(bills.values(), inv.getArgument(0), TVehSignBill.class).size());

        org.mockito.Mockito.lenient().when(inkMapper.insert(any())).thenAnswer(inv -> {
            TVehSignInk n = inv.getArgument(0);
            if (n == null) {
                return 0;
            }
            if (n.getId() == null) {
                n.setId(++seq);
            }
            inks.put(n.getId(), n);
            return 1;
        });
        org.mockito.Mockito.lenient().when(inkMapper.updateById(any())).thenAnswer(inv -> {
            TVehSignInk n = inv.getArgument(0);
            if (n == null) {
                return 0;
            }
            inks.put(n.getId(), n);
            return 1;
        });
        org.mockito.Mockito.lenient().when(inkMapper.selectList(any())).thenAnswer(
                inv -> filter(inks.values(), inv.getArgument(0), TVehSignInk.class));

        org.mockito.Mockito.lenient().when(registerMapper.insert(any())).thenAnswer(inv -> {
            TVehTransferRegister r = inv.getArgument(0);
            if (r == null) {
                return 0;
            }
            if (r.getId() == null) {
                r.setId(++seq);
            }
            registers.put(r.getId(), r);
            return 1;
        });
        org.mockito.Mockito.lenient().when(registerMapper.selectList(any())).thenAnswer(
                inv -> new ArrayList<TVehTransferRegister>(registers.values()));
    }

    // ------------------------------------------------ 立案

    @Test
    public void openNeedsClueAndStartsAtFirstRoundSigning() {
        assertThrows(VehSignRejectException.class, () -> service.openBill(new VehSignOpenForm()));
        VehSignOpenForm f = new VehSignOpenForm();
        f.setClueNo("XS-2024-LJ-001");
        TVehSignBill b = service.openBill(f);
        assertTrue(b.getBillNo().startsWith("HS"));
        assertEquals(0, b.getStage().intValue());
        assertEquals(1, b.getRoundNo().intValue());
        assertEquals(0, b.getStatus().intValue());
        assertEquals(1, service.countActive());
    }

    // ------------------------------------------------ 初审：一名先落即齐

    @Test
    public void firstNodeOneMoveAdvancesToReview() {
        TVehSignBill b = open("C1");
        VehSignReceipt rc = sign(b.getBillNo(), 0, "监控岗甲", "MOVE", "r1");
        assertTrue(rc.getAccepted());
        assertTrue(rc.getNewInk());
        assertEquals(VehSignRuler.OUT_PASS, rc.getOutcome());
        TVehSignBill now = rc.getBill();
        assertEquals(1, now.getStage().intValue());
        assertEquals(1, now.getRoundNo().intValue());
        assertEquals(0, now.getStatus().intValue());
        // 初审那笔原样在册
        TVehSignNodeView v0 = now.getNodes().get(0);
        assertEquals(1, v0.getSignedCount().intValue());
        assertTrue(v0.getReady());
        assertTrue(v0.getHistory().isEmpty());
    }

    @Test
    public void firstNodeRejectStaysAtFirstButStartsRoundTwoAndVoidsOnlyInk() {
        TVehSignBill b = open("C2");
        VehSignReceipt rc = sign(b.getBillNo(), 0, "监控岗甲", "REJECT", "r1");
        assertEquals(VehSignRuler.OUT_PRESS_BACK, rc.getOutcome());
        TVehSignBill now = rc.getBill();
        // 头一段压回还是头一段，但重走是第二轮：新份旧份分开放
        assertEquals(0, now.getStage().intValue());
        assertEquals(2, now.getRoundNo().intValue());
        TVehSignNodeView v0 = now.getNodes().get(0);
        assertEquals(0, v0.getSignedCount().intValue());
        assertEquals(1, v0.getHistory().size());
        assertEquals(VehSignRuler.INK_STATE_VOID, v0.getHistory().get(0).getInkState().intValue());
    }

    // ------------------------------------------------ 复核：两名取先落笔那位的话为准

    @Test
    public void reviewTakesFirstInkAsCallEvenIfSecondDisagrees() {
        TVehSignBill b = reachReview("C3");
        // 先可后不可：取先落笔那位，过；两人各写了什么都在册
        VehSignReceipt a = sign(b.getBillNo(), 1, "法规甲", "MOVE", "r2a");
        assertEquals(VehSignRuler.OUT_NOT_READY, a.getOutcome());
        VehSignReceipt c = sign(b.getBillNo(), 1, "法规乙", "REJECT", "r2b");
        assertEquals(VehSignRuler.OUT_PASS, c.getOutcome());
        assertEquals(2, c.getBill().getStage().intValue());
        TVehSignNodeView v1 = c.getBill().getNodes().get(1);
        assertEquals(2, v1.getSignedCount().intValue());
        assertEquals("MOVE", v1.getCurrent().get(0).getVerdict());
        assertEquals("REJECT", v1.getCurrent().get(1).getVerdict());
    }

    @Test
    public void reviewFirstRejectPressesBackToFirstRoundTwoKeepingEarlierInks() {
        TVehSignBill b = reachReview("C4");
        sign(b.getBillNo(), 1, "法规甲", "REJECT", "r2a");
        VehSignReceipt c = sign(b.getBillNo(), 1, "法规乙", "REJECT", "r2b");
        assertEquals(VehSignRuler.OUT_PRESS_BACK, c.getOutcome());
        TVehSignBill now = c.getBill();
        // 压回初审：初审从第二轮办；初审头一轮的旧笔原样存底，复核两笔被抹进 history
        assertEquals(0, now.getStage().intValue());
        assertEquals(2, now.getRoundNo().intValue());
        TVehSignNodeView v0 = now.getNodes().get(0);
        assertEquals(0, v0.getSignedCount().intValue());
        assertEquals(1, v0.getHistory().size());
        assertEquals(VehSignRuler.INK_STATE_LIVE, v0.getHistory().get(0).getInkState().intValue());
        TVehSignNodeView v1 = now.getNodes().get(1);
        assertEquals(0, v1.getSignedCount().intValue());
        assertEquals(2, v1.getHistory().size());
        assertTrue(v1.getHistory().stream().allMatch(n -> n.getInkState() == VehSignRuler.INK_STATE_VOID));
    }

    // ------------------------------------------------ 终审：两名同判

    @Test
    public void finalConsensusMovesDoneAndWritesRegisterInSameOutcome() {
        TVehSignBill b = reachFinal("C5");
        sign(b.getBillNo(), 2, "分管甲", "MOVE", "r3a");
        VehSignReceipt c = sign(b.getBillNo(), 2, "分管乙", "MOVE", "r3b");
        assertEquals(VehSignRuler.OUT_PASS, c.getOutcome());
        TVehSignBill now = c.getBill();
        assertEquals(VehSignRuler.STATUS_DONE, now.getStatus().intValue());
        assertEquals(VehSignRuler.VERDICT_MOVE, now.getFinalDecision());
        assertNotNull(now.getClosedAt());
        // 终审结论与名册行同一次回算
        assertTrue(c.getRegistered());
        assertEquals(1, registers.size());
        TVehTransferRegister reg = registers.values().iterator().next();
        assertEquals(now.getId(), reg.getBillId());
        assertEquals(now.getBillNo(), reg.getBillNo());
        assertEquals("MOVE", reg.getDecision());
        assertEquals(now.getClosedAt(), reg.getDecidedAt());
        // 办讫即减一：在签张数为 0
        assertEquals(0, service.countActive());
    }

    @Test
    public void finalSplitHangsPendingAndAcceptsNoMoreInk() {
        TVehSignBill b = reachFinal("C6");
        sign(b.getBillNo(), 2, "分管甲", "MOVE", "r3a");
        VehSignReceipt split = sign(b.getBillNo(), 2, "分管乙", "REJECT", "r3b");
        assertEquals(VehSignRuler.OUT_PENDING, split.getOutcome());
        TVehSignBill hung = split.getBill();
        assertEquals(VehSignRuler.STATUS_PENDING, hung.getStatus().intValue());
        assertEquals(2, hung.getStage().intValue());
        // 两笔都没抹
        assertEquals(2, hung.getNodes().get(2).getSignedCount().intValue());
        // 待定占在办数
        assertEquals(1, service.countActive());

        VehSignReceipt more = sign(hung.getBillNo(), 2, "分管丙", "MOVE", "r3c");
        assertFalse(more.getAccepted());
        assertFalse(more.getNewInk());
        // 待定也不走驳回这道门
        VehSignDismissForm d = new VehSignDismissForm();
        d.setBillNo(hung.getBillNo());
        d.setClaimNode(2);
        d.setSignerNo("分管甲");
        d.setReason("想顺手终止");
        assertFalse(service.dismiss(d).getAccepted());
    }

    @Test
    public void finalBothRejectPressesBackToReviewNewRound() {
        TVehSignBill b = reachFinal("C7");
        sign(b.getBillNo(), 2, "分管甲", "REJECT", "r3a");
        VehSignReceipt c = sign(b.getBillNo(), 2, "分管乙", "REJECT", "r3b");
        assertEquals(VehSignRuler.OUT_PRESS_BACK, c.getOutcome());
        TVehSignBill now = c.getBill();
        assertEquals(1, now.getStage().intValue());
        assertEquals(2, now.getRoundNo().intValue());
        assertEquals(0, now.getStatus().intValue());
        // 终审两笔抹掉，初审/复核定论那轮的旧笔都在册
        TVehSignNodeView v2 = now.getNodes().get(2);
        assertEquals(0, v2.getSignedCount().intValue());
        assertEquals(2, v2.getHistory().size());
        // 复核定论那轮的旧笔没被抹（state 仍在册），只是重走另起第 2 轮，旧份归 history
        TVehSignNodeView v1 = now.getNodes().get(1);
        assertEquals(0, v1.getSignedCount().intValue());
        assertEquals(2, v1.getHistory().size());
        assertTrue(v1.getHistory().stream().allMatch(n -> n.getInkState() == VehSignRuler.INK_STATE_LIVE));
    }

    // ------------------------------------------------ 口信 / 跳关 / 幂等 / 并发

    @Test
    public void claimNodeMismatchDropsTheInkAndBillStaysPut() {
        TVehSignBill b = reachReview("C8");
        // 跳过复核径直由终审落笔：那一笔当没落，整单照旧停在复核
        VehSignReceipt rc = sign(b.getBillNo(), 2, "分管甲", "MOVE", "skip");
        assertFalse(rc.getAccepted());
        assertFalse(rc.getNewInk());
        assertFalse(rc.getClaimMatched());
        assertEquals(1, rc.getBill().getStage().intValue());
        // 跳关那笔没落：复核/终审一段新行都没有（初审那笔是 reachReview 时落的，照旧在）
        long beyondFirst = inks.values().stream().filter(n -> n.getNodeNo() != null && n.getNodeNo() >= 1).count();
        assertEquals(0, beyondFirst);
        // 上一节点没点齐，下一节点(claimNode 与系统段不符)落的笔一律不作数；不带口信（null）照落
        VehSignReceipt ok = sign(b.getBillNo(), null, "法规甲", "MOVE", "r2a");
        assertTrue(ok.getAccepted());
    }

    @Test
    public void sameSignerSameRoundSecondInkDoesNotMakeNewLine() {
        TVehSignBill b = reachReview("C9");
        sign(b.getBillNo(), 1, "法规甲", "MOVE", "r2a");
        VehSignReceipt again = sign(b.getBillNo(), 1, "法规甲", "REJECT", "r2-again");
        assertTrue(again.getAccepted());
        assertFalse(again.getNewInk());
        long live = inks.values().stream().filter(n -> n.getInkState() == 0
                && n.getNodeNo() == 1 && n.getRoundNo() == 1).count();
        assertEquals(1, live);
    }

    @Test
    public void duplicateRequestIsIdempotentEvenFromAnotherSigner() {
        TVehSignBill b = reachReview("C10");
        VehSignReceipt first = sign(b.getBillNo(), 1, "法规甲", "MOVE", "DUP-KEY");
        assertTrue(first.getNewInk());
        VehSignReceipt second = sign(b.getBillNo(), 1, "法规乙", "REJECT", "DUP-KEY");
        assertTrue(second.getAccepted());
        assertFalse(second.getNewInk());
        long live = inks.values().stream().filter(n -> n.getInkState() == 0 && n.getNodeNo() == 1).count();
        assertEquals(1, live);
    }

    @Test
    public void concurrentDuplicateFromDatabaseIsRejectedByUniqueKeyPath() {
        TVehSignBill b = reachReview("C11");
        // 服务先查没有；插的那一刹那并发那笔先进来：库里唯一键抛 DuplicateKeyException
        when(inkMapper.insert(any())).thenAnswer(inv -> {
            TVehSignInk n = inv.getArgument(0);
            n.setId(++seq);
            inks.put(n.getId(), n); // 同行已可见（模拟并发先提交）
            throw new DuplicateKeyException("uk_veh_sign_ink_signer");
        });
        VehSignForm f = form(b.getBillNo(), 1, "法规甲", "MOVE", "race");
        VehSignReceipt rc = service.sign(f);
        assertTrue(rc.getAccepted());
        assertFalse(rc.getNewInk());
    }

    @Test
    public void badVerdictAndMissingSignerAreRejected() {
        TVehSignBill b = open("C12");
        VehSignForm f = form(b.getBillNo(), 0, "甲", "WAIT", "x");
        assertThrows(VehSignRejectException.class, () -> service.sign(f));
        f.setVerdict("MOVE");
        f.setSignerNo("  ");
        assertThrows(VehSignRejectException.class, () -> service.sign(f));
    }

    // ------------------------------------------------ 驳回与封档

    @Test
    public void dismissStopsTheWholeBillAndLaterStagesTakeNoInk() {
        TVehSignBill b = reachReview("C13");
        VehSignDismissForm d = new VehSignDismissForm();
        d.setBillNo(b.getBillNo());
        d.setClaimNode(1);
        d.setSignerNo("法规甲");
        d.setReason("线索不实");
        d.setRequestId("D1");
        VehSignReceipt rc = service.dismiss(d);
        assertTrue(rc.getAccepted());
        assertEquals(VehSignRuler.STATUS_DISMISSED, rc.getBillStatus().intValue());
        assertNotNull(rc.getBill().getClosedAt());
        // 后面各节点不再收笔
        VehSignReceipt late = sign(b.getBillNo(), 2, "分管甲", "MOVE", "late");
        assertFalse(late.getAccepted());
        // 驳回是终态，减出在办数
        assertEquals(0, service.countActive());
        // 同回驳回事务重递：不重复收口
        VehSignReceipt again = service.dismiss(d);
        assertFalse(again.getAccepted());
    }

    @Test
    public void doneBillIsSealedNoInkNoChange() {
        TVehSignBill b = reachFinal("C14");
        sign(b.getBillNo(), 2, "分管甲", "MOVE", "a");
        sign(b.getBillNo(), 2, "分管乙", "MOVE", "b");
        VehSignReceipt more = sign(b.getBillNo(), 2, "分管丙", "MOVE", "c");
        assertFalse(more.getAccepted());
        VehSignDismissForm d = new VehSignDismissForm();
        d.setBillNo(b.getBillNo());
        d.setClaimNode(2);
        d.setSignerNo("分管甲");
        d.setReason("办讫后想驳回");
        assertFalse(service.dismiss(d).getAccepted());
        // 名册仍只一行（同判那一次回算，不重不漏）
        assertEquals(1, registers.size());
    }

    @Test
    public void unknownBillAndBadDismissComeBackGracefully() {
        VehSignForm f = form("NO-SUCH", 0, "甲", "MOVE", "x");
        VehSignReceipt rc = service.sign(f);
        assertFalse(rc.getAccepted());
        assertNull(service.detail("NO-SUCH"));

        VehSignDismissForm d = new VehSignDismissForm();
        d.setBillNo("NO-SUCH");
        d.setSignerNo("甲");
        d.setReason("r");
        assertFalse(service.dismiss(d).getAccepted());
    }

    // ------------------------------------------------ 回读点条数与台账同源

    @Test
    public void detailCountsInksNodeByNodeAndStandsAlonePerNode() {
        TVehSignBill b = reachFinal("C15");
        TVehSignBill detail = service.detail(b.getBillNo());
        assertEquals(3, detail.getNodes().size());
        // 随便抽一段单看也站得住：初审 1、复核 2 都在册且齐，终审没落 0
        assertEquals(1, detail.getNodes().get(0).getSignedCount().intValue());
        assertEquals(2, detail.getNodes().get(1).getSignedCount().intValue());
        assertEquals(0, detail.getNodes().get(2).getSignedCount().intValue());
        assertTrue(detail.getNodes().get(0).getReady());
        assertTrue(detail.getNodes().get(1).getReady());
        assertFalse(detail.getNodes().get(2).getReady());
    }

    @Test
    public void boardHeadCountComesFromSameSnapshotAsRows() {
        reachFinal("C16");          // 在终审在签
        TVehSignBill done = reachFinal("C17");
        sign(done.getBillNo(), 2, "分管甲", "MOVE", "a");
        sign(done.getBillNo(), 2, "分管乙", "MOVE", "b"); // 办讫减一

        TVehSignBill rowForBoard = bills.values().iterator().next();
        rowForBoard.setBoardTotal(2L);
        rowForBoard.setBoardActiveCount(1);
        org.mockito.Mockito.lenient().when(boardMapper.selectBoardPage(any(VehSignQuery.class)))
                .thenReturn(new ArrayList<TVehSignBill>(bills.values()));
        com.fc.v2.model.custom.VehSignBoard board = service.openBoard(new VehSignQuery());
        assertEquals(2L, board.getTotalCount().longValue());
        // 屏头数与回库逐张点的数对得齐：一张在签、一张办讫
        assertEquals(1, board.getActiveCount().intValue());
        assertEquals(1, service.countActive());
    }

    // ------------------------------------------------ 帮手

    private TVehSignBill open(String clue) {
        VehSignOpenForm f = new VehSignOpenForm();
        f.setClueNo(clue);
        return service.openBill(f);
    }

    private TVehSignBill reachReview(String clue) {
        TVehSignBill b = open(clue);
        sign(b.getBillNo(), 0, "监控岗甲", "MOVE", clue + "-n0");
        return service.detail(b.getBillNo());
    }

    private TVehSignBill reachFinal(String clue) {
        TVehSignBill b = reachReview(clue);
        sign(b.getBillNo(), 1, "法规甲", "MOVE", clue + "-n1a");
        sign(b.getBillNo(), 1, "法规乙", "MOVE", clue + "-n1b");
        return service.detail(b.getBillNo());
    }

    private VehSignReceipt sign(String billNo, Integer claimNode, String signer, String verdict, String req) {
        return service.sign(form(billNo, claimNode, signer, verdict, req));
    }

    private VehSignForm form(String billNo, Integer claimNode, String signer, String verdict, String req) {
        VehSignForm f = new VehSignForm();
        f.setBillNo(billNo);
        f.setClaimNode(claimNode);
        f.setSignerNo(signer);
        f.setVerdict(verdict);
        f.setRequestId(req);
        return f;
    }

    // ------------------------------------------------ 极简 Wrapper 条件解析（把内存库当库筛）

    private static final Pattern PH = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(\\w+)}");
    private static final Pattern EQ = Pattern.compile("(?i)([a-z_]+)\\s*=\\s*'([^']*)'");
    private static final Pattern IN = Pattern.compile("(?i)([a-z_]+)\\s+IN\\s*\\(([^)]*)\\)");

    private <T> List<T> filter(java.util.Collection<T> all, QueryWrapper<?> w, Class<T> type) {
        String sql = w.getSqlSegment() == null ? "" : w.getSqlSegment();
        Map<String, Object> params = w.getParamNameValuePairs();
        Matcher pm = PH.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (pm.find()) {
            Object v = params.get(pm.group(1));
            pm.appendReplacement(sb, Matcher.quoteReplacement("'" + String.valueOf(v) + "'"));
        }
        pm.appendTail(sb);
        String resolved = sb.toString();

        Map<String, String> eq = new LinkedHashMap<String, String>();
        Matcher em = EQ.matcher(resolved);
        while (em.find()) {
            eq.put(em.group(1), em.group(2));
        }
        Map<String, List<String>> in = new LinkedHashMap<String, List<String>>();
        Matcher im = IN.matcher(resolved);
        while (im.find()) {
            List<String> vs = new ArrayList<String>();
            for (String one : im.group(2).split(",")) {
                String t = one.trim();
                if (t.startsWith("'") && t.endsWith("'") && t.length() >= 2) {
                    t = t.substring(1, t.length() - 1);
                }
                vs.add(t);
            }
            in.put(im.group(1), vs);
        }

        List<T> out = new ArrayList<T>();
        for (T row : all) {
            boolean keep = true;
            for (Map.Entry<String, String> e : eq.entrySet()) {
                Object got = readColumn(row, e.getKey());
                if (got == null || !String.valueOf(got).equals(e.getValue())) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                for (Map.Entry<String, List<String>> e : in.entrySet()) {
                    Object got = readColumn(row, e.getKey());
                    if (got == null || !e.getValue().contains(String.valueOf(got))) {
                        keep = false;
                        break;
                    }
                }
            }
            if (keep) {
                out.add(row);
            }
        }
        if (resolved.toUpperCase().contains("LIMIT")) {
            return out.isEmpty() ? out : Arrays.asList(out.get(0));
        }
        return out;
    }

    private Object readColumn(Object row, String column) {
        String prop;
        if (row instanceof TVehSignBill) {
            prop = columnProp(column, "bill_no:billNo", "clue_no:clueNo", "node_no:stage",
                    "round_no:roundNo", "status:status", "request_id:requestId", "del_flag:delFlag");
            TVehSignBill b = (TVehSignBill) row;
            switch (prop) {
                case "billNo": return b.getBillNo();
                case "clueNo": return b.getClueNo();
                case "stage": return b.getStage();
                case "roundNo": return b.getRoundNo();
                case "status": return b.getStatus();
                case "requestId": return b.getRequestId();
                case "delFlag": return b.getDelFlag();
                default: return null;
            }
        }
        if (row instanceof TVehSignInk) {
            TVehSignInk n = (TVehSignInk) row;
            switch (column) {
                case "bill_id": return n.getBillId();
                case "node_no": return n.getNodeNo();
                case "round_no": return n.getRoundNo();
                case "signer_no": return n.getSignerNo();
                case "ink_state": return n.getInkState();
                case "request_id": return n.getRequestId();
                case "del_flag": return n.getDelFlag();
                default: return null;
            }
        }
        if (row instanceof TVehTransferRegister) {
            return "del_flag".equals(column) ? ((TVehTransferRegister) row).getDelFlag() : null;
        }
        return null;
    }

    private String columnProp(String column, String... pairs) {
        for (String p : pairs) {
            String[] kv = p.split(":");
            if (kv[0].equals(column)) {
                return kv[1];
            }
        }
        return column;
    }
}
