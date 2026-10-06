package com.fc.v2.mapper.custom;

import java.util.List;

import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.custom.VehXferQuery;

/**
 * 核签单台账屏查数据层。
 * 屏上显的在核张数、逐节点点出的三段张数、当段已落几人，与一张一张点出来的单
 * 出自同一条 SELECT、同一回点算，不许两本账。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TVehXferBillBoardMapper {

    /** 台账开屏：每页名单上自带全册单数、在核单数、逐节点在核张数、当段本轮已落几人。 */
    List<TVehXferBill> selectBoardPage(VehXferQuery query);
}
