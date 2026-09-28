package com.zmh.atlantic.coffee.store.dto;

import java.util.List;

/** 门店视图结构（总体设计 §5.2 门店域契约）。 */
public final class StoreDtos {

    private StoreDtos() {
    }

    public record StoreBrief(Long id, String name, String address, String businessHours, String phone) {
    }

    public record StoreListResponse(List<StoreBrief> stores) {
    }
}
