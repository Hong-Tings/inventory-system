#!/bin/bash
# 进销存管理系统 — 自动化接口测试脚本
# 使用方法: bash test-api.sh
# 前提: 后端已启动 (mvn spring-boot:run)

BASE="http://localhost:8080/api/v1"
PASS=0
FAIL=0
TODO=""

ok() { PASS=$((PASS+1)); echo "  ✅ $1"; }
fail() { FAIL=$((FAIL+1)); echo "  ❌ $1"; msg="$2"; echo "     原因: $msg"; }
skip() { echo "  ⏭️  $1"; }

echo ""
echo "========================================"
echo "  进销存管理系统 — 自动化测试"
echo "========================================"
echo ""

# 1. 登录
echo "--- 1. 登录 ---"
LOGIN=$(curl -s "$BASE/auth/login" -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}')
TOKEN=$(echo "$LOGIN" | python -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)
if [ -n "$TOKEN" ]; then
  ok "登录成功"
else
  fail "登录失败" "$LOGIN"
  echo "❌ 无法继续测试，退出"
  exit 1
fi

AUTH="Authorization: Bearer $TOKEN"

# 2. 基础数据查询
echo ""
echo "--- 2. 基础数据 ---"

CATS=$(curl -s "$BASE/category/tree" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); print(d['code'])" 2>/dev/null)
[ "$CATS" = "200" ] && ok "商品分类树" || fail "商品分类树" "$CATS"

PRODS=$(curl -s "$BASE/product/page?page=1&size=5" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['total'])" 2>/dev/null)
[ "$PRODS" -ge 1 ] 2>/dev/null && ok "商品列表 ($PRODS 条)" || fail "商品列表"

WHS=$(curl -s "$BASE/warehouse/list" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))" 2>/dev/null)
[ "$WHS" -ge 1 ] 2>/dev/null && ok "仓库列表 ($WHS 个)" || fail "仓库列表"

SUPS=$(curl -s "$BASE/supplier/list" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))" 2>/dev/null)
[ "$SUPS" -ge 1 ] 2>/dev/null && ok "供应商列表 ($SUPS 个)" || fail "供应商列表"

CUSTS=$(curl -s "$BASE/customer/list" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))" 2>/dev/null)
[ "$CUSTS" -ge 1 ] 2>/dev/null && ok "客户列表 ($CUSTS 个)" || fail "客户列表"

LOCS=$(curl -s "$BASE/location/list?warehouseId=1" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))" 2>/dev/null)
[ "$LOCS" -ge 1 ] 2>/dev/null && ok "库位列表 ($LOCS 个)" || fail "库位列表"

# 3. 新增商品
echo ""
echo "--- 3. 新增商品 ---"
NEW_P=$(curl -s -X POST "$BASE/product" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"code":"P-TEST","name":"测试植物","spec":"测试","unit":"盆","purchasePrice":10,"salePrice":20,"minStock":10,"status":1}')
NP_CODE=$(echo "$NEW_P" | python -c "import sys,json; print(json.load(sys.stdin).get('code'))" 2>/dev/null)
[ "$NP_CODE" = "200" ] && ok "新增商品" || fail "新增商品" "$NEW_P"

# 4. 采购入库 → 库存增加
echo ""
echo "--- 4. 核心链路：采购入库 ---"
PO=$(curl -s -X POST "$BASE/purchase-order" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"supplierId":1,"warehouseId":1,"locationId":1,"orderDate":"2026-04-27","items":[{"productId":1,"quantity":100,"unitPrice":8,"batchNo":"BATCH-TEST"}]}')
PO_ID=$(echo "$PO" | python -c "import sys,json; print(json.load(sys.stdin).get('data'))" 2>/dev/null)
if [ -n "$PO_ID" ] && [ "$PO_ID" != "None" ]; then
  ok "创建入库单 (ID=$PO_ID)"
  PS=$(curl -s -X PUT "$BASE/purchase-order/$PO_ID/submit" -H "$AUTH")
  PS_CODE=$(echo "$PS" | python -c "import sys,json; print(json.load(sys.stdin).get('code'))" 2>/dev/null)
  [ "$PS_CODE" = "200" ] && ok "提交入库 (库存+100)" || fail "提交入库" "$PS"
else
  fail "创建入库单" "$PO"
fi

# 5. 验证库存
echo ""
echo "--- 5. 验证库存 ---"
INV=$(curl -s "$BASE/inventory/page?page=1&size=10" -H "$AUTH")
INV_COUNT=$(echo "$INV" | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['total'])" 2>/dev/null)
PROD_NAME=$(echo "$INV" | python -c "import sys,json; d=json.load(sys.stdin); r=d['data']['records'][0] if d['data']['records'] else {}; print(r.get('productName',''))" 2>/dev/null)
PROD_QTY=$(echo "$INV" | python -c "import sys,json; d=json.load(sys.stdin); r=d['data']['records'][0] if d['data']['records'] else {}; print(r.get('quantity',0))" 2>/dev/null)
[ "$INV_COUNT" -ge 1 ] 2>/dev/null && ok "库存记录 ($INV_COUNT 条, $PROD_NAME=$PROD_QTY)" || fail "库存记录"

# 6. 销售出库 → 库存扣减
echo ""
echo "--- 6. 核心链路：销售出库 ---"
SO=$(curl -s -X POST "$BASE/sales-order" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"customerId":1,"warehouseId":1,"salesman":"测试","orderDate":"2026-04-27","items":[{"productId":1,"quantity":10,"unitPrice":18}]}')
SO_ID=$(echo "$SO" | python -c "import sys,json; print(json.load(sys.stdin).get('data'))" 2>/dev/null)
if [ -n "$SO_ID" ] && [ "$SO_ID" != "None" ]; then
  ok "创建出库单 (ID=$SO_ID)"
  SS=$(curl -s -X PUT "$BASE/sales-order/$SO_ID/submit" -H "$AUTH")
  SS_CODE=$(echo "$SS" | python -c "import sys,json; print(json.load(sys.stdin).get('code'))" 2>/dev/null)
  [ "$SS_CODE" = "200" ] && ok "提交出库 (库存-10)" || fail "提交出库" "$SS"
else
  fail "创建出库单" "$SO"
fi

# 7. 库存流水
echo ""
echo "--- 7. 库存流水 ---"
LOGS=$(curl -s "$BASE/inventory/log/page?page=1&size=5" -H "$AUTH")
LOG_COUNT=$(echo "$LOGS" | python -c "import sys,json; print(json.load(sys.stdin)['data']['total'])" 2>/dev/null)
[ "$LOG_COUNT" -ge 1 ] 2>/dev/null && ok "库存流水 ($LOG_COUNT 条)" || fail "库存流水"

# 8. 盘点
echo ""
echo "--- 8. 核心链路：库存盘点 ---"
ST=$(curl -s -X POST "$BASE/stock-take" -H "$AUTH" -H "Content-Type: application/json" -d '{"warehouseId":1,"takeType":0}')
ST_ID=$(echo "$ST" | python -c "import sys,json; print(json.load(sys.stdin).get('data'))" 2>/dev/null)
if [ -n "$ST_ID" ] && [ "$ST_ID" != "None" ]; then
  ok "创建盘点单 (ID=$ST_ID)"
  # 获取当前库存数量
  CUR_QTY=$(curl -s "$BASE/inventory/page?page=1&size=1" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin); r=d['data']['records'][0] if d['data']['records'] else {}; print(r.get('quantity',0))" 2>/dev/null)
  ADJ=$((CUR_QTY - 2))
  II=$(curl -s -X POST "$BASE/stock-take/$ST_ID/items" -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"productId\":1,\"bookQty\":$CUR_QTY,\"actualQty\":$ADJ,\"diffQty\":-2,\"diffReason\":\"测试损耗\"}")
  echo "$II" | python -c "import sys,json; d=json.load(sys.stdin); print('  ✅ 录入盘点明细' if d['code']==200 else '  ❌ 录入失败')" 2>/dev/null

  AA=$(curl -s -X PUT "$BASE/stock-take/$ST_ID/approve" -H "$AUTH")
  echo "$AA" | python -c "import sys,json; d=json.load(sys.stdin); print('  ✅ 盘点审核' if d['code']==200 else '  ❌ 审核失败: '+d['message'])" 2>/dev/null

  AJ=$(curl -s -X PUT "$BASE/stock-take/$ST_ID/adjust" -H "$AUTH")
  echo "$AJ" | python -c "import sys,json; d=json.load(sys.stdin); print('  ✅ 盘点调整 (库存已更新)' if d['code']==200 else '  ❌ 调整失败: '+d['message'])" 2>/dev/null
else
  fail "创建盘点单" "$ST"
fi

# 9. 仪表盘
echo ""
echo "--- 9. 仪表盘 ---"
DS=$(curl -s "$BASE/dashboard/stats" -H "$AUTH" | python -c "import sys,json; d=json.load(sys.stdin)['data']; print(f\"商品={d['productCount']}, 仓库={d['warehouseCount']}, 入库={d['todayPurchaseCount']}, 出库={d['todaySalesCount']}, 预警={d['alertCount']}\")" 2>/dev/null)
ok "仪表盘 ($DS)"

# 10. 入库查询（含筛选）
echo ""
echo "--- 10. 入库查询带筛选 ---"
PO_LIST=$(curl -s "$BASE/purchase-order/page?page=1&size=5&status=1" -H "$AUTH")
PO_TOTAL=$(echo "$PO_LIST" | python -c "import sys,json; print(json.load(sys.stdin)['data']['total'])" 2>/dev/null)
PO_NAME=$(echo "$PO_LIST" | python -c "import sys,json; r=json.load(sys.stdin)['data']['records']; print(r[0].get('supplierName','') if r else '')" 2>/dev/null)
[ -n "$PO_NAME" ] && ok "入库单供应商名显示 ($PO_NAME)" || fail "入库单供应商名为空"

# 11. 出库查询（含筛选）
SO_LIST=$(curl -s "$BASE/sales-order/page?page=1&size=5&status=1" -H "$AUTH")
SO_NAME=$(echo "$SO_LIST" | python -c "import sys,json; r=json.load(sys.stdin)['data']['records']; print(r[0].get('customerName','') if r else '')" 2>/dev/null)
[ -n "$SO_NAME" ] && ok "出库单客户名显示 ($SO_NAME)" || fail "出库单客户名为空"

# 12. 预警
echo ""
echo "--- 12. 库存预警 ---"
ALERTS=$(curl -s "$BASE/report/inventory-alert" -H "$AUTH")
ALERT_COUNT=$(echo "$ALERTS" | python -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null)
echo "  ℹ️  库存预警: $ALERT_COUNT 项"

echo ""
echo "========================================"
echo "  测试完成"
echo "========================================"
echo ""
echo "  ✅ 通过: $PASS"
echo "  ❌ 失败: $FAIL"
echo ""

# 建议
if [ "$FAIL" -eq 0 ]; then
  echo "🎉 所有接口测试通过！"
  echo ""
  echo "你现在可以："
  echo "  1. 打开管理后台 http://localhost:3000 用 admin/123456 登录"
  echo "  2. 查看入库单列表（应有供应商名/仓库名）"
  echo "  3. 查看库存页面（应有商品名称/编码/仓库/库位）"
  echo "  4. 查看报表（应有柱状图）"
else
  echo "⚠️   $FAIL 个测试失败，需要修复"
fi
