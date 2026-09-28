package com.zmh.atlantic.coffee.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.store.dto.StoreDtos.StoreBrief;
import com.zmh.atlantic.coffee.store.dto.StoreDtos.StoreListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreMapper storeMapper;

    public StoreListResponse list(String keyword) {
        var wrapper = new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1).orderByAsc(Store::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Store::getName, keyword).or().like(Store::getAddress, keyword));
        }
        List<StoreBrief> stores = storeMapper.selectList(wrapper).stream()
                .map(s -> new StoreBrief(s.getId(), s.getName(), s.getAddress(), s.getBusinessHours(), s.getPhone()))
                .toList();
        return new StoreListResponse(stores);
    }
}
