package com.fc.v2.common.exception;

/**
 * 核签单（三节点新走法）递交退回：必须点明卡在哪一格。
 * 只用于格错（落笔人空着、话不是可移交/不予移交两样、立案线索没落）；
 * 查无此单、封了档、跳关、待定挂着这类"不收"由回执 accepted=false 回话，不抛。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class VehSignRejectException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 出毛病的那一格（字段名/节点名） */
    private final String field;

    public VehSignRejectException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
