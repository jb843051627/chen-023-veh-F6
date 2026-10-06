package com.fc.v2.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fc.v2.mapper.auto.TVehXferBillMapper;
import com.fc.v2.mapper.auto.TVehXferRosterMapper;
import com.fc.v2.mapper.auto.TVehXferSignMapper;
import com.fc.v2.mapper.custom.TVehXferBillBoardMapper;
import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.auto.TVehXferRoster;
import com.fc.v2.model.auto.TVehXferSign;
import com.fc.v2.model.custom.VehXferAudit;
import com.fc.v2.model.custom.VehXferAuditBoard;
import com.fc.v2.model.custom.VehXferBoard;
import com.fc.v2.model.custom.VehXferNodeView;
import com.fc.v2.model.custom.VehXferOpenForm;
import com.fc.v2.model.custom.VehXferQuery;
import com.fc.v2.model.custom.VehXferSignForm;
import com.fc.v2.model.custom.VehXferSignReceipt;
import com.fc.v2.service.ITVehXferBillService;
import com.fc.v2.service.VehXferRuler;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 移交执法核签单（系统盯办一套）Service业务层处理。
 *
 * 今年这张单交系统从头盯到尾，盯的就是三件事：哪一关由几个人说话、压回来清哪一段、
 * 点齐之后还动得动不动。立住的三条：
 * 1. 四问只认一处——齐不齐、下段开不开、压回清哪段、点齐后收不收，全出 {@link VehXferRuler}
 *    与本类，控制器只递话，别处不许另答，也不许在旁处直接把段次按下去、把先后重排；
 * 2. 段次只有一个来处——系统数出来的当段（node_no 一栏）。递单带进来的节点号只当口信，
 *    对不上不作调和，那一笔当它没落，只认这一处回去的号；
 * 3. 已落几人不留手填格——系统顺着落笔留痕一段一段点出来，屏上那个数只算回显。
 *
 * 旧核签单一套（TVehMoveBillServiceImpl）模样不改，本套是另起的新名目，各走各的。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Service
public class TVehXferBillServiceImpl implements ITVehXferBillService {

    private static final String FMT_DAY = "yyyy-MM-dd";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @javax.annotation.Resource
    private TVehXferBillMapper vehXferBillMapper;
    @javax.annotation.Resource
    private TVehXferSignMapper vehXferSignMapper;
    @javax.annotation.Resource
    private TVehXferRosterMapper vehXferRosterMapper;
    @javax.annotation.Resource
    private TVehXferBillBoardMapper boardMapper;

    // ---------------------------------------------------------------- 回读

    @Override
    public TVehXferBill selectTVehXferBillById(Long id) {
        TVehXferBill r = vehXferBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        attachRead(r);
        return r;
    }

    @Override
    public VehXferBoard openBoard(VehXferQuery query) {
        VehXferQuery q = query == null ? new VehXferQuery() : query;
        int pageNum = q.getPage() != null && q.getPage() > 0 ? q.getPage() : 1;
        int pageSize = q.getLimit() != null && q.getLimit() > 0 ? q.getLimit() : DEFAULT_PAGE_SIZE;

        // 分页切同一条 SQL 的同一批结果：在核张数、逐节点张数、当段已落几人都是这条
        // SELECT 同一快照里点出来的，与一张一张点出来的单同一次算成，不许别处另点一遍。
        PageHelper.startPage(pageNum, pageSize, false);
        List<TVehXferBill> rows = boardMapper.selectBoardPage(q);
        PageInfo<TVehXferBill> page = new PageInfo<TVehXferBill>(rows);

        VehXferBoard board = new VehXferBoard();
        board.setBills(page.getList());
        if (!page.getList().isEmpty()) {
            TVehXferBill first = page.getList().get(0);
            // 屏头几个数全取自这一条 SELECT 的同一快照，不由服务层另点
            board.setTotalCount(first.getBoardTotal() == null ? page.getTotal() : first.getBoardTotal());
            board.setActiveCount(first.getBoardActiveCount() == null ? 0 : first.getBoardActiveCount());
            board.setNode0Active(first.getBoardNode0Active() == null ? 0 : first.getBoardNode0Active());
            board.setNode1Active(first.getBoardNode1Active() == null ? 0 : first.getBoardNode1Active());
            board.setNode2Active(first.getBoardNode2Active() == null ? 0 : first.getBoardNode2Active());
            for (TVehXferBill r : page.getList()) {
                attachBoardEcho(r);
            }
        }
        return board;
    }

    /** 台账行回显：当段应落几人、已落几人（SQL 同回点出的那个数）、还收不收笔，全照唯一一把尺 */
    private void attachBoardEcho(TVehXferBill r) {
        int node = VehXferRuler.nodeOf(r);
        int signed = r.getCurSignCount() == null ? 0 : r.getCurSignCount();
        r.setNeedCount(VehXferRuler.needCount(node));
        r.setSignedCount(signed);
        r.setModeText(VehXferRuler.modeText(node));
        r.setNodeName(VehXferRuler.nodeName(node));
        String block = VehXferRuler.signBlockReason(r, signed);
        r.setSignOpen(block == null);
        r.setSignBlockReason(block);
        r.setNodePending(block != null && !VehXferRuler.isTerminal(r));
    }

    /** 单卷回读挂齐：三段逐段回显 + 全卷落笔留痕，已落几人系统现点，屏上只算回显 */
    private void attachRead(TVehXferBill r) {
        int node = VehXferRuler.nodeOf(r);
        int round = VehXferRuler.roundOf(r);
        List<TVehXferSign> all = loadSigns(r.getId());
        List<TVehXferSign> cur = VehXferRuler.roundSigns(all, node, round);

        r.setNeedCount(VehXferRuler.needCount(node));
        r.setModeText(VehXferRuler.modeText(node));
        r.setNodeName(VehXferRuler.nodeName(node));
        r.setSignedCount(cur.size());
        String block = VehXferRuler.signBlockReason(r, cur.size());
        r.setSignOpen(block == null);
        r.setSignBlockReason(block);
        r.setNextOpen(VehXferRuler.nextOpen(r, all));
        r.setNodePending(VehXferRuler.nodePending(node, cur));
        r.setSigns(all);

        List<VehXferNodeView> views = new ArrayList<VehXferNodeView>();
        for (int k = VehXferRuler.NODE_FIRST; k <= VehXferRuler.MAX_NODE; k++) {
            // 当下所停段看本轮的笔；已走过的段看有效那一轮（最后一轮）的笔，旧轮存底不动
            List<TVehXferSign> viewSigns = k == node
                    ? VehXferRuler.roundSigns(all, k, round) : VehXferRuler.effectiveSigns(all, k);
            VehXferNodeView v = new VehXferNodeView();
            v.setNodeNo(k);
            v.setNodeName(VehXferRuler.nodeName(k));
            v.setNeedCount(VehXferRuler.needCount(k));
            v.setModeText(VehXferRuler.modeText(k));
            v.setCurrent(k == node);
            v.setSigns(viewSigns);
            v.setSignedCount(viewSigns.size());
            v.setFull(VehXferRuler.nodeFull(k, viewSigns));
            v.setPassed(VehXferRuler.nodePassed(k, viewSigns));
            v.setPending(VehXferRuler.nodePending(k, viewSigns));
            views.add(v);
        }
        r.setNodeViews(views);
    }

    // ---------------------------------------------------------------- 立案

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehXferBill openBill(VehXferOpenForm form) {
        TVehXferBill r = new TVehXferBill();
        // 单代号系统排号，单上自带号递来不认
        r.setBillNo("HQ" + SnowflakeIdWorker.getUUID());
        r.setNodeNo(VehXferRuler.NODE_FIRST);
        r.setRoundNo(1);
        // 立案即在核，立案一张在核数加一
        r.setStatus(VehXferRuler.STATUS_RUNNING);
        r.setRemark(form == null ? null : trimToNull(form.getRemark()));
        r.setDelFlag(0);
        vehXferBillMapper.insert(r);
        return attachAndReturn(r);
    }

    // ---------------------------------------------------------------- 递单落笔（唯一入口）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehXferSignReceipt sign(VehXferSignForm form, Date at) {
        VehXferSignReceipt rc = new VehXferSignReceipt();
        Date moment = at == null ? new Date() : at;
        String billNo = form == null ? null : trimToNull(form.getBillNo());
        if (billNo == null) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setMark("报文里没带核签单代号，接不住");
            return rc;
        }
        rc.setBillNo(billNo);
        TVehXferBill bill = findByBillNo(billNo);
        if (bill == null) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setMark("查无这张核签单，报文原样退回");
            return rc;
        }
        int node = VehXferRuler.nodeOf(bill);
        int round = VehXferRuler.roundOf(bill);
        int need = VehXferRuler.needCount(node);
        rc.setNodeNo(node);
        rc.setRoundNo(round);
        rc.setNeedCount(need);
        rc.setClaimNode(form.getClaimNode());
        rc.setClaimMatched(form.getClaimNode() == null ? null : form.getClaimNode() == node);
        rc.setResultNode(node);
        rc.setStatus(bill.getStatus());

        // 点齐之后还动得动不动：封了卷的单，一笔也加不进
        if (VehXferRuler.isTerminal(bill)) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setSignedCount(VehXferRuler.signedCount(loadSigns(bill.getId()), node, round));
            rc.setMark(bill.getStatus() != null && bill.getStatus() == VehXferRuler.STATUS_DONE
                    ? "这张单已移交办讫，封卷只读，一笔也加不进" : "这张单已驳回止住，一笔也加不进");
            return rc;
        }
        // 口信节点号只当来意：与系统数出来的当段对不上，那一笔当它没落，卷上一格不动
        if (form.getClaimNode() != null && form.getClaimNode() != node) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setSignedCount(VehXferRuler.signedCount(loadSigns(bill.getId()), node, round));
            rc.setMark("递单口信说第" + form.getClaimNode() + "段，系统数在"
                    + VehXferRuler.nodeName(node) + "——那一笔当它没落，只认系统这一段，不作调和");
            return rc;
        }
        String signerNo = trimToNull(form.getSignerNo());
        if (signerNo == null) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setMark("落笔人代号没落，这一笔记不全");
            return rc;
        }
        String decision = trimToNull(form.getDecision());
        if (!VehXferRuler.DECISION_MOVE.equals(decision) && !VehXferRuler.DECISION_HOLD.equals(decision)) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setMark("这一笔的话只认 可移交(MOVE)/不予移交(HOLD)，写别的接不住");
            return rc;
        }

        List<TVehXferSign> all = loadSigns(bill.getId());
        List<TVehXferSign> cur = VehXferRuler.roundSigns(all, node, round);
        rc.setSignedCount(cur.size());

        // 同一人在本段本轮已落过：重复递交按幂等，账上还是头一笔，不另起新行
        TVehXferSign existing = findSigner(cur, signerNo);
        if (existing != null) {
            rc.setNewLine(false);
            if (decision.equals(existing.getDecision())) {
                rc.setAccepted(true);
                rc.setMark("这一笔已在账上，重复递交不另起行");
            } else {
                rc.setAccepted(false);
                rc.setMark("同一人在同一段本轮只有一笔，旧笔不改");
            }
            return rc;
        }
        // 本段人已满（只第三段挂待定会停在这情形）：不再收笔
        if (VehXferRuler.nodeFull(node, cur)) {
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setMark("本段应落" + need + "人已齐，挂在待定上等处置，不再收笔");
            return rc;
        }

        TVehXferSign row = new TVehXferSign();
        row.setBillId(bill.getId());
        row.setNodeNo(node);
        row.setRoundNo(round);
        row.setSignerNo(signerNo);
        row.setSignerName(trimToNull(form.getSignerName()));
        row.setDecision(decision);
        row.setComment(trimToNull(form.getComment()));
        row.setClaimNode(form.getClaimNode());
        row.setSignedAt(moment);
        row.setDelFlag(0);
        try {
            vehXferSignMapper.insert(row);
        } catch (DuplicateKeyException dup) {
            // 前后脚同时到，唯一键挡住同一人同段本轮的第二笔：按幂等回，不另起行
            List<TVehXferSign> again = VehXferRuler.roundSigns(loadSigns(bill.getId()), node, round);
            rc.setSignedCount(again.size());
            rc.setNewLine(false);
            TVehXferSign landed = findSigner(again, signerNo);
            if (landed != null && decision.equals(landed.getDecision())) {
                rc.setAccepted(true);
                rc.setMark("前后脚同时到，唯一键挡住重笔：账上那头一笔算数，不另起行");
            } else {
                rc.setAccepted(false);
                rc.setMark("前后脚同时到，唯一键挡住重笔：同一人在同一段本轮只有一笔，旧笔不改");
            }
            return rc;
        }

        // 落完这一笔，系统顺着落笔留痕把本段本轮重新点一遍（屏上那个数只算回显）
        List<TVehXferSign> allAfter = loadSigns(bill.getId());
        List<TVehXferSign> after = VehXferRuler.roundSigns(allAfter, node, round);
        rc.setAccepted(true);
        rc.setNewLine(true);
        rc.setSignedCount(after.size());

        if (!VehXferRuler.nodeFull(node, after)) {
            rc.setNodeComplete(false);
            rc.setMark("本段还差" + (need - after.size()) + "人，单照旧停在" + VehXferRuler.nodeName(node));
            return rc;
        }
        if (VehXferRuler.nodePassed(node, after)) {
            rc.setNodeComplete(true);
            if (node == VehXferRuler.MAX_NODE) {
                // 第三段点齐：移交办讫。单上结论与移交名册同一回算出来、同一事务落库
                bill.setStatus(VehXferRuler.STATUS_DONE);
                bill.setDoneAt(moment);
                vehXferBillMapper.updateById(bill);
                writeRoster(bill, allAfter, moment);
                rc.setStatus(VehXferRuler.STATUS_DONE);
                rc.setResultNode(node);
                rc.setMark("第三段两名同判可移交，这张单移交办讫；此后加不进一笔、改不了一个字、整张也抽不掉");
            } else {
                // 走只许进紧邻的下一节点
                bill.setNodeNo(node + 1);
                vehXferBillMapper.updateById(bill);
                rc.setResultNode(node + 1);
                rc.setMark("本段点齐，进" + VehXferRuler.nodeName(node + 1));
            }
            return rc;
        }
        if (node != VehXferRuler.MAX_NODE || VehXferRuler.nodeAllHold(node, after)) {
            // 不予移交（收只收一格）：本段本轮已落的笔当场抹去、单子压回一格，
            // 抹笔与退档落在同一事务里；更早各段的旧笔原样存底，重走新份另起一轮
            eraseRound(after);
            bill.setRoundNo(round + 1);
            bill.setNodeNo(VehXferRuler.pushBackTarget(node));
            vehXferBillMapper.updateById(bill);
            rc.setNodeComplete(false);
            rc.setResultNode(bill.getNodeNo());
            rc.setRoundNo(round + 1);
            rc.setSignedCount(0);
            if (node == VehXferRuler.MAX_NODE) {
                rc.setMark("两名同判不予移交：本轮落笔当场抹去，单子压回" + VehXferRuler.nodeName(bill.getNodeNo()) + "重走");
            } else if (node == VehXferRuler.NODE_FIRST) {
                rc.setMark("这一笔说不予移交：本轮落笔当场抹去，头一段退无可退，原地重走");
            } else {
                rc.setMark("先落笔那位说不予移交：本轮落笔当场抹去，单子压回" + VehXferRuler.nodeName(bill.getNodeNo()) + "重走");
            }
            return rc;
        }
        // 第三段一可一不可：挂在待定上，既不进下一节点，也不就此终止
        rc.setNodeComplete(false);
        rc.setNodePending(true);
        rc.setMark("两笔一可一不可，本段挂在待定上：不进下一段，也不就此终止，等不予移交或驳回处置");
        return rc;
    }

    // ---------------------------------------------------------------- 不予移交 / 驳回

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehXferBill pushBack(Long id, String comment) {
        TVehXferBill r = vehXferBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 封了卷的压不动
        if (VehXferRuler.isTerminal(r)) {
            return null;
        }
        int node = VehXferRuler.nodeOf(r);
        int round = VehXferRuler.roundOf(r);
        // 本段这一轮已落的笔当场抹去、不许留着；抹笔与退档同一事务，要么一起成要么一起不成
        eraseRound(VehXferRuler.roundSigns(loadSigns(id), node, round));
        r.setRoundNo(round + 1);
        r.setNodeNo(VehXferRuler.pushBackTarget(node));
        if (comment != null) {
            r.setRemark(trimToNull(comment));
        }
        vehXferBillMapper.updateById(r);
        return attachAndReturn(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehXferBill reject(Long id, String comment) {
        TVehXferBill r = vehXferBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (VehXferRuler.isTerminal(r)) {
            return null;
        }
        // 驳回即终态：全单就此止住，后面各段不再收笔
        r.setStatus(VehXferRuler.STATUS_VETO);
        if (comment != null) {
            r.setRemark(trimToNull(comment));
        }
        vehXferBillMapper.updateById(r);
        return attachAndReturn(r);
    }

    // ---------------------------------------------------------------- 改 / 撤

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String remark) {
        TVehXferBill r = vehXferBillMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 办讫之后改不了一个字：修改的位置一律锁死
        if (VehXferRuler.isTerminal(r)) {
            return false;
        }
        r.setRemark(remark);
        return vehXferBillMapper.updateById(r) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long id) {
        TVehXferBill r = vehXferBillMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 办讫/驳回的单整张抽不掉；只有在核的才撤得掉
        if (VehXferRuler.isTerminal(r)) {
            return false;
        }
        vehXferSignMapper.delete(new QueryWrapper<TVehXferSign>().eq("bill_id", id));
        return vehXferBillMapper.deleteById(id) > 0;
    }

    // ---------------------------------------------------------------- 名册与事后翻查

    @Override
    public List<TVehXferRoster> listRoster(String billNo, String day) {
        QueryWrapper<TVehXferRoster> w = new QueryWrapper<TVehXferRoster>().eq("del_flag", 0);
        String no = trimToNull(billNo);
        if (no != null) {
            w.likeRight("bill_no", no);
        }
        dayRange(w, "done_at", day);
        w.orderByDesc("done_at").orderByDesc("id");
        return vehXferRosterMapper.selectList(w);
    }

    @Override
    public VehXferAuditBoard audit(String day) {
        QueryWrapper<TVehXferRoster> w = new QueryWrapper<TVehXferRoster>().eq("del_flag", 0);
        dayRange(w, "done_at", day);
        w.orderByDesc("done_at").orderByDesc("id");
        List<TVehXferRoster> roster = vehXferRosterMapper.selectList(w);

        List<VehXferAudit> rows = new ArrayList<VehXferAudit>();
        int matched = 0;
        for (TVehXferRoster book : roster) {
            List<TVehXferSign> all = loadSigns(book.getBillId());
            // 从第三段一节点一节点数回第一段，与当日那本名册一笔一笔对
            List<TVehXferSign> n2 = VehXferRuler.effectiveSigns(all, VehXferRuler.NODE_FINAL);
            List<TVehXferSign> n1 = VehXferRuler.effectiveSigns(all, VehXferRuler.NODE_SECOND);
            List<TVehXferSign> n0 = VehXferRuler.effectiveSigns(all, VehXferRuler.NODE_FIRST);
            VehXferAudit a = new VehXferAudit();
            a.setBillNo(book.getBillNo());
            a.setDoneAt(book.getDoneAt());
            a.setRosterConclusion(book.getConclusion());
            a.setRecountConclusion(VehXferRuler.nodePassed(VehXferRuler.NODE_FINAL, n2)
                    ? VehXferRuler.DECISION_MOVE : null);
            a.setRosterNode0(book.getNode0Count());
            a.setRosterNode1(book.getNode1Count());
            a.setRosterNode2(book.getNode2Count());
            a.setRecountNode0(n0.size());
            a.setRecountNode1(n1.size());
            a.setRecountNode2(n2.size());
            boolean ok = Objects.equals(book.getConclusion(), a.getRecountConclusion())
                    && Objects.equals(book.getNode0Count(), a.getRecountNode0())
                    && Objects.equals(book.getNode1Count(), a.getRecountNode1())
                    && Objects.equals(book.getNode2Count(), a.getRecountNode2());
            a.setMatched(ok);
            if (ok) {
                matched++;
            }
            rows.add(a);
        }
        VehXferAuditBoard board = new VehXferAuditBoard();
        board.setRows(rows);
        board.setCheckedCount(rows.size());
        board.setMatchedCount(matched);
        board.setMismatchedCount(rows.size() - matched);
        return board;
    }

    // ---------------------------------------------------------------- 杂件

    /** 落笔留痕全卷取出：按落笔先后（自增主键）升序，先后只认这把尺 */
    private List<TVehXferSign> loadSigns(Long billId) {
        return vehXferSignMapper.selectList(new QueryWrapper<TVehXferSign>()
                .eq("bill_id", billId)
                .eq("del_flag", 0)
                .orderByAsc("id"));
    }

    /** 本段本轮已落的笔当场抹去（硬删，不许留着）；与退档同在一个事务里 */
    private void eraseRound(List<TVehXferSign> roundSigns) {
        if (roundSigns == null || roundSigns.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<Long>();
        for (TVehXferSign s : roundSigns) {
            ids.add(s.getId());
        }
        vehXferSignMapper.deleteBatchIds(ids);
    }

    /**
     * 办讫写名册：终审结论与三段笔数快照，与单上结论同源一次回算（同一份落笔留痕），
     * 一处取数、不许手填；与单子的办讫更新落在同一事务里。
     */
    private void writeRoster(TVehXferBill bill, List<TVehXferSign> allAfter, Date moment) {
        TVehXferRoster r = new TVehXferRoster();
        r.setBillId(bill.getId());
        r.setBillNo(bill.getBillNo());
        r.setConclusion(VehXferRuler.DECISION_MOVE);
        r.setNode0Count(VehXferRuler.effectiveSigns(allAfter, VehXferRuler.NODE_FIRST).size());
        r.setNode1Count(VehXferRuler.effectiveSigns(allAfter, VehXferRuler.NODE_SECOND).size());
        r.setNode2Count(VehXferRuler.effectiveSigns(allAfter, VehXferRuler.NODE_FINAL).size());
        r.setRoundNo(VehXferRuler.roundOf(bill));
        r.setDoneAt(moment);
        r.setDelFlag(0);
        vehXferRosterMapper.insert(r);
    }

    private TVehXferBill findByBillNo(String billNo) {
        return vehXferBillMapper.selectOne(new QueryWrapper<TVehXferBill>()
                .eq("bill_no", billNo).eq("del_flag", 0).last("limit 1"));
    }

    private TVehXferBill attachAndReturn(TVehXferBill r) {
        TVehXferBill fresh = vehXferBillMapper.selectById(r.getId());
        if (fresh != null) {
            attachRead(fresh);
            return fresh;
        }
        return r;
    }

    private static TVehXferSign findSigner(List<TVehXferSign> roundSigns, String signerNo) {
        for (TVehXferSign s : roundSigns) {
            if (signerNo.equals(s.getSignerNo())) {
                return s;
            }
        }
        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 名册按日筛：墙钟日 yyyy-MM-dd，写法不对就不筛（时刻只认墙钟字符串那一套） */
    private static void dayRange(QueryWrapper<TVehXferRoster> w, String column, String day) {
        String d = trimToNull(day);
        if (d == null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_DAY);
        sdf.setLenient(false);
        try {
            sdf.parse(d);
        } catch (ParseException e) {
            return;
        }
        w.ge(column, d + " 00:00:00").le(column, d + " 23:59:59");
    }
}
