package com.zmh.atlantic.coffee.wallet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /** 充值专用：行锁读取（事务内串行化同账户并发写入，04 v1.6 §8.3）。 */
    @Select("SELECT * FROM wallet_account WHERE user_id = #{userId} FOR UPDATE")
    Wallet selectByUserIdForUpdate(@Param("userId") Long userId);

    /** 入账并递增版本：在行锁保护下调用。 */
    @Update("UPDATE wallet_account SET balance = #{balance}, version = version + 1, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateBalance(@Param("id") Long id, @Param("balance") java.math.BigDecimal balance);
}
