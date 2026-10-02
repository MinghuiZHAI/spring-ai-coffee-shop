-- M2 批次 1（SpecPicker 差价试算演示数据，用户批准）：
-- 为两个规格选项设置非 0 差价，验证前端 SpecPicker 的实时差价试算与购物车快照展示。
-- 仅 UPDATE 数据，无表结构变更（后端 Java 零改动）；
-- 历史订单不受影响：cart_item.spec_price_delta 与 order_item.unit_price 在落库时已固化。
-- 数值与语义均为演示口径（"无糖"按换代糖加价 3 元、"去冰"按加价 2 元），非真实定价；
-- 全局 spec_option 字典不区分商品品类，三组规格对所有商品可见（与 01 v1.4.2 口径一致）。

UPDATE spec_option SET price_delta = 3.00 WHERE spec_group = 'SWEETNESS' AND option_name = '无糖';
UPDATE spec_option SET price_delta = 2.00 WHERE spec_group = 'ICE' AND option_name = '去冰';
