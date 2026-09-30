/**
 * 钱包接口（批次 2 已上线的三个接口）：仅操作当前用户钱包（UserContext）。
 */
import { request } from './client'

export interface WalletView {
  balance: number
}

export function getWallet(): Promise<WalletView> {
  return request<WalletView>('/api/user/wallet')
}

export function recharge(amount: number): Promise<WalletView> {
  return request<WalletView>('/api/user/wallet/recharge', {
    method: 'POST',
    body: { amount },
  })
}
