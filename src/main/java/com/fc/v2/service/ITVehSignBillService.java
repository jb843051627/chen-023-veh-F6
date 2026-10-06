package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TVehSignBill;
import com.fc.v2.model.auto.TVehTransferRegister;
import com.fc.v2.model.custom.VehSignBoard;
import com.fc.v2.model.custom.VehSignDismissForm;
import com.fc.v2.model.custom.VehSignForm;
import com.fc.v2.model.custom.VehSignOpenForm;
import com.fc.v2.model.custom.VehSignQuery;
import com.fc.v2.model.custom.VehSignReceipt;

/**
 * 核签单（三节点新走法）Service：四问只认这一处——
 * 这一节点算不算齐、下一节点开不开、压回来清哪一段、点齐之后还收不收笔。
 * 旧 {@link ITVehMoveBillService} approve/reject/rollback 模样不改，这是另起接着的一套。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface ITVehSignBillService {

    /** 立案：线索代号必给，单号系统排（HS+雪花），立案即在签占数 */
    TVehSignBill openBill(VehSignOpenForm form);

    /** 落一笔：口信节点与系统当段对不上这笔当没落；同人同轮由唯一键挡；重递幂等 */
    VehSignReceipt sign(VehSignForm form);

    /** 某一节点整单驳回：全单止住，后面各节点不再收笔 */
    VehSignReceipt dismiss(VehSignDismissForm form);

    /** 按单号回卷情：三段一节点一节点点条数挂齐，旧轮旧笔分开放 */
    TVehSignBill detail(String billNo);

    /** 值班台账：名单与屏头在签张数同一条 SQL 同一回算 */
    VehSignBoard openBoard(VehSignQuery query);

    /** 全册在签张数回库现点（立案加一、终态减一；与台账标量子查询同口径，对得齐） */
    int countActive();

    /** 移交名册翻查（名册每一行都来自终审同判那一次回算） */
    List<TVehTransferRegister> listRegister();
}
