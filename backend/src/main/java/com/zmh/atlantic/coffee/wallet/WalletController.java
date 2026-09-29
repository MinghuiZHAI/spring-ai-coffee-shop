package com.zmh.atlantic.coffee.wallet;

import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.common.web.CursorPage;
import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.wallet.WalletDtos.RechargeRequest;
import com.zmh.atlantic.coffee.wallet.WalletDtos.WalletTransactionView;
import com.zmh.atlantic.coffee.wallet.WalletDtos.WalletView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钱包接口（03 v1.4 追加接口契约）：仅操作当前用户钱包——
 * 身份一律取 UserContext，无任何 userId 入参，"查/充他人钱包"在接口层面不存在。
 */
@RestController
@RequestMapping("/api/user/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Result<WalletView> wallet() {
        return Result.ok(walletService.getWallet(UserContext.requireUserId()));
    }

    @GetMapping("/transactions")
    public Result<CursorPage<WalletTransactionView>> transactions(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(walletService.transactions(UserContext.requireUserId(), cursor, limit));
    }

    /** 模拟充值（MVP：单次 1~1000 元，不做真实支付/余额抵扣/提现）。 */
    @PostMapping("/recharge")
    public Result<WalletView> recharge(@Valid @RequestBody RechargeRequest request) {
        return Result.ok(walletService.recharge(UserContext.requireUserId(), request.amount()));
    }
}
