
管理后台


第一次修改
1.给新建入库单的商品宽度调整一下，收缩一点， 数量和单价的宽度扩大一点，保证能看到完整数字，
2.仓库管理中的仓库列表，商品总数是显示的但是库存金额显示的0，以及在仓库列表添加备注栏
3.库位管理，不显示所属仓库，应该增加单位，并在列表显示单位，
4.供应商导出excel显示系统内部错误，供应商查询需要支持名称和联系人以及电话和地址查询
5.商品分类的查询和筛选需要分开，查询是输入内容点击查询后刷新过滤后的列表，状态仓库等等这些筛选应该在点击某个下拉框后自动刷新，不需点击查询再刷新，以及仓库的筛选默认的全部改为全部仓库，增加采购价区间筛选，增加销售价区间筛选，增加时间选择区间筛选。
6。在所有导入excel功能选择xsxl后显示文件解析失败，请检查 Excel 格式: ### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'P-018' for key 'product.uk_code' ### The error may exist in com/inventory/product/mapper/ProductMapper.java (best guess) ### The error may involve com.inventory.product.mapper.ProductMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO product ( code, name, spec, unit, purchase_price, sale_price, min_stock, status, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'P-018' for key 'product.uk_code' ; Duplicate entry 'P-018' for key 'product.uk_code'，这个导入的excel格式该怎么统一，这个问题我还不知道如何解决，如果你有成熟易用方案请告诉我，
7.商品分类功能，只在一个商品分类列表里面进行了分类，但是好像没有用到这个分类的地方，那些查询场景也没有分类查询之类的，如果你认为该系统需要留着，你就更新这个分类系统体现出它的作用，如果你认为不需要留着把分类系统删除，
8.仓库管理列表，添加仓库地址仓库负责人联系电话查询，添加商品总数区间筛选，添加库存金额区间筛选，添加更正后的excel导入系统，
9.采购入库，添加供应商查询，仓库筛选，每次入库的数量区间筛选，每次的总金额查询，添加一列采购单价，显示采购入库时的单价，
10.新建出库单，在用户选择商品的下拉栏中，应该不仅仅显示商品名称，还应该根据选择出库的仓库，查询该商品在该仓库的库存量，并且同第一条商品宽度调整一下，收缩一点， 数量和单价的宽度扩大一点，保证能看到完整数字
11.在账号为管理员权限时为采购入库以及销售出库的所有出入库单添加删除和批量操作功能，以及销售出库的查询和筛选同第5点修改，


第二次修改
1.点击商品列表显示ProductList.vue:56 Uncaught (in promise) Error: 系统内部错误: text
    at request.ts:34:29
    at async fetchData (ProductList.vue:52:17)并且不显示商品数据，以及商品分类这个功能有个父子级分类，但是在商品列表中筛选下拉框父子级的分类是直接合到一起的，这样会导致混在一起，对于这个分类应用你有什么方案
2.将所有系统的导入excel功能全部取消掉。
3.新建入库单出库单填写好数据后，点击保存成草稿后，并不能二次编辑提交，失去了草稿的作用，
4.现在的仓库管理系统，商品管理系统的采购价销售价，和采购或者销售出入库的新建出入库表单数据是否该统一，因为我在测试入库的时候，填写的金额是不和商品管理里面的采购价统一的，这就导致了一个商品，在采购入库的金额和仓库管理总金额以及商品管理的采购价完全不同，销售价可能也有次问题，我不知道该如何解决
5.销售出库添加客户筛选，仓库筛选，数量区间筛选，总金额区间筛选，销售员查询，出库日期区间筛选。以及不能批量删除，还有新建入库出库单的商品，下拉栏，只有选择该商品的时候才会有库存数量，请给每个商品都增添较为醒目的库存数量，还有在新建出入库单的页面，商品列的宽度较大，需要缩小，数量和售价的宽度较小，影响数字全部显示，
6.库存盘点功能疑似不完全，并且同前面的系统一样，添加仓库筛选，盘点方式筛选，商品总数区间筛选，差异数区间筛选，盘点人查询，审核人查询，以及在账号权限为管理员的时候添加删除以及批量删除或者修改
7.库存调拨，新建调拨单同上述一样需要在商品下拉栏显示每个商品在调出仓库的库存，并且醒目，在提交新建调拨单的时候显示报错系统内部错误: ### Error updating database. Cause: java.sql.SQLException: Field 'from_warehouse_id' doesn't have a default value ### The error may exist in com/inventory/transfer/mapper/InventoryTransferMapper.java (best guess) ### The error may involve com.inventory.transfer.mapper.InventoryTransferMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO inventory_transfer ( order_no, total_quantity, status, operator_id, remark, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLException: Field 'from_warehouse_id' doesn't have a default value ; Field 'from_warehouse_id' doesn't have a default value
在库存调拨列表，需要添加调拨单号查询，调出仓库筛选调入仓库筛选，
8.统计报表，入库汇总无统计图，



第三次修改
1.进入商品列表页面依旧显示Error: 系统内部错误: text，
2.客户管理，增加联系人查询，电话查询，地址查询，
3.供应商和客户在账号身份为管理员的时候添加批量删除或者批量修改或者单个修改
4.库位管理在身份为管理员的时候添加批量操作，删除或者启用修改
5.采购入库在身份为管理员的时候添加批量操作，删除或者其他，
6.销售出库已出库的单并不能删除，并且此页面没有批量删除
7.新建出库单新建入库单的商品明细下的商品，点击下拉栏某个商品后，不显示商品名称和库存，应该常驻显示，并且，售价并没有刷新同步，并且，列表页面，取消按钮和删除按钮颜色都是红色是相同的，调整一下，
8.库存管理，仓库筛选栏，进去默认应该是全部仓库，再往下才是主要仓库二号仓库什么的，并且不选择仓库直接进行商品名称搜索无效，
9.库存调拨列表不显示调出仓库和调入仓库，日期也不现实，并且没有日期筛选，并且没有数量区间筛选，没有操作人查询
10.操作日志这方面，应该贴合客户的使用对象，客户看不懂的操作日志是没有发挥出来作用的



第四次修改
1.新建入库单，选择商品之后只有第一个商品的价格会同步，后续不管选什么商品都没有同步，都是选的第一个物品的价格。
2.新建出库单，选择商品之后只有第一个商品的价格会同步，后续不管选什么商品都没有同步，都是选的第一个物品的价格。
3.新建入库单, 仓库库位的作用是什么，选择第一个仓库后，库位选择第一个仓库的第一个库位，再给仓库切换成第二仓库，直接成了第二个仓库，但是库位还是第一个仓库的，这逻辑是否有致命缺陷呢，我建议是你把关于库位的所有逻辑全部去掉，因为是小店并不需要库位什么玩意的，
4.还有这个扫码录入这个逻辑，我想不通这个业务流程到底是怎样的，码从哪来，扫码本质是获取信息，录入信息，那录入的信息又从哪来，能保持格式和系统一样吗





第四次修改，
1.仓库管理，地址输入框按下回车没有执行持续查询操作，商品列表商品名称查询，商品编码查询在输入框输入搜索的东西后，列表刷新成搜索的东西，点击输入框里的小X号后，没有自动重置列表，供应商管理的供应商名称以及联系人联系电话，地址点击输入框的小X后均没有自动重置列表，客户管理里的客户名称、联系人、联系电话、地址等也是这种情况，仓库管理的仓库名称、联系人、联系电话、地址等也是这种情况，并且仓库地址查询失效，查询无反应，采购入库的入库单号查询也是此情况，销售出库的出库单号查询以及销售员查询也是此情况，库存管理的商品名称查询也是此情况，库存盘点的盘点单号查询也是此情况，盘点人和审核人也是此情况，并且列表不显示盘点单的仓库，以及新建盘点单没有盘点人和审核人，列表不显示盘点人和审核人，库存挑拨的调拨单号查询也是没有自动重置列表，操作人查询也是没有自动重置，
2.添加功能，商品列表添加，采购价升序降序，销售价升序降序，当前库存升序降序，最低库存升序降序，状态筛选添加启用停用
仓库管理，添加仓库管理新增日期，商品总数升序降序，库存金额升序降序，采购入库添加，总数量升序降序、总金额升序降序、采购单价升序降序、入库日期从最早到最新排序和最新到最早排序，销售出库添加总数量升序降序、总金额升序降序、出库日期由早到晚以及由晚到早排序，库存管理，添加可用库存升序降序，锁定库存升序降序，可发库存升序降序，库存盘点，添加商品数升序降序，差异数升序降序，库存调拨，添加数量升序降序，添加日期早到晚，晚到早排序，

3.其他bug，库存调拨不显示调出仓库调入仓库日期，以及无在管理员权限环境下无删除键，无批量删除键，无批量取消键，




第五次修改
1.库存流水和库存管理放在二级菜单，就和商品管理一样，一级管理就保持叫库存管理就行了，二级菜单的库存管理重命名为库存列表，
2.库存流水添加时间早到晚，晚到早排序，商品名称不显示，仓库名称不显示，调拨操作的类型显示不是中文，调拨入库的类型显示TRANSFER_IN，调拨出库的类型显示TRANSFER_OUT，以及增加商品名称查询后点击输入框后的小X号自动重置列表，操作人也不显示，增加仓库筛选，增加操作类型筛选，增加关联单号查找，增加操作人查找，新增的查询都需要有自动重置列表
3.库存调拨的批量删除按钮无需直接展示，同采购出入库的那种形式就行，记得所有删除选项均需要在管理员身份才可启用，库存调拨的取消按钮和删除按钮颜色相同，需要修改，库存盘点批量删除形式同上添加，
4.新建出库新建入库单，这两个在选择商品的时候依旧是选择第一个商品后，价格自动刷新了第一次选择的商品采购/销售价，再修改选择商品后依旧是第一个商品的采购/销售价格，
5.新建调拨单的商品下拉栏选择后，只显示一个数字不显示商品的名称，以及禁止同仓库调拨，同仓库调拨属于是无效操作，禁止同仓库调拨，

第六次修改

1.库存调拨列表和详细不显示调出仓库和调入仓库
2.库存调拨时没有填写操作人的地方
3.库存盘点差异数量区间查询无效，
4.新建盘点单没有填写盘点人的地方，在盘点中状态，盘点单详细中没有填写审核人的地方
5.库存盘点商品数量区间查询无效，
6.库存列表下方自动汇总全部库存的可用库存数量和锁定库存数量以及可发库存数量，随筛选而变化
7.根据客户要求，删除功能需要重构，在商品管理、商品分类、供应商管理、客户管理、仓库管理、采购入库、销售出库、库存盘点、库存调拨场景，均把删除调整为作废，作废后进入无限期储存的回收站，回收站要求给收纳的单据分类，在用户想恢复的时候有利于寻找单据恢复，当然，以上均要在管理员身份的情况下，员工身份不得有任何删除或者弃用的操作

第七次修改
1.任何场景作废后回收站均无数据
2.作废相当于删除，在任何有作废按钮的场景均设为红色，停用保持黄色
3.商品分类如果有商品引用，禁止作废
4.新增供应商，保存表单提示系统内部错误: [DuplicateKeyException] ### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'supplier.uk_code' ### The error may exist in com/inventory/supplier/mapper/SupplierMapper.java (best guess) ### The error may involve com.inventory.supplier.mapper.SupplierMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO supplier ( code, name, contact, phone, address, remark, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'supplier.uk_code' ; Duplicate entry '' for key 'supplier.uk_code'
5.采购入库和销售入库已取消、草稿状态的可以作废，已入库的禁止作废，
6.库存盘点，盘点中的单据作废后只有状态消失，列表并没有消失
7.调拨单商品明细的商品栏宽度缩小，数量栏宽度增加
8.调拨栏同第6点原因一样的单据作废后只有状态消失，列表并没有消失
9.调拨单作废仅对已取消、草稿状态开放，已完成的现在也有作废，


第八次修改
1.商品列表、商品分类、供应商管理、客户管理、仓库管理的作废键依旧黄色
2.供应商管理，保存新增供应商后爆系统内部错误: [DuplicateKeyException] ### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'SUP-005' for key 'supplier.uk_code' ### The error may exist in com/inventory/supplier/mapper/SupplierMapper.java (best guess) ### The error may involve com.inventory.supplier.mapper.SupplierMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO supplier ( code, name, contact, phone, address, remark, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'SUP-005' for key 'supplier.uk_code' ; Duplicate entry 'SUP-005' for key 'supplier.uk_code'
3.给所有导出的excel的时间数据列宽度大幅拉高，列不够会导致时间显示为#不直接显示出来时间
4.供应商管理列表显示新增时间，以及时间升降序
5.客户管理显示客户新增时间，以及升降序
6.仓库管理显示仓库创建时间，上次更新时间，将备注和地址栏的宽度稍微压缩一下，并在仓库管理列表下方，显示总数和金额总数，随筛选或者查询变化
7.采购入库、采购出库，日期精确秒数，并在列表下方添加总数量汇总和总金额汇总，随筛选变化
8.销售出库列表下方添加汇总总数量，总金额，随筛选而变化，出库日期精确到秒，
9.商品作废后没有进入回收站，商品分类作废后没有进入回收站、供应商作废后没有进入回收站、客户作废后没有进入回收站、仓库作废后没有进入回收站、
10，新增仓库编码留空提交会爆系统内部错误: [DuplicateKeyException] ### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'warehouse.uk_code' ### The error may exist in com/inventory/warehouse/mapper/WarehouseMapper.java (best guess) ### The error may involve com.inventory.warehouse.mapper.WarehouseMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO warehouse ( code, name, contact, phone, address, status, remark, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'warehouse.uk_code' ; Duplicate entry '' for key 'warehouse.uk_code'
11，商品新增编码留空爆系统内部错误: [DuplicateKeyException] ### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'product.uk_code' ### The error may exist in com/inventory/product/mapper/ProductMapper.java (best guess) ### The error may involve com.inventory.product.mapper.ProductMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO product ( code, name, spec, unit, purchase_price, sale_price, image_url, min_stock, max_stock, status, remark, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'product.uk_code' ; Duplicate entry '' for key 'product.uk_code'
12.客户新增编码留空爆系统内部错误: [DuplicateKeyException] ### Error updating database. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'customer.uk_code' ### The error may exist in com/inventory/customer/mapper/CustomerMapper.java (best guess) ### The error may involve com.inventory.customer.mapper.CustomerMapper.insert-Inline ### The error occurred while setting parameters ### SQL: INSERT INTO customer ( code, name, contact, phone, address, remark, create_time, update_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? ) ### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '' for key 'customer.uk_code' ; Duplicate entry '' for key 'customer.uk_code'
，修改以上bug，并说明bug形成原因

第9次修改
1.回收站取消恢复功能，我已手动更改弹窗，将恢复等前后端逻辑取消掉，将回收站名字更改为已作废列表，


第10次修改
1.已作废列表，每次作废操作应该添加精确到秒的作废时间，以及有些作废时是需要填写作废原因的，但是并没有显示作废原因，该显示的需要显示一下，
2.为保证系统基础资料标识的唯一性、规范性与可维护性，本系统对仓库、商品、供应商、客户等核心基础资料，采用统一的自动编码规则，格式为：
模块前缀 + 日期（yyyyMMdd） + 3位流水号
模块前缀
为不同类型的基础资料设置固定前缀，便于快速识别与区分：
仓库：WH
商品：GD
供应商：SP
客户：CT
日期部分
采用生成编码当日的日期，格式为 yyyyMMdd，例如 20260429，便于追溯资料创建时间。
3 位流水号
每日按模块从 001 开始递增，不足 3 位时用前导零补全（如 001、010、100），确保同一天内同类型资料的编码唯一，格式统一、长度固定。
生成与校验规则
编码由后端自动生成，不允许用户手动输入，避免空值、重复或格式不规范问题。
数据库层面为编码字段设置 NOT NULL 与 UNIQUE 约束，保证非空与全局唯一，防止数据异常。
编码生成时，系统会查询当前模块当日的最大流水号并自增，确保无冲突。







第11次修改
1.商品分类无作废时间
2.新建入库单以及新建出库单添加创建单据时间，这个时间后台自动设置，不可人工填写，进入新建入库单和新建出库单时，入库日期自动校准为当天日期，只能选择当天或者过去日期，不可设置未来日期
3.库存列表，增加仓库最新变动时间，仓库创建时间，
4.库存盘点系统添加盘点单据创建时间这个时间不可人工修改，由后端生成实际时间，以及新建盘点单时增加盘点日期，默认当天日期，仅能选择当天或者过去日期，不可选择未来日期
5，库存调拨系统，添加调拨单创建时间，不可人工修改，由后端生成实际时间，在新建调拨单添加选择调拨日期，默认当天日期，仅能选择当天或者过去日期，不可选择未来日期
6，员工管理，添加账号添加时间
7









小程序