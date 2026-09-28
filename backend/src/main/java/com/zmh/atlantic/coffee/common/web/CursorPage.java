package com.zmh.atlantic.coffee.common.web;

import java.util.List;

/** 游标分页统一结构（总体设计 §5.1：data.list[] + data.nextCursor，null 表示到底）。 */
public record CursorPage<T>(List<T> list, Long nextCursor) {
}
