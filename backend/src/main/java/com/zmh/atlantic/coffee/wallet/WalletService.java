package com.zmh.atlantic.coffee.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.CursorPage;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.wallet.WalletDtos.WalletTransactionView;
import com.zmh.atlantic.coffee.wallet.WalletDtos.WalletView;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包服务（03 v1.4 追加 / 04 v1.6 §8.3）：
 * 懒创建（首次查询/充值时补建 0 元账户，uk_wallet_user 兜底并发）→
 * 充值 = 行锁（FOR UPDATE）串行化 + 余额更新 + RECHARGE 流水，同一事务原子提交。
 * 钱包仅由业务系统操作（01 v1.3 追加"AI 不触钱包"）；身份由调用方从 UserContext 取。
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private static final int LIMIT_CAP = 50;

    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    /** 余额查询：账户不存在则懒创建（balance=0）。 */
    public WalletView getWallet(Long userId) {
        return new WalletView(ensureWalletRow(userId).getBalance());
    }

    /** 模拟充值：1~1000 元（校验在 Controller 注解层，服务内兜底），行锁下入账并写流水。 */
    @Transactional
    public WalletView recharge(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ONE) < 0
                || amount.compareTo(new BigDecimal("1000")) > 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "单次充值金额需在 1~1000 元之间");
        }
        ensureWalletRow(userId);
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            throw new BizException(ResultCode.NOT_FOUND, "钱包账户不存在");
        }
        BigDecimal newBalance = wallet.getBalance().add(amount);
        walletMapper.updateBalance(wallet.getId(), newBalance);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType("RECHARGE");
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setRemark("模拟充值");
        walletTransactionMapper.insert(tx);
        return new WalletView(newBalance);
    }

    /** 流水查询：游标分页（id 倒序，与订单列表同口径）。 */
    public CursorPage<WalletTransactionView> transactions(Long userId, Long cursor, int limit) {
        int capped = Math.min(limit, LIMIT_CAP);
        List<WalletTransaction> rows = walletTransactionMapper.selectList(
                new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getUserId, userId)
                        .lt(cursor != null, WalletTransaction::getId, cursor)
                        .orderByDesc(WalletTransaction::getId)
                        .last("LIMIT " + capped));
        List<WalletTransactionView> views = rows.stream().map(WalletTransactionView::from).toList();
        Long nextCursor = rows.size() == capped && !rows.isEmpty()
                ? rows.get(rows.size() - 1).getId() : null;
        return new CursorPage<>(views, nextCursor);
    }

    // ===== 内部 =====

    /** 懒创建：不存在则插入 0 元账户；并发首查由 uk_wallet_user 唯一键兜底。 */
    private Wallet ensureWalletRow(Long userId) {
        Wallet existing = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>()
                .eq(Wallet::getUserId, userId));
        if (existing != null) {
            return existing;
        }
        Wallet created = new Wallet();
        created.setUserId(userId);
        created.setBalance(BigDecimal.ZERO);
        created.setVersion(0);
        try {
            walletMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发首查：另一事务已建户，回读即可
            return walletMapper.selectOne(new LambdaQueryWrapper<Wallet>()
                    .eq(Wallet::getUserId, userId));
        }
    }
}
