# Atlantic Coffee · 前端构建体积与 Element Plus 按需引入评估

| 文档信息 | 说明 |
|----------|------|
| 所属阶段 | 五、编码实现（M2 前置评估，批次 7 收口后） |
| 版本 | v1.0 |
| 日期 | 2026-10-02 |
| 性质 | 评估方案——**不含任何代码改动**；实施与否、实施时机待拍板 |
| 背景 | 演示部署公网时首屏加载时间是关键体验；当前构建产物 >500kB 警告（docs/07 §3 遗留项） |

**修订记录**

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-10-02 | 初稿：构建体积实测、组件使用盘点、三方案对比与建议 |

---

## 1. 现状实测（2026-10-02，基线 5c25528）

### 1.1 引入方式

- `main.ts`：`import ElementPlus from 'element-plus'`（全量组件全局注册）+ `import 'element-plus/dist/index.css'`（全量样式）+ 中文 locale
- 无 `@element-plus/icons-vue`、无 unplugin 系列插件（vite.config 仅 vue 插件）
- 命令式 API（ElMessage/ElMessageBox）全部为**显式具名 import**（12 处），不依赖全局注册

### 1.2 构建产物（未压缩、无服务器 gzip 产物）

| 产物 | 体积 | 说明 |
|------|------|------|
| index-*.js | **~856 KB** | 单一入口，含全量 EP JS（路由组件已全部懒加载拆分，页面 chunk 均 <20KB） |
| index-*.css | **~355 KB** | 单一入口，含全量 EP 样式 |

### 1.3 组件使用盘点（grep `<el-`，15 个文件）

**模板组件 12 种**：el-button（25 处）、el-input（6）、el-select（4）、el-option（4）、el-table-column（6）、el-dialog（3）、el-radio（3）、el-form-item（2）、el-table（1）、el-radio-group（1）、el-form（1）、el-drawer（1）。

**命令式 API**：ElMessage（7 文件）、ElMessageBox（3 文件）、FormInstance/FormRules 类型（LoginPage）。

### 1.4 已有的有利条件

- 路由 15 处**全部懒加载**（页面代码已按路由拆分，入口体积大头就是 EP）
- 主题定制基于 **EP 内部 CSS 变量桥接**（tokens.css `--el-*` + element-plus.scss 72 行类名覆盖），非 SCSS 主题变量编译——与按需样式引入兼容性好

## 2. 方案对比

### 方案 A · unplugin 按需引入（推荐）

- **做法**：devDependencies 增加 `unplugin-vue-components` + `unplugin-auto-import`，vite.config 配 ElementPlusResolver；main.ts 移除全量引入与全量 CSS；`ElMessage`/`ElMessageBox` 由 resolver 自动带样式（或手动补 `element-plus/es/components/xx/style/css` 引入）；locale 手动保留
- **预估效果**：入口 JS → **~300KB±**（-65%）、CSS → **~80KB±**（-77%）；gzip 后传输约 100KB±/25KB±
- **工作量**：约半天（含全量回归）
- **回归清单**：①12 种组件逐个过（含 KbDocTable 的 el-table、各抽屉/对话框）；②el-button--cta 与 element-plus.scss 类名覆盖仍生效（验证导入顺序：EP 组件样式按需注入晚于覆盖文件的场景）；③ElMessage/ElMessageBox 样式；④zh-cn locale；⑤移动端 375 抽屉/对话框
- **风险**：低——组件清单封闭、无 SSR、类名覆盖依赖 EP 内部 CSS 变量（按需模式同样输出这些变量）

### 方案 B · 全量保留 + 服务器 gzip（部署侧兜底）

- **做法**：代码零改动；生产 Nginx 开 gzip/brotli + 静态资源强缓存
- **效果**：传输体积降 60-70%（JS ~250KB±），但**浏览器解析/执行体积不变**
- **定位**：与方案 A 并行（A 解决解析体积，B 解决传输体积），公网部署两者都建议做

### 方案 C · 手动逐组件 import（不推荐）

- 不用插件、每文件手动 import 组件与样式——维护成本高、易漏样式，收益与 A 相同

## 3. 建议与时机

1. **M2 启动前实施方案 A**（半天内，含回归）；实施本身不属 M2 功能批次，单独一笔 `build(frontend): EP 按需引入`
2. 公网部署时叠加方案 B（Nginx 配置）
3. 实施后更新 docs/07 §3 待办与本文件状态

## 4. 不评估项

- 商品图/字体等静态资源优化（当前无真实商品图）；CDN（部署架构未定）；代码分割策略调整（路由级已做，组件级待真实 Profile 出现再议）
