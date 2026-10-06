package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.auto.TVehXferRoster;
import com.fc.v2.model.custom.VehXferAuditBoard;
import com.fc.v2.model.custom.VehXferBoard;
import com.fc.v2.model.custom.VehXferOpenForm;
import com.fc.v2.model.custom.VehXferQuery;
import com.fc.v2.model.custom.VehXferSignForm;
import com.fc.v2.model.custom.VehXferSignReceipt;

/**
 * 移交执法核签单（系统盯办一套）Service接口。
 *
 * 这一节点算不算齐、下一节点开不开、被压回来时清哪一段、点齐之后还收不收笔——
 * 四问只认 {@link VehXferRuler} 与本实现一处给的答案，别处问不出第二套。
 * 旧核签单一套（ITVehMoveBillService）模样不改，本套是另起的新名目。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface ITVehXferBillService {

    /** 按主键回读：三段逐段回显（应落几人/口径/已落几人/各笔写了什么）挂齐 */
    TVehXferBill selectTVehXferBillById(Long id);

    /** 台账开屏：名单与在核张数、逐节点张数同一条 SQL 同一回点算 */
    VehXferBoard openBoard(VehXferQuery query);

    /** 立案：单代号系统排，从头一段第一轮起，立案一张在核数加一 */
    TVehXferBill openBill(VehXferOpenForm form);

    /**
     * 递单落笔（唯一入口）：口信节点号只当来意，对不上那一笔当它没落；
     * 重复递交按幂等；落够人数按当段口径结算——过、压回（不予移交）或挂待定。
     */
    VehXferSignReceipt sign(VehXferSignForm form, Date at);

    /** 不予移交（收只收一格）：本段本轮落笔当场抹去、单子压回一格，抹笔与退档同一事务 */
    TVehXferBill pushBack(Long id, String comment);

    /** 驳回：全单就此止住，后面各段不再收笔；已封卷返回 null */
    TVehXferBill reject(Long id, String comment);

    /** 改备注：封卷后改不了一个字，被拒返回 false */
    boolean updateContent(Long id, String remark);

    /** 撤单：封卷后整张抽不掉，被拒返回 false */
    boolean remove(Long id);

    /** 移交名册（受理簿）：办讫一张进一行，与终审结论同源一次回算 */
    List<TVehXferRoster> listRoster(String billNo, String day);

    /** 事后翻查：从第三段一节点一节点数回第一段，与名册一笔不差才算对得上 */
    VehXferAuditBoard audit(String day);
}
