package com.fc.v2.mapper.custom;

import java.util.List;

import com.fc.v2.model.auto.TVehSignBill;
import com.fc.v2.model.custom.VehSignQuery;

/**
 * 核签单值班台账数据层：名单一页与全册在签张数只此一条 SELECT 同一快照。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TVehSignBillBoardMapper {

    /** 台账分页查询（屏头在签张数随名单同一回点算，不另起第二条口径） */
    List<TVehSignBill> selectBoardPage(VehSignQuery query);
}
