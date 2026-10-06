package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.github.pagehelper.PageHelper;
import com.fc.v2.mapper.auto.TVehXferBillMapper;
import com.fc.v2.mapper.auto.TVehXferRosterMapper;
import com.fc.v2.mapper.auto.TVehXferSignMapper;
import com.fc.v2.mapper.custom.TVehXferBillBoardMapper;
import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.auto.TVehXferRoster;
import com.fc.v2.model.auto.TVehXferSign;
import com.fc.v2.model.custom.VehXferAuditBoard;
import com.fc.v2.model.custom.VehXferBoard;
import com.fc.v2.model.custom.VehXferOpenForm;
import com.fc.v2.model.custom.VehXferQuery;
import com.fc.v2.model.custom.VehXferSignForm;
import com.fc.v2.model.custom.VehXferSignReceipt;
import com.fc.v2.service.impl.TVehXferBillServiceImpl;

/**
 * 核签单系统盯办全口径测试（不启 Spring，桩掉数据层）。
 * 钉的事：立案从头一段第一轮起；第一段一名即齐；第二段两名取先落笔那位的话、票次累加；
 * 第三段两名同判——都可办讫、都不予移交压回、一可一不可挂待定不进不终；
 * 跳段那笔当它没落；口信对不上不作调和只认系统那一处；重复递交幂等不另起行；
 * 唯一键挡住同人重笔；不予移交抹笔与退档同落、旧笔存底新份另起一轮；
 * 办讫封卷只读写名册同源；驳回全单止住；屏上数与逐节点点算同一回；事后翻查一笔不差。
 */
@ExtendWith(MockitoExtension.class)
public class TVehXferBillServiceImplTest {

    @Mock
    private TVehXferBillMapper vehXferBillMapper;
    @Mock
    private TVehXferSignMapper vehXferSignMapper;
    @Mock
    private TVehXferRosterMapper vehXferRosterMapper;
    @Mock
    private TVehXferBillBoardMapper boardMapper;

    @InjectMocks
    private TVehXferBillServiceImpl service;

    private TVehXferBill store;
    private List<TVehXferSign> signs;
    private List<TVehXferRoster> rosters;
    private long signSeq;

    @BeforeEach
    public void wireRoundtrip() {
        signs = new ArrayList<TVehXferSign>();
        rosters = new ArrayList<TVehXferRoster>();
        signSeq = 0;
        // 模拟落库：立案派 id、按号/按主键回同一卷、落笔派自增号（落笔先后只认这把尺）
        org.mockito.Mockito.lenient().when(vehXferBillMapper.insert(any())).thenAnswer(inv -> {
            TVehXferBill r = inv.getArgument(0);
            r.setId(1L);
            store = r;
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferBillMapper.selectById(1L)).thenAnswer(inv -> store);
        org.mockito.Mockito.lenient().when(vehXferBillMapper.selectOne(any())).thenAnswer(inv -> store);
        org.mockito.Mockito.lenient().when(vehXferBillMapper.updateById(any())).thenAnswer(inv -> {
            store = inv.getArgument(0);
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferBillMapper.deleteById(1L)).thenAnswer(inv -> {
            store = null;
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferSignMapper.insert(any())).thenAnswer(inv -> {
            TVehXferSign s = inv.getArgument(0);
            s.setId(++signSeq);
            signs.add(s);
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferSignMapper.selectList(any()))
                .thenAnswer(inv -> new ArrayList<TVehXferSign>(signs));
        org.mockito.Mockito.lenient().when(vehXferSignMapper.deleteBatchIds(any())).thenAnswer(inv -> {
            java.util.Collection<?> ids = inv.getArgument(0);
            signs.removeIf(s -> ids.contains(s.getId()));
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferSignMapper.delete(any())).thenAnswer(inv -> {
            signs.clear();
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferRosterMapper.insert(any())).thenAnswer(inv -> {
            TVehXferRoster r = inv.getArgument(0);
            r.setId((long) (rosters.size() + 1));
            rosters.add(r);
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehXferRosterMapper.selectList(any()))
                .thenAnswer(inv -> new ArrayList<TVehXferRoster>(rosters));
    }

    @AfterEach
    public void clearPage() {
        PageHelper.clearPage();
    }

    // ------------------------------------------------ 杂件

    private TVehXferBill open() {
        VehXferOpenForm f = new VehXferOpenForm();
        f.setRemark("督办两回仍没落地，递执法");
        return service.openBill(f);
    }

    private VehXferSignReceipt sign(String signer, String decision) {
        return signWithClaim(signer, decision, null);
    }

    private VehXferSignReceipt signWithClaim(String signer, String decision, Integer claimNode) {
        VehXferSignForm f = new VehXferSignForm();
        f.setBillNo(store.getBillNo());
        f.setClaimNode(claimNode);
        f.setSignerNo(signer);
        f.setSignerName("姓名" + signer);
        f.setDecision(decision);
        f.setComment("话:" + signer + decision);
        return service.sign(f, null);
    }

    /** 一路可移交把单推到第三段门口 */
    private void driveToFinal() {
        sign("初审甲", "MOVE");
        sign("复核甲", "MOVE");
        sign("复核乙", "MOVE");
        assertEquals(2, store.getNodeNo().intValue());
    }

    private List<TVehXferSign> signsAt(int node, int round) {
        List<TVehXferSign> out = new ArrayList<TVehXferSign>();
        for (TVehXferSign s : signs) {
            if (s.getNodeNo() == node && s.getRoundNo() == round) {
                out.add(s);
            }
        }
        return out;
    }

    // ------------------------------------------------ 立案

    @Test
    public void openStartsAtFirstNodeRoundOne() {
        TVehXferBill b = open();
        assertTrue(b.getBillNo().startsWith("HQ"));
        assertEquals(0, b.getNodeNo().intValue());
        assertEquals(1, b.getRoundNo().intValue());
        assertEquals(0, b.getStatus().intValue());
        // 回显：第一段应落一名、还收笔
        assertEquals(1, b.getNeedCount().intValue());
        assertEquals(0, b.getSignedCount().intValue());
        assertTrue(b.getSignOpen());
    }

    // ------------------------------------------------ 第一段：一名即可

    @Test
    public void firstNodeSingleMoveAdvances() {
        open();
        VehXferSignReceipt rc = sign("初审甲", "MOVE");
        assertTrue(rc.getAccepted());
        assertTrue(rc.getNewLine());
        assertTrue(rc.getNodeComplete());
        assertEquals(1, rc.getResultNode().intValue());
        assertEquals(1, store.getNodeNo().intValue());
        assertEquals(1, signs.size());
    }

    @Test
    public void firstNodeHoldErasesAndRestartsInPlace() {
        open();
        VehXferSignReceipt rc = sign("初审甲", "HOLD");
        assertTrue(rc.getAccepted());
        // 头一段退无可退：本轮落笔当场抹去，原地重走新一轮
        assertEquals(0, rc.getResultNode().intValue());
        assertEquals(2, rc.getRoundNo().intValue());
        assertEquals(0, store.getNodeNo().intValue());
        assertEquals(2, store.getRoundNo().intValue());
        assertTrue(signs.isEmpty());
        // 新一轮照常落笔
        VehXferSignReceipt rc2 = sign("初审乙", "MOVE");
        assertTrue(rc2.getAccepted());
        assertEquals(1, store.getNodeNo().intValue());
    }

    // ------------------------------------------------ 第二段：两名，取先落笔那位的话

    @Test
    public void secondNodeNeedsTwoAndFirstDecides() {
        open();
        sign("初审甲", "MOVE");
        VehXferSignReceipt one = sign("复核甲", "MOVE");
        // 只落一笔不算齐，单照旧停在复核
        assertFalse(one.getNodeComplete());
        assertEquals(1, one.getSignedCount().intValue());
        assertEquals(2, one.getNeedCount().intValue());
        assertEquals(1, store.getNodeNo().intValue());
        // 后一笔写不予移交也照数入账、票次累加，但取先落笔那位的话：过
        VehXferSignReceipt two = sign("复核乙", "HOLD");
        assertTrue(two.getNodeComplete());
        assertEquals(2, store.getNodeNo().intValue());
        // 两笔都在账上，两人各写了什么屏上都看得见
        List<TVehXferSign> n1 = signsAt(1, 1);
        assertEquals(2, n1.size());
        TVehXferBill detail = service.selectTVehXferBillById(1L);
        assertEquals(2, detail.getNodeViews().get(1).getSigns().size());
        assertEquals("话:复核甲MOVE", detail.getNodeViews().get(1).getSigns().get(0).getComment());
        assertEquals("话:复核乙HOLD", detail.getNodeViews().get(1).getSigns().get(1).getComment());
    }

    @Test
    public void secondNodeFirstHoldPushesBackToFirst() {
        open();
        sign("初审甲", "MOVE");
        sign("复核甲", "HOLD");
        VehXferSignReceipt rc = sign("复核乙", "MOVE");
        // 先落笔那位说不予移交：复核本轮两笔当场抹去，单子压回初审重走
        assertEquals(0, rc.getResultNode().intValue());
        assertEquals(2, rc.getRoundNo().intValue());
        assertEquals(0, store.getNodeNo().intValue());
        assertEquals(2, store.getRoundNo().intValue());
        assertTrue(signsAt(1, 1).isEmpty());
        // 更早各段的旧笔原样存底：初审第一轮那一笔还在
        assertEquals(1, signsAt(0, 1).size());
        // 重走从被压回那一段往上办，新份另起一轮
        VehXferSignReceipt again = sign("初审丙", "MOVE");
        assertTrue(again.getAccepted());
        assertEquals(1, store.getNodeNo().intValue());
        assertEquals(1, signsAt(0, 2).size());
    }

    // ------------------------------------------------ 第三段：两名同判

    @Test
    public void finalNodeUnanimousMoveCompletesAndWritesRoster() {
        open();
        driveToFinal();
        VehXferSignReceipt one = sign("终审甲", "MOVE");
        assertFalse(one.getNodeComplete());
        assertEquals(2, store.getNodeNo().intValue());
        VehXferSignReceipt two = sign("终审乙", "MOVE");
        // 第三段点齐：移交办讫
        assertTrue(two.getNodeComplete());
        assertEquals(1, two.getStatus().intValue());
        assertEquals(1, store.getStatus().intValue());
        assertNotNull(store.getDoneAt());
        // 名册与单上结论同源一次回算：三段笔数快照一笔不差
        assertEquals(1, rosters.size());
        TVehXferRoster book = rosters.get(0);
        assertEquals("MOVE", book.getConclusion());
        assertEquals(1, book.getNode0Count().intValue());
        assertEquals(2, book.getNode1Count().intValue());
        assertEquals(2, book.getNode2Count().intValue());
        assertEquals(store.getDoneAt(), book.getDoneAt());
    }

    @Test
    public void finalNodeMixedHangsPendingThenPushBack() {
        open();
        driveToFinal();
        sign("终审甲", "MOVE");
        VehXferSignReceipt rc = sign("终审乙", "HOLD");
        // 一可一不可：挂在待定上，既不进下一节点，也不就此终止
        assertTrue(rc.getNodePending());
        assertFalse(rc.getNodeComplete());
        assertEquals(2, store.getNodeNo().intValue());
        assertEquals(0, store.getStatus().intValue());
        assertEquals(2, signsAt(2, 1).size());
        // 人齐之后本段不再收笔
        VehXferSignReceipt third = sign("终审丙", "MOVE");
        assertFalse(third.getAccepted());
        assertEquals(2, signsAt(2, 1).size());
        // 不予移交处置：终审本轮两笔当场抹去，压回复核重走
        TVehXferBill back = service.pushBack(1L, "同判没合上，压回复核");
        assertNotNull(back);
        assertEquals(1, store.getNodeNo().intValue());
        assertEquals(2, store.getRoundNo().intValue());
        assertTrue(signsAt(2, 1).isEmpty());
        // 复核旧笔原样存底
        assertEquals(2, signsAt(1, 1).size());
    }

    @Test
    public void finalNodeBothHoldPushesBackToSecond() {
        open();
        driveToFinal();
        sign("终审甲", "HOLD");
        VehXferSignReceipt rc = sign("终审乙", "HOLD");
        // 两名同判不予移交：本轮落笔当场抹去，压回复核
        assertEquals(1, rc.getResultNode().intValue());
        assertEquals(1, store.getNodeNo().intValue());
        assertEquals(2, store.getRoundNo().intValue());
        assertTrue(signsAt(2, 1).isEmpty());
        assertEquals(2, signsAt(1, 1).size());
    }

    // ------------------------------------------------ 口信只当来意 / 幂等 / 唯一键

    @Test
    public void claimNodeMismatchIsVoidAndNothingMoves() {
        open();
        // 跳过第一段径直递第二段的笔：当它没落，整张单照旧停在第一段
        VehXferSignReceipt rc = signWithClaim("复核甲", "MOVE", 1);
        assertFalse(rc.getAccepted());
        assertFalse(rc.getNewLine());
        assertEquals(0, rc.getNodeNo().intValue());
        assertEquals(1, rc.getClaimNode().intValue());
        assertFalse(rc.getClaimMatched());
        assertTrue(signs.isEmpty());
        assertEquals(0, store.getNodeNo().intValue());
        // 口信对得上才作数
        VehXferSignReceipt ok = signWithClaim("初审甲", "MOVE", 0);
        assertTrue(ok.getAccepted());
        assertTrue(ok.getClaimMatched());
        // 单到复核后，递单自报第三段：照样不作调和
        VehXferSignReceipt skip = signWithClaim("终审甲", "MOVE", 2);
        assertFalse(skip.getAccepted());
        assertEquals(1, skip.getNodeNo().intValue());
        assertEquals(1, store.getNodeNo().intValue());
        assertEquals(1, signs.size());
    }

    @Test
    public void duplicateSubmitIsIdempotentAndOldPenStands() {
        open();
        driveToFinal();
        sign("终审甲", "MOVE");
        // 同一话重复递交：幂等，不另起行
        VehXferSignReceipt dup = sign("终审甲", "MOVE");
        assertTrue(dup.getAccepted());
        assertFalse(dup.getNewLine());
        assertEquals(1, signsAt(2, 1).size());
        // 同一人在同一段改口：不接，旧笔不改
        VehXferSignReceipt flip = sign("终审甲", "HOLD");
        assertFalse(flip.getAccepted());
        assertFalse(flip.getNewLine());
        assertEquals(1, signsAt(2, 1).size());
        assertEquals("MOVE", signsAt(2, 1).get(0).getDecision());
    }

    @Test
    public void uniqueKeyBlocksConcurrentSameSigner() {
        open();
        driveToFinal();
        // 前后脚同时到：对方那笔先落库，我们这笔撞唯一键，按幂等回
        org.mockito.Mockito.doAnswer(inv -> {
            TVehXferSign row = inv.getArgument(0);
            TVehXferSign landed = new TVehXferSign();
            landed.setId(++signSeq);
            landed.setBillId(row.getBillId());
            landed.setNodeNo(row.getNodeNo());
            landed.setRoundNo(row.getRoundNo());
            landed.setSignerNo(row.getSignerNo());
            landed.setDecision(row.getDecision());
            signs.add(landed);
            throw new DuplicateKeyException("uk_veh_xfer_sign_once");
        }).when(vehXferSignMapper).insert(any());
        VehXferSignReceipt rc = sign("终审甲", "MOVE");
        assertTrue(rc.getAccepted());
        assertFalse(rc.getNewLine());
        assertEquals(1, signsAt(2, 1).size());
    }

    // ------------------------------------------------ 封卷只读 / 驳回

    @Test
    public void doneBillIsSealed() {
        open();
        driveToFinal();
        sign("终审甲", "MOVE");
        sign("终审乙", "MOVE");
        assertEquals(1, store.getStatus().intValue());
        // 办讫之后：加不进一笔、改不了一个字、整张也抽不掉
        assertFalse(sign("终审丙", "MOVE").getAccepted());
        assertNull(service.pushBack(1L, "压不动"));
        assertNull(service.reject(1L, "驳不动"));
        assertFalse(service.updateContent(1L, "改不动"));
        assertFalse(service.remove(1L));
        assertEquals(5, signs.size());
    }

    @Test
    public void rejectStopsTheWholeBill() {
        open();
        sign("初审甲", "MOVE");
        TVehXferBill b = service.reject(1L, "线索不实，驳回");
        assertNotNull(b);
        assertEquals(2, store.getStatus().intValue());
        // 全单就此止住，后面各段不再收笔
        assertFalse(sign("复核甲", "MOVE").getAccepted());
        assertNull(service.pushBack(1L, "压不动"));
        assertFalse(service.updateContent(1L, "改不动"));
        assertFalse(service.remove(1L));
    }

    @Test
    public void missingBillAndBlankFieldsAreRefused() {
        VehXferSignForm f = new VehXferSignForm();
        f.setBillNo("HQ查无此单");
        f.setSignerNo("甲");
        f.setDecision("MOVE");
        assertFalse(service.sign(f, null).getAccepted());
        open();
        VehXferSignForm noSigner = new VehXferSignForm();
        noSigner.setBillNo(store.getBillNo());
        noSigner.setDecision("MOVE");
        assertFalse(service.sign(noSigner, null).getAccepted());
        VehXferSignForm badWord = new VehXferSignForm();
        badWord.setBillNo(store.getBillNo());
        badWord.setSignerNo("甲");
        badWord.setDecision("再看看");
        assertFalse(service.sign(badWord, null).getAccepted());
        assertTrue(signs.isEmpty());
    }

    // ------------------------------------------------ 台账同回点算 / 事后翻查

    @Test
    public void boardCountsComeFromTheSamePass() {
        open();
        TVehXferBill row = new TVehXferBill();
        row.setId(1L);
        row.setNodeNo(0);
        row.setRoundNo(1);
        row.setStatus(0);
        row.setCurSignCount(0);
        row.setBoardTotal(1);
        row.setBoardActiveCount(1);
        row.setBoardNode0Active(1);
        row.setBoardNode1Active(0);
        row.setBoardNode2Active(0);
        List<TVehXferBill> rows = new ArrayList<TVehXferBill>();
        rows.add(row);
        when(boardMapper.selectBoardPage(any())).thenReturn(rows);
        VehXferBoard board = service.openBoard(new VehXferQuery());
        // 屏上显的数与逐节点点出的数同一回算成：立案加一
        assertEquals(1, board.getActiveCount());
        assertEquals(1, board.getNode0Active() + board.getNode1Active() + board.getNode2Active());
        assertEquals(1, board.getBills().get(0).getNeedCount().intValue());
        assertTrue(board.getBills().get(0).getSignOpen());
    }

    @Test
    public void auditRecountsNodeByNodeAndCatchesMismatch() {
        open();
        // 走出一条带压回的路：复核先不可→压回初审→重走→办讫
        sign("初审甲", "MOVE");
        sign("复核甲", "HOLD");
        sign("复核乙", "MOVE");
        assertEquals(0, store.getNodeNo().intValue());
        sign("初审丙", "MOVE");
        sign("复核丙", "MOVE");
        sign("复核丁", "MOVE");
        sign("终审甲", "MOVE");
        sign("终审乙", "MOVE");
        assertEquals(1, store.getStatus().intValue());
        // 名册快照：初审有效轮一笔（新份），复核两笔，终审两笔
        assertEquals(1, rosters.size());
        assertEquals(1, rosters.get(0).getNode0Count().intValue());
        assertEquals(2, rosters.get(0).getNode1Count().intValue());
        assertEquals(2, rosters.get(0).getNode2Count().intValue());
        // 事后翻查：从第三段数回第一段，与名册一笔不差
        VehXferAuditBoard ok = service.audit(null);
        assertEquals(1, ok.getCheckedCount().intValue());
        assertEquals(1, ok.getMatchedCount().intValue());
        assertEquals(0, ok.getMismatchedCount().intValue());
        // 名册上哪格被人动了，指名道姓对不上
        rosters.get(0).setNode1Count(1);
        VehXferAuditBoard bad = service.audit(null);
        assertEquals(1, bad.getMismatchedCount().intValue());
        assertFalse(bad.getRows().get(0).getMatched());
        assertEquals(2, bad.getRows().get(0).getRecountNode1().intValue());
    }

    // ------------------------------------------------ 在核才许改/撤

    @Test
    public void runningBillCanUpdateAndRemove() {
        open();
        assertTrue(service.updateContent(1L, "补一句"));
        assertTrue(service.remove(1L));
        assertNull(store);
        assertTrue(signs.isEmpty());
    }

    @Test
    public void pushBackAtFirstNodeStaysPut() {
        open();
        TVehXferBill b = service.pushBack(1L, "头一段压回");
        assertNotNull(b);
        assertEquals(0, store.getNodeNo().intValue());
        assertEquals(2, store.getRoundNo().intValue());
    }
}
