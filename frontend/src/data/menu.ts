/**
 * 菜单静态假数据（M1-6 步骤 2）：严格镜像 V2 种子的 5 分类 / 25 SKU / 标签体系
 * （backend/src/main/resources/db/migration/V2__seed_data.sql）。
 * 接入后端后由 GET /api/user/menu 的响应替换，字段名与接口契约对齐。
 * tint/glyph 为商品图占位的视觉元数据（真实商品图就位后移除）。
 */

export interface MenuCategory {
  id: number
  name: string
  /** 图占位渐变（起/止色） */
  tint: [string, string]
  /** 图占位线条颜色 */
  glyphColor: string
  /** 图占位线稿类型 */
  glyph: 'coffee' | 'latte' | 'citrus' | 'bubbles' | 'bread'
}

export interface MenuItem {
  id: number
  categoryId: number
  name: string
  description: string
  price: number
  tags: string[]
}

export const MENU_CATEGORIES: MenuCategory[] = [
  { id: 1, name: '经典咖啡', tint: ['#dcebf5', '#c3ddec'], glyphColor: '#0369a1', glyph: 'coffee' },
  { id: 2, name: '奶咖系列', tint: ['#f4efe8', '#e7dccb'], glyphColor: '#8a6a4b', glyph: 'latte' },
  { id: 3, name: '特调果咖', tint: ['#fdeee3', '#fadcc2'], glyphColor: '#c2410c', glyph: 'citrus' },
  { id: 4, name: '非咖啡饮品', tint: ['#e3f2fd', '#c8e4f9'], glyphColor: '#0284c7', glyph: 'bubbles' },
  { id: 5, name: '烘焙小食', tint: ['#f5efe2', '#ebdfc8'], glyphColor: '#b45309', glyph: 'bread' },
]

export const MENU_ITEMS: MenuItem[] = [
  { id: 1, categoryId: 1, name: '深海美式', description: '经典意式浓缩与冰水的对话，入口微苦、回甘干净', price: 12, tags: ['微苦', '清爽', '含咖啡因'] },
  { id: 2, categoryId: 1, name: '冷萃远航', description: '低温慢萃 12 小时，顺滑低酸，远航路上的清醒伴侣', price: 15, tags: ['微苦', '清爽', '含咖啡因'] },
  { id: 3, categoryId: 1, name: '潮汐意式浓缩', description: '双份浓缩，油脂厚重，适合需要立刻清醒的时刻', price: 14, tags: ['浓烈', '含咖啡因'] },
  { id: 4, categoryId: 1, name: '赤道手冲', description: '单一产区手冲，花果酸香明亮，每日限定豆单', price: 18, tags: ['果香', '微酸', '含咖啡因'] },
  { id: 5, categoryId: 2, name: '湾流拿铁', description: '经典意式与丝绒奶泡的平衡之作', price: 18, tags: ['奶香', '微甜', '含咖啡因'] },
  { id: 6, categoryId: 2, name: '燕麦洋流拿铁', description: '植物基燕麦奶与浓缩咖啡，谷物香明显', price: 20, tags: ['奶香', '微甜', '含咖啡因', '植物基'] },
  { id: 7, categoryId: 2, name: '焦糖风暴玛奇朵', description: '焦糖淋面与分层奶咖，甜与苦的风暴交界', price: 22, tags: ['奶香', '偏甜', '含咖啡因'] },
  { id: 8, categoryId: 2, name: '馥芮白·深海版', description: '双份浓缩配绵密奶沫，咖啡感更强的奶咖', price: 21, tags: ['奶香', '微苦', '含咖啡因'] },
  { id: 9, categoryId: 2, name: '摩卡航海家', description: '巧克力与浓缩的暖流，顶部淡奶油', price: 22, tags: ['奶香', '偏甜', '含咖啡因', '巧克力'] },
  { id: 10, categoryId: 3, name: '西柚冰汐', description: '西柚汁与冷萃的分层特调，微苦回甜', price: 23, tags: ['果香', '清爽', '含咖啡因'] },
  { id: 11, categoryId: 3, name: '荔枝海风冷萃', description: '荔枝果肉与冷萃咖啡，盛夏海风的味道', price: 24, tags: ['果香', '偏甜', '清爽', '含咖啡因'] },
  { id: 12, categoryId: 3, name: '莓果暗涌', description: '莓果酱与拿铁的暗色漩涡，酸甜平衡', price: 23, tags: ['果香', '偏甜', '含咖啡因'] },
  { id: 13, categoryId: 3, name: '椰岛生椰咖', description: '厚椰乳与浓缩，热带岛屿的松弛感', price: 20, tags: ['果香', '奶香', '清爽', '含咖啡因'] },
  { id: 14, categoryId: 3, name: '青柠气泡冰咖', description: '青柠与气泡水的冰咖特调，气泡感十足', price: 21, tags: ['果香', '清爽', '含咖啡因', '气泡'] },
  { id: 15, categoryId: 4, name: '抹茶静谧', description: '石磨抹茶与鲜奶，苦甜之间的一片宁静', price: 19, tags: ['奶香', '微甜', '不含咖啡因'] },
  { id: 16, categoryId: 4, name: '可可暖流', description: '浓郁可可与牛奶，冬天里的暖流', price: 20, tags: ['奶香', '偏甜', '不含咖啡因', '巧克力'] },
  { id: 17, categoryId: 4, name: '桂花乌龙奶茶', description: '桂花香乌龙底奶茶，秋天被装进了杯子', price: 18, tags: ['奶香', '偏甜', '不含咖啡因'] },
  { id: 18, categoryId: 4, name: '柠檬远航气泡水', description: '现打柠檬与气泡水，酸爽提神不含咖啡因', price: 16, tags: ['果香', '清爽', '不含咖啡因', '气泡'] },
  { id: 19, categoryId: 4, name: '太平洋水果茶', description: '五种水果与茉莉绿茶底的大杯满足', price: 19, tags: ['果香', '偏甜', '清爽', '不含咖啡因'] },
  { id: 20, categoryId: 4, name: '厚牛乳', description: '冷藏鲜奶直供，简单纯粹的奶香', price: 12, tags: ['奶香', '不含咖啡因'] },
  { id: 21, categoryId: 5, name: '黄油可颂', description: '现烤可颂，外层酥脆内里湿润', price: 12, tags: ['烘焙', '黄油香'] },
  { id: 22, categoryId: 5, name: '巧克力丹麦', description: '丹麦酥皮夹层层巧克力', price: 15, tags: ['烘焙', '偏甜', '巧克力'] },
  { id: 23, categoryId: 5, name: '提拉米苏', description: '马斯卡彭与咖啡浸手指饼的经典搭配', price: 22, tags: ['甜品', '偏甜', '含微量咖啡因'] },
  { id: 24, categoryId: 5, name: '芝士贝果', description: '贝果配芝士抹酱，扎实饱腹', price: 16, tags: ['轻食', '烘焙'] },
  { id: 25, categoryId: 5, name: '海盐芝士蛋糕', description: '海盐平衡甜腻的芝士蛋糕', price: 20, tags: ['甜品', '偏甜', '烘焙'] },
]
