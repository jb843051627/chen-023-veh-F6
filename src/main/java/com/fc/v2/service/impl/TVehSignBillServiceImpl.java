package com.fc.v2.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fc.v2.common.exception.VehSignRejectException;
import com.fc.v2.mapper.auto.TVehSignBillMapper;
import com.fc.v2.mapper.auto.TVehSignInkMapper;
import com.fc.v2.mapper.auto.TVehTransferRegisterMapper;
import com.fc.v2.mapper.custom.TVehSignBillBoardMapper;
import com.fc.v2.model.auto.TVehSignBill;
import com.fc.v2.model.auto.TVehSignInk;
import com.fc.v2.model.auto.TVehTransferRegister;
import com.fc.v2.model.custom.TVehSignNodeView;
import com.fc.v2.model.custom.VehSignBoard;
import com.fc.v2.model.custom.VehSignDismissForm;
import com.fc.v2.model.custom.VehSignForm;
import com.fc.v2.model.custom.VehSignOpenForm;
import com.fc.v2.model.custom.VehSignQuery;
import com.fc.v2.model.custom.VehSignReceipt;
import com.fc.v2.service.ITVehSignBillService;
import com.fc.v2.service.VehSignRuler;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 核签单（三节点新走法）Service——三件事从头盯到尾：哪一关该几个人说话、压回来清哪一段、
 * 点齐之后还动得动不动。四问（这一节点算不算齐、下一节点开不开、压回来清哪一段、点齐之后收不收笔）
 * 只认 {@link VehSignRuler} 与本类给出的答案，别处问不出第二套。
 *
 * 旧 {@link TVehMoveBillServiceImpl} approve/reject/rollback 一字未动，这是另起接着的一套，
 * 不回头改它的模样。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Service
public class TVehSignBillServiceImpl implements ITVehSignBillService {

    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @javax.annotation.Resource
    private TVehSignBillMapper vehSignBillMapper;
    @javax.annotation.Resource
    private TVehSignInkMapper vehSignInkMapper;
    @javax.annotation.Resource
    private TVehTransferRegisterMapper transferRegisterMapper;
    @javax.annotation.Resource
    private TVehSignBillBoardMapper boardMapper;

    // ---------------------------------------------------------------- 立案

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehSignBill openBill(VehSignOpenForm form) {
        if (form == null) {
            throw new VehSignRejectException("form", "空单递进来，立不了案");
        }
        String clueNo = trimToNull(form.getClueNo());
        if (clueNo == null) {
            throw new VehSignRejectException("clueNo", "督办线索代号没落，这张核签单立不起来");
        }
        Date now = new Date();
        TVehSignBill b = new TVehSignBill();
        // 单号系统排（HS+雪花），单上自带号不认
        b.setBillNo("HS" + SnowflakeIdWorker.getUUID());
        b.setClueNo(clueNo);
        // 立案即停在初审、第一轮、在签（立案加一，计数由回库点，不在卷上记数）
        b.setStage(VehSignRuler.NODE_FIRST);
        b.setRoundNo(1);
        b.setStatus(VehSignRuler.STATUS_SIGNING);
        b.setDelFlag(0);
        b.setRemark(trimToNull(form.getRemark()));
        vehSignBillMapper.insert(b);
        return attachRead(b);
    }

    // ---------------------------------------------------------------- 落笔

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehSignReceipt sign(VehSignForm form) {
        if (form == null || trimToNull(form.getBillNo()) == null) {
            throw new VehSignRejectException("billNo", "没落核签单号，这一笔接不住");
        }
        String signer = trimToNull(form.getSignerNo());
        if (signer == null) {
            throw new VehSignRejectException("signerNo", "没落落笔人，这一笔算不到谁头上");
        }
        String verdict = trimToNull(form.getVerdict());
        if (!VehSignRuler.validVerdict(verdict)) {
            throw new VehSignRejectException("verdict", "这一笔只许可移交(MOVE)/不予移交(REJECT)两样，落的是：" + verdict);
        }

        VehSignReceipt rc = new VehSignReceipt();
        TVehSignBill b = loadByNo(form.getBillNo().trim());
        if (b == null) {
            rc.setAccepted(false);
            rc.setNewInk(false);
            rc.setBillNo(form.getBillNo().trim());
            rc.setMark("查无这张核签单，笔原样退回");
            return rc;
        }
        rc.setBillNo(b.getBillNo());
        int stage = VehSignRuler.stageOf(b);
        int round = b.getRoundNo() == null ? 1 : b.getRoundNo();
        rc.setStage(stage);
        rc.setClaimNode(form.getClaimNode());
        rc.setClaimMatched(form.getClaimNode() != null && form.getClaimNode() == stage);

        if (VehSignRuler.isSealed(b)) {
            // 点齐办讫/驳回的单：此后加不进一笔、改不了一个字、整张也抽不掉
            return rejectWithoutChange(rc, b, "这张单已" + (b.getStatus() == VehSignRuler.STATUS_DONE ? "办讫封档" : "驳回止住")
                    + "，一笔也加不进了");
        }
        if (VehSignRuler.isPending(b)) {
            // 终审一可一不可挂待定：冻在终审，不进下一段也不终止，这期间不收笔
            return rejectWithoutChange(rc, b, "终审两名一可一不可，单挂待定冻着，再加笔不作数");
        }

        // 递单捎来的节点号只当口信：与系统当段对不上不作调和，这一笔当没落，整单照旧停在系统那一段
        if (form.getClaimNode() != null && form.getClaimNode() != stage) {
            return rejectWithoutChange(rc, b, "口信说落在第" + form.getClaimNode()
                    + "段，系统数在" + VehSignRuler.nodeName(stage) + "——只认系统那一段，跳过/错位这一笔当没落");
        }

        String requestId = trimToNull(form.getRequestId());
        // 重复递交按幂等：同一次递交带进来的头一笔已在卷，第二遍只回头一回的话，不再落行
        if (requestId != null) {
            TVehSignInk prior = findInkByRequest(b.getId(), requestId);
            if (prior != null) {
                return echoPrior(rc, b, prior, "同一次递交重递，按头一回回，不另落一笔");
            }
        }
        // 唯一键挡同一个人在同一节点同一轮重复落笔（服务先挡一道，库里唯一键再挡并发那一道）
        TVehSignInk own = findLiveInk(b.getId(), stage, round, signer);
        if (own != null) {
            return echoPrior(rc, b, own, signer + "在" + VehSignRuler.nodeName(stage) + "本轮已落过一笔，同一人不许重复落笔");
        }

        Date moment = parseWallClock(form.getAt());
        if (moment == null) {
            moment = new Date();
        }
        TVehSignInk ink = new TVehSignInk();
        ink.setBillId(b.getId());
        ink.setNodeNo(stage);
        ink.setRoundNo(round);
        ink.setSignerNo(signer);
        ink.setVerdict(verdict);
        ink.setClaimNode(form.getClaimNode());
        ink.setRequestId(requestId);
        ink.setInkState(VehSignRuler.INK_STATE_LIVE);
        ink.setInkAt(moment);
        ink.setDelFlag(0);
        try {
            vehSignInkMapper.insert(ink);
        } catch (DuplicateKeyException dup) {
            // 并发前后脚同时到：唯一键拦下后到那一笔，回库认头一笔，绝不放同人两行
            TVehSignBill reload = vehSignBillMapper.selectById(b.getId());
            if (requestId != null && findInkByRequest(b.getId(), requestId) != null) {
                return echoPrior(rc, reload, findInkByRequest(b.getId(), requestId), "并发重递，认头一笔");
            }
            TVehSignInk concurrent = findLiveInk(b.getId(), stage, round, signer);
            if (concurrent != null) {
                return echoPrior(rc, reload, concurrent, "同人同段并发两笔，唯一键挡下后一笔");
            }
            throw dup;
        }
        // 卷上只挂最近一回递交号，真正幂等凭据在流水那一行
        b.setRequestId(requestId);

        // 落完再顺着落笔先后点这一段当轮在册的条数，四问只认尺子这一处
        List<TVehSignInk> live = currentRoundLive(b.getId(), stage, round);
        String outcome = VehSignRuler.evaluate(stage, live);
        rc.setOutcome(outcome);

        Date now = new Date();
        String mark;
        boolean registered = false;
        if (VehSignRuler.OUT_NOT_READY.equals(outcome)) {
            // 条数没与应落人数持平：这一节点当没齐，整单照旧停在这一段
            b.setStatus(VehSignRuler.STATUS_SIGNING);
            mark = VehSignRuler.nodeName(stage) + "这一笔已落，还差"
                    + (VehSignRuler.needCount(stage) - live.size()) + "人，段口不动";
        } else if (VehSignRuler.OUT_PASS.equals(outcome)) {
            if (VehSignRuler.isLastNode(stage)) {
                // 第三段两名同判可移交点齐：移交办讫，终审结论与名册行同一次回算（同一事务）
                b.setStatus(VehSignRuler.STATUS_DONE);
                b.setFinalDecision(VehSignRuler.VERDICT_MOVE);
                b.setClosedAt(now);
                registered = writeRegister(b, now);
                mark = "终审两名同判可移交，这张单移交办讫，名册同次回算落一行";
            } else {
                // 走只许进紧邻的下一节点：新节点从它自己的头一轮办起
                int next = stage + 1;
                b.setStage(next);
                b.setRoundNo(nextRoundNo(b.getId(), next));
                b.setStatus(VehSignRuler.STATUS_SIGNING);
                mark = VehSignRuler.nodeName(stage) + "点齐可移交，进紧邻的" + VehSignRuler.nodeName(next);
            }
        } else if (VehSignRuler.OUT_PRESS_BACK.equals(outcome)) {
            // 判这张单不予移交：只收一格——本节点本轮已落的笔当场抹去（留痕不删），
            // 抹笔与退档同一事务，要么一起成要么一起不成就靠这个 @Transactional；
            // 更早各节点的旧笔原样存底，重走从被压回那一段往上办，新份旧份分开放
            int target = stage == VehSignRuler.NODE_FIRST ? VehSignRuler.NODE_FIRST : stage - 1;
            int newRound = nextRoundNo(b.getId(), target);
            voidRound(b.getId(), stage, round);
            b.setStage(target);
            b.setRoundNo(newRound);
            b.setStatus(VehSignRuler.STATUS_SIGNING);
            mark = VehSignRuler.nodeName(stage) + "定论不予移交：本轮这" + live.size()
                    + "笔当场抹去留痕，退回" + VehSignRuler.nodeName(target) + "第" + newRound + "轮重走";
        } else {
            // 终审一可一不可：挂待定，笔不抹，不进下一段也不终止
            b.setStatus(VehSignRuler.STATUS_PENDING);
            mark = "终审两名一可一不可，挂待定：不进下一段，也不就此终止，笔原样留着";
        }
        vehSignBillMapper.updateById(b);

        rc.setAccepted(true);
        rc.setNewInk(true);
        rc.setBillStatus(b.getStatus());
        rc.setRegistered(registered);
        rc.setMark(mark);
        rc.setBill(attachRead(vehSignBillMapper.selectById(b.getId())));
        return rc;
    }

    // ---------------------------------------------------------------- 整单驳回

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehSignReceipt dismiss(VehSignDismissForm form) {
        if (form == null || trimToNull(form.getBillNo()) == null) {
            throw new VehSignRejectException("billNo", "没落核签单号，驳回接不住");
        }
        if (trimToNull(form.getSignerNo()) == null) {
            throw new VehSignRejectException("signerNo", "没落驳回人");
        }
        if (trimToNull(form.getReason()) == null) {
            throw new VehSignRejectException("reason", "驳回缘由空着，日后翻查无凭");
        }

        VehSignReceipt rc = new VehSignReceipt();
        TVehSignBill b = loadByNo(form.getBillNo().trim());
        if (b == null) {
            rc.setAccepted(false);
            rc.setNewInk(false);
            rc.setBillNo(form.getBillNo().trim());
            rc.setMark("查无这张核签单，驳回原样退回");
            return rc;
        }
        rc.setBillNo(b.getBillNo());
        int stage = VehSignRuler.stageOf(b);
        rc.setStage(stage);
        rc.setClaimNode(form.getClaimNode());
        rc.setClaimMatched(form.getClaimNode() != null && form.getClaimNode() == stage);

        // 重递同一回驳回事务：已驳回且是同一个 requestId，按头一回回，不重复收口
        String requestId = trimToNull(form.getRequestId());
        if (VehSignRuler.isSealed(b)) {
            boolean sameDismiss = requestId != null && requestId.equals(b.getRequestId())
                    && b.getStatus() == VehSignRuler.STATUS_DISMISSED;
            return rejectWithoutChange(rc, b, sameDismiss
                    ? "同回驳回事务重递，按头一回回，单已驳回止住"
                    : "这张单已" + (b.getStatus() == VehSignRuler.STATUS_DONE ? "办讫封档" : "驳回止住") + "，驳不动了");
        }
        if (VehSignRuler.isPending(b)) {
            return rejectWithoutChange(rc, b, "单挂在终审待定，按规矩冻着，不走驳回这道门");
        }
        // 驳回也只认系统当段：口信节点对不上，不调和，这一回当没发生
        if (form.getClaimNode() != null && form.getClaimNode() != stage) {
            return rejectWithoutChange(rc, b, "口信节点与系统当段对不上，驳回只认系统那一段，这一回不落");
        }

        Date moment = parseWallClock(form.getAt());
        if (moment == null) {
            moment = new Date();
        }
        // 某一节点驳回：全单就此止住，后面各节点不再收笔（终态与办讫一样封档只读）
        b.setStatus(VehSignRuler.STATUS_DISMISSED);
        b.setFinalDecision(VehSignRuler.VERDICT_REJECT);
        b.setClosedAt(moment);
        b.setRequestId(requestId);
        b.setRemark(appendDismiss(b.getRemark(), form.getSignerNo().trim(), form.getReason().trim(), moment));
        vehSignBillMapper.updateById(b);

        rc.setAccepted(true);
        rc.setNewInk(false);
        rc.setOutcome("DISMISS");
        rc.setBillStatus(b.getStatus());
        rc.setRegistered(false);
        rc.setMark(VehSignRuler.nodeName(stage) + "驳回：全单就此止住，后面各段不再收笔");
        rc.setBill(attachRead(vehSignBillMapper.selectById(b.getId())));
        return rc;
    }

    // ---------------------------------------------------------------- 回读 / 台账 / 名册

    @Override
    public TVehSignBill detail(String billNo) {
        String no = trimToNull(billNo);
        if (no == null) {
            return null;
        }
        TVehSignBill b = loadByNo(no);
        return b == null ? null : attachRead(b);
    }

    @Override
    public VehSignBoard openBoard(VehSignQuery query) {
        VehSignQuery q = query == null ? new VehSignQuery() : query;
        int pageNum = q.getPage() != null && q.getPage() > 0 ? q.getPage() : 1;
        int pageSize = q.getLimit() != null && q.getLimit() > 0 ? q.getLimit() : DEFAULT_PAGE_SIZE;

        // 名单分页与屏头在签张数切同一条 SQL 的同一批结果：在签张数是同一 SELECT 快照里的标量子查询，
        // 与一张一张点出来的单同一次算成，不随筛选借行，也不许服务层另点一遍凑屏头。
        PageHelper.startPage(pageNum, pageSize, false);
        List<TVehSignBill> rows = boardMapper.selectBoardPage(q);
        PageInfo<TVehSignBill> page = new PageInfo<TVehSignBill>(rows);

        VehSignBoard board = new VehSignBoard();
        board.setBills(page.getList());
        if (!page.getList().isEmpty()) {
            TVehSignBill first = page.getList().get(0);
            board.setTotalCount(first.getBoardTotal() == null ? page.getTotal() : first.getBoardTotal().longValue());
            board.setActiveCount(first.getBoardActiveCount() == null ? 0 : first.getBoardActiveCount());
            for (TVehSignBill r : page.getList()) {
                // 每条都逐节点点条数回挂，与屏头那一回回库点算本是同源
                attachRead(r);
            }
        } else {
            board.setTotalCount(page.getTotal());
            board.setActiveCount(0);
        }
        return board;
    }

    @Override
    public int countActive() {
        // 与台账标量子查询同一句口径：在签(0)与待定(3)都还没封口，办讫(1)/驳回(2)减出去
        Integer n = vehSignBillMapper.selectCount(new QueryWrapper<TVehSignBill>()
                .eq("del_flag", 0)
                .in("status", VehSignRuler.STATUS_SIGNING, VehSignRuler.STATUS_PENDING));
        return n == null ? 0 : n;
    }

    @Override
    public List<TVehTransferRegister> listRegister() {
        return transferRegisterMapper.selectList(new QueryWrapper<TVehTransferRegister>()
                .eq("del_flag", 0)
                .orderByDesc("decided_at")
                .orderByDesc("id"));
    }

    // ---------------------------------------------------------------- 私伙

    /** 回挂三段：一节点一节点点条数（当轮在册才算），新份旧份分开，齐不齐只问尺子 */
    private TVehSignBill attachRead(TVehSignBill b) {
        if (b == null) {
            return null;
        }
        List<TVehSignInk> all = vehSignInkMapper.selectList(new QueryWrapper<TVehSignInk>()
                .eq("bill_id", b.getId())
                .eq("del_flag", 0)
                .orderByAsc("node_no")
                .orderByAsc("round_no")
                .orderByAsc("ink_at")
                .orderByAsc("id"));
        int stage = VehSignRuler.stageOf(b);
        int billRound = b.getRoundNo() == null ? 1 : b.getRoundNo();

        List<TVehSignNodeView> views = new ArrayList<TVehSignNodeView>();
        for (int node = VehSignRuler.NODE_FIRST; node <= VehSignRuler.MAX_NODE; node++) {
            VehSignRuler.SignMode mode = VehSignRuler.modeOf(node);
            // 这一节点各轮分开：当轮=当前停着的那轮（已过的节点取它在册那轮），余皆旧份
            int currentRound;
            Map<Integer, List<TVehSignInk>> byRound = new LinkedHashMap<Integer, List<TVehSignInk>>();
            for (TVehSignInk ink : all) {
                if (ink.getNodeNo() == null || ink.getNodeNo() != node) {
                    continue;
                }
                int rn = ink.getRoundNo() == null ? 1 : ink.getRoundNo();
                List<TVehSignInk> bucket = byRound.get(rn);
                if (bucket == null) {
                    bucket = new ArrayList<TVehSignInk>();
                    byRound.put(rn, bucket);
                }
                bucket.add(ink);
            }
            if (node == stage) {
                currentRound = billRound;
            } else {
                // 已过的节点：在册(state=0)那一轮是它定论那一轮；未来节点没有轮
                currentRound = 0;
                for (Map.Entry<Integer, List<TVehSignInk>> e : byRound.entrySet()) {
                    for (TVehSignInk ink : e.getValue()) {
                        if (ink.getInkState() == null || ink.getInkState() == VehSignRuler.INK_STATE_LIVE) {
                            currentRound = Math.max(currentRound, e.getKey());
                        }
                    }
                }
            }

            List<TVehSignInk> current = new ArrayList<TVehSignInk>();
            List<TVehSignInk> history = new ArrayList<TVehSignInk>();
            for (Map.Entry<Integer, List<TVehSignInk>> e : byRound.entrySet()) {
                for (TVehSignInk ink : e.getValue()) {
                    boolean isCurrent = e.getKey() == currentRound
                            && (ink.getInkState() == null || ink.getInkState() == VehSignRuler.INK_STATE_LIVE);
                    if (isCurrent) {
                        current.add(ink);
                    } else {
                        history.add(ink);
                    }
                }
            }
            current.sort(Comparator.comparing(TVehSignInk::getInkAt,
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(n -> n.getId() == null ? Long.MAX_VALUE : n.getId()));

            TVehSignNodeView v = new TVehSignNodeView();
            v.setNodeNo(node);
            v.setNodeName(mode.title());
            v.setMode(mode.name());
            v.setNeedCount(mode.needCount());
            v.setRoundNo(currentRound == 0 ? null : currentRound);
            v.setCurrent(current);
            v.setHistory(history);
            v.setSignedCount(current.size());
            String outcome = node <= stage ? VehSignRuler.evaluate(node, current) : VehSignRuler.OUT_NOT_READY;
            // 条数与应落人数持平且尺子说过才算齐；待定、没齐都不算齐
            v.setReady(VehSignRuler.OUT_PASS.equals(outcome));
            views.add(v);
        }
        b.setNodes(views);
        return b;
    }

    /** 抹笔：本节点本轮在册各笔置 void 留痕（不物理删），更早节点/更早轮次一概不碰 */
    private void voidRound(Long billId, int node, int round) {
        List<TVehSignInk> live = currentRoundLive(billId, node, round);
        for (TVehSignInk ink : live) {
            ink.setInkState(VehSignRuler.INK_STATE_VOID);
            vehSignInkMapper.updateById(ink);
        }
    }

    /** 终审同判可移交：名册行与卷上结论同一次回算，一处算成两处同源 */
    private boolean writeRegister(TVehSignBill b, Date at) {
        TVehTransferRegister reg = new TVehTransferRegister();
        reg.setBillId(b.getId());
        reg.setBillNo(b.getBillNo());
        reg.setDecision(VehSignRuler.VERDICT_MOVE);
        reg.setDecidedAt(at);
        reg.setDelFlag(0);
        return transferRegisterMapper.insert(reg) == 1;
    }

    /** 进/退到某节点时该用第几轮：这节点已有轮次数 +1（头一回头一轮） */
    private int nextRoundNo(Long billId, int node) {
        List<TVehSignInk> rows = vehSignInkMapper.selectList(new QueryWrapper<TVehSignInk>()
                .eq("bill_id", billId)
                .eq("node_no", node)
                .eq("del_flag", 0));
        int max = 0;
        for (TVehSignInk ink : rows) {
            if (ink.getRoundNo() != null) {
                max = Math.max(max, ink.getRoundNo());
            }
        }
        return max + 1;
    }

    private List<TVehSignInk> currentRoundLive(Long billId, int node, int round) {
        return vehSignInkMapper.selectList(new QueryWrapper<TVehSignInk>()
                .eq("bill_id", billId)
                .eq("node_no", node)
                .eq("round_no", round)
                .eq("ink_state", VehSignRuler.INK_STATE_LIVE)
                .eq("del_flag", 0)
                .orderByAsc("ink_at")
                .orderByAsc("id"));
    }

    private TVehSignInk findLiveInk(Long billId, int node, int round, String signerNo) {
        List<TVehSignInk> rows = vehSignInkMapper.selectList(new QueryWrapper<TVehSignInk>()
                .eq("bill_id", billId)
                .eq("node_no", node)
                .eq("round_no", round)
                .eq("signer_no", signerNo)
                .eq("ink_state", VehSignRuler.INK_STATE_LIVE)
                .eq("del_flag", 0)
                .last("limit 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private TVehSignInk findInkByRequest(Long billId, String requestId) {
        List<TVehSignInk> rows = vehSignInkMapper.selectList(new QueryWrapper<TVehSignInk>()
                .eq("bill_id", billId)
                .eq("request_id", requestId)
                .eq("del_flag", 0)
                .last("limit 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private TVehSignBill loadByNo(String billNo) {
        List<TVehSignBill> rows = vehSignBillMapper.selectList(new QueryWrapper<TVehSignBill>()
                .eq("bill_no", billNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 不收笔的回话：卷上一格不动，按系统当段把卷情照回 */
    private VehSignReceipt rejectWithoutChange(VehSignReceipt rc, TVehSignBill b, String mark) {
        rc.setAccepted(false);
        rc.setNewInk(false);
        rc.setBillStatus(b.getStatus());
        rc.setRegistered(false);
        List<TVehSignInk> live = currentRoundLive(b.getId(), VehSignRuler.stageOf(b),
                b.getRoundNo() == null ? 1 : b.getRoundNo());
        rc.setOutcome(VehSignRuler.evaluate(VehSignRuler.stageOf(b), live));
        rc.setMark(mark);
        rc.setBill(attachRead(b));
        return rc;
    }

    /** 幂等/同人重笔：认卷上已有的头一笔，不另落行，定论照卷面当下那一段回 */
    private VehSignReceipt echoPrior(VehSignReceipt rc, TVehSignBill b, TVehSignInk prior, String mark) {
        rc.setAccepted(true);
        rc.setNewInk(false);
        rc.setBillStatus(b.getStatus());
        rc.setRegistered(false);
        int stageNow = VehSignRuler.stageOf(b);
        int roundNow = b.getRoundNo() == null ? 1 : b.getRoundNo();
        List<TVehSignInk> live = currentRoundLive(b.getId(), stageNow, roundNow);
        rc.setOutcome(VehSignRuler.evaluate(stageNow, live));
        rc.setMark(mark);
        rc.setBill(attachRead(b));
        return rc;
    }

    private static String appendDismiss(String old, String signer, String reason, Date at) {
        String line = "驳回[" + new SimpleDateFormat(FMT_TS).format(at) + "/" + signer + "]：" + reason;
        String o = old == null ? "" : old.trim();
        return o.isEmpty() ? line : o + "；" + line;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Date parseWallClock(String text) {
        String t = trimToNull(text);
        if (t == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_TS);
        sdf.setLenient(false);
        try {
            return sdf.parse(t);
        } catch (ParseException e) {
            return null;
        }
    }
}
