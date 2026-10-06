# veh 域口径交接册

接手这一片只看这一份：每样事归哪一处作答、状态怎么数、旧账翻哪一本，都在下面。
改口径时只改"唯一入口"那一列指到的方法，屏上/控制器里不许另抄第二套。

库：`chen_023`，建表脚本 `doc/schema/veh.sql`（列名与 `model/auto` 实体逐列对齐，改列两边一起改）。

## 1. 运行界次判定线（判档）

- 表：`t_veh_run_line`（界册）；`t_veh_run_line_verdict`（裁决留痕，判完即归档在册）。
- 唯一裁决口：`TVehRunLineServiceImpl.verdict(measureKind, rawValue, at)`，HTTP 一道门 `GET /vehRunLine/verdict`。
  折算、请界、说话次序、归层、归档全在这一处，控制器只递话。
- 取数折算（只往界册那一头折，取数比数同一把 HALF_UP、同到三位小数）：
  - 车速：车上报 m/s ×3.6 → km/h；
  - 连续驾驶时长：车上报分钟 ÷60 → 小时。
- 量程门（在请界之前）：车速 0..100 m/s、时长 0..1440 分钟；越界（含负值）判出档位，
  结论写"超出量程"，层与代号留空，不许塞进立案层。
- 在位口径（起用含当日、交班不含当日；交班栏空着的存量界认作一直在位；
  已停用、分界数缺格/没递增的不参与）：`isEffective` 一处，
  裁决、`/available` 名单、旧办法 `usable/listAvailable/countAvailable`、屏挂核对同用它。
- 说话次序：`priority` 大者先说话；相同再比 `rule_code` 字典序，落在末位的胜出（确定性，不许两次给不同档）。
- 归层（等号压线归本层，新旧两路同一个说法）：
  `[th1,th2)` 提示=1、`[th2,th3)` 严管=2、`[th3,…)` 立案=3；没到 th1 给"未到起算"、层留空；
  那一刻一条在位界都没有给"无可用界"，代号与起用日两栏留空。
- 补判旧账：`at` 传旧账自己的那一刻，请的是那一刻在位的界，不许拿最新界顶替。
- 归档：每次 `verdict` 都把说话界的代号、界名、起用日、th1/th2/th3、单位、次序、结论、折后数
  快照写入 `t_veh_run_line_verdict`。**日后追溯以归档这一份为准**，界册后来改了也不重算旧账。
  翻阅：`GET /vehRunLine/archive`（`listVerdictArchive`，可按类别/问话时刻区间/代号）。
- 旧办法 `evaluate/evaluateTop`：名目参数表照旧，输入按界册单位直收（不折算），
  但请界、在位、量程、压线归层已并到同一套口径，返回 1..3，没到起算/越界/无在位界返 0。
- 界册翻检（含停用、已交班，只许看不许拿去比数）：`browseRegister` / `GET /vehRunLine/browse`。
- 界册体检（缺哪格、哪格没越过前一格，指名道姓）：`inspectRegister` / `GET /vehRunLine/inspect`。
- 屏挂悬空代号核对：`danglingScreenCodes` / `GET /vehRunLine/dangling`。

## 2. 严重报警处置单（状态机）

- 表：`t_veh_handle_flow`（卷）+ `t_veh_handle_note`（一段一段的按语）；
  服务 `TVehHandleFlowServiceImpl`；唯一一把尺 `VehHandleRuler`（纯算，不碰库）。
- 四站 `stage` 0定性 1知会 2回告 3归档；情形 `status` 0未起 1在办 2已封尾。
  新单立单即在办（0未起只留给建了没动过的存量旧账）。
- **四问只认一处**：这一张走到哪一站、下一站开不开、收口后再递接不接、同站第二遍另不另起行——
  册面（`/list` 台账、`/detail` 回读）与报文收口两边同一句答案，全出 `VehHandleRuler` + 服务，
  控制器只递话，别处不许另答、不许事先替卷把站次定好。
- **步次只有一个来处**：系统数已经过的站数（`stage` 一栏）。报文里自报的 `claimStage` 只当来意，
  落在回告段的 `claim_stage` 与卷上 `reply_claim_stage` 留痕；与系统站次不一致时，
  入库按系统数的记、回读照系统数的回（`POST /vehHandleFlow/reply`，回执 `VehHandleReplyReceipt`
  把当站、来意、一不一致、开不开全回挂）。去年秋天报文推着走两站的旧路已堵死。
- **立单（定性站）四样齐才起**（`POST /open`，`VehHandleOpenForm`）：车辆营运代号 `veh_code`、
  报警发生那一刻 `alarm_at`（墙钟 yyyy-MM-dd HH:mm:ss）、当刻那个数（`measure_kind`=speed/duration
  配 `alarm_value`，车速 km/h、连续驾驶时长小时）、报警类别 `alarm_kind`；缺一样抛
  `VehHandleRejectException`（点明哪一格）。单号系统排（CL+雪花），卷上自带号不认。
- **同车同类只容一张没收口的卷**：`veh_code+alarm_kind` 已有非封尾卷（在办或半道止住，含存量未起），
  第二张起不来。
- 往前走只留一处口子 `moveTo`：一次只挪一档，跳站不作数。各站守门（`VehHandleRuler.nextBlockReason`）：
  - 知会站：看知会几笔全没全（`notify_required` 当地定，缺省 1；`notify_count` 落一笔加一）。
    笔记走 `POST /notify`；**笔记只记数不挪步**——末笔齐了卷也停在知会站，进回告站只由 advance 开；
    已齐再递不另起新行、不抛错。
  - 回告站：守当地定的钟点 `reply_wait_hours`——从**进回告站那一刻** `notify_done_at` 起数，
    钟点没走满（压线：正满那一刻算满，墙钟字符串比），企业回话递得再齐 advance 也当没开；
    且回话（`reply_count>0`）本身得到。
  - 归档站：只等复查意见 `review_opinion` 落下（`POST /review`），落下即凭 `REVIEW` 入档收口。
- **回告报文**：只在系统数出来的回告站接头一遍（落 REPLY 段、`reply_count=1`）；
  不在回告站（报文说自己到了后面几站也没用）、查无此单、收口后的，一律不接、卷上一格不动
  （被拒的回话连段也不留）；同站第二遍 accepted 但 `newLine=false`，卷上还是头一遍那行字。
- **收口两说**（任何在办站都收得，`close_kind` 钉凭据）：
  `OUTSIDE` 不归运输机构督办（`/closeOutside`）；`DEREGISTERED` 业户已销户
  （`/closeDeregistered`，名录 `t_veh_ent.status=1` 核实，没落代号/查无/在册都收不得）。
- **收了口便封住**（`closeFlow` 唯一落法，status=2）：推、退、`updateContent`、`remove`、再接回告全拒，
  修改位置一律锁死、整张撤不掉；季度追查从入档往回数段全凭卷。
- **退**（旧 `/rollback` 模样不改）：一次只退一站；被退站的旧按语一段不抹（留痕压末位），
  重走时该站攒头清零重攒——退回知会：知会几笔/回告几笔/钟点起算点/来意全清；
  退回回告：只清本趟回告，知会那几笔与钟点起算点不动。头一站退无可退返 null。
- **按语段 `t_veh_handle_note`**：每段钉 flow_id、当站（系统数的）、话种
  OPEN/NOTIFY/REPLY/REVIEW/CLOSE/MOVE、话、时刻；回读按 note_at,id 升序挂齐。
  同一秒钟两条来路各记各的行，不合成、不漏记。
- **在办卷数**：起一张加一、收一张减一。值班屏 `GET /list` 一条 SQL
  （`TVehHandleFlowBoardMapper.xml`）：名单分页与屏头数同一快照（全册 `del_flag=0 且 status<>2`
  标量子查询，不随筛选借行），与回库一站一站点出来的本是同一次算；`ResultTable.handleTotal/handleActive`。
- **旧口子模样不改**：`advance(id,remark)`/`rollback(id,remark)` 仍是进一个号、还一张单（拒返 null）；
  `updateContent`/`remove` 返 boolean。日后要换一种走法，另起一套去接，不回头动它。
- 旧库补列见 `doc/schema/veh.sql` 末尾（新列 + 新建 `t_veh_handle_note`）。

## 3. 移交执法核签单（多档签批）

- 表：`t_veh_move_bill`；服务 `TVehMoveBillServiceImpl`。
- 档口 `node_no` 0初核 1复核 2核准；`sign_mode` 0一名即可 1两名同判；
  `need_count` 本档应到人数（空着按 sign_mode 兜底 1/2）；`sign_count` 本档已落印。
- 情形 `status` 0在核 1已办结 2已否决。
- `approve`：**先在本档 sign_count+1，再判够不够应到人数**；够了清零进下一档，过了核准才办结；
  没够档口不动。（早先先 +node 再拿旧值判过档，永远差一档。）
- `reject`：一票否决即终态 status=2（早先只空 update，仍挂"在核"）。
- `rollback`：退上一档、本档落印作废清零、重回在核；初核退无可退返 null。
- 已办结/已否决封档，不再落印。
- **本套是旧走法，模样不改**：按单据取当下节点那一套进什么出什么一概照旧。
  今年起系统从头盯到尾的新走法另起了一套（见 3b），两边各走各的，谁也不动谁。

## 3b. 移交执法核签单（系统盯办一套，新名目）

- 新表：`t_veh_xfer_bill`（卷）、`t_veh_xfer_sign`（落笔留痕）、`t_veh_xfer_roster`（移交名册/受理簿）；
  服务 `TVehXferBillServiceImpl`；唯一一把尺 `VehXferRuler`（纯算，不碰库）；
  HTTP 在 `/vehXferBill` 下，控制器只递话。旧库补建三张表见 veh.sql 末尾。
- 三段 `node_no` 0初审(监督处监控岗) 1复核(市处法规科) 2终审(省中心分管负责人)；
  情形 `status` 0在核 1办讫 2驳回；`round_no` 第几轮落笔（压回一格即加一，新份旧份凭这轮次分开放）。
- **三段规矩互不相同，不许彼此抄**（`VehXferRuler` 一处定死，屏上照此回显）：
  第一段一名即可，那一笔的话就是本段的话；第二段要两名，**取先落笔那位的话为准**，
  后一名照数入账、票次累加，两人各写了什么都回显；第三段**两名同判**——都写可移交才算过，
  都写不予移交即本段判不予移交，一可一不可**挂待定**（不进下一段，也不就此终止，等不予移交/驳回处置）。
- **已落几人不留手填格**：系统顺着 `t_veh_xfer_sign` 按落笔先后（自增主键）一段一段点出来，
  屏上那个数只算回显；点出来的条数与应落人数持平才叫齐，不持平就当没齐。
- **四问只认一处**：这一节点算不算齐、下一节点开不开、被压回来时清哪一段、点齐之后还收不收笔，
  册面（`/list` 台账、`/detail` 回读）与递单回执两边同一句答案，全出 `VehXferRuler` + 服务。
- **段次只有一个来处**：系统数出来的当段（`node_no` 一栏）。递单带进来的 `claimNode` 只当口信，
  对不上不作调和——那一笔当它没落、卷上一格不动，回执只认系统这一段；
  谁也不许在旁处直接把号按下去、把先后重排。重复递交按幂等：同一人同段本轮同话，
  回接住了但不另起行；改口不接，旧笔不改。同一人同段本轮重复落笔由唯一键
  `uk_veh_xfer_sign_once(bill_id,node_no,round_no,signer_no)` 挡，前后脚同时到撞了键按幂等回。
- **走只许进紧邻的下一节点**：本段点齐由 `sign` 一处结算进下一段；跳过中段直递末段的笔不作数。
- **收只收一格**（不予移交）：本节点这一轮已落的笔**当场抹去（硬删，不许留着）**、单子压回一格、
  `round_no` 加一，抹笔与退档落在同一事务里；更早各段的旧笔原样存底，重走从被压回那一段往上办，
  新份与旧份分开放、哪天补的各有凭据。头一段退无可退，抹了原地重走。
  来路两条同走这一处：落笔落出来的（头两段话头是否决、第三段同判不予移交）与明示 `POST /pushBack`。
- **驳回**（`POST /reject`）：全单就此止住 status=2，后面各段不再收笔。
- **办讫即封卷**：第三段两名同判可移交 → status=1 钉 `done_at`；此后加不进一笔、改不了一个字、
  整张也抽不掉（`sign/pushBack/reject/updateContent/remove` 全拒）。
- **名册同源一次回算**：办讫那一事务里，用同一份落笔留痕把终审结论与三段有效笔数（各段最后一轮）
  写进 `t_veh_xfer_roster`——屏上这一处、名册那几处跨模块联动，一处取数，不许手填。
  翻阅 `GET /vehXferBill/roster`（可按单代号/办讫日）。
- **事后翻查** `GET /vehXferBill/audit`：从第三段一节点一节点数回第一段（各段有效轮现点），
  与名册快照一笔一笔对，一笔不差才算对得上；对不上指名道姓摆在明细里。
- **在核张数**：立案加一、办讫（驳回亦同）减一。台账 `GET /vehXferBill/list` 一条 SQL
  （`TVehXferBillBoardMapper.xml`）：名单分页、在核总数、逐节点在核张数、当段本轮已落几人
  同一快照点算，屏上显的数与逐节点点出的数出自同一回计算；`ResultTable.handleTotal/handleActive`。
- **表单照现行那张，不另起式样**：立案只收事由记要（`POST /vehXferBill/open`，`VehXferOpenForm`），
  单代号系统排（HQ+雪花），三段应落几人与算齐口径是系统定的，不在表单上填。

## 4. 证照到期催办（周期任务）

- 表：`t_veh_due_task`；服务 `TVehDueTaskServiceImpl`（旧驱动）。
- `due_at` 应开口止点；`amount` 最可提前几日；`status` 0搁着(待派) 1递出去了(已派出) 2递不动(分不出)（存量空状态按搁着认）。
- **到没到期只有一把尺 `VehDueRuler`（`service/VehDueRuler.java`）**，旧驱动与新批派一趟同用，别处不许另摆：
  - 开口点 = 止点往回点 `amount` 个**办事日**（不是自然日/24 小时）：一日一日回点，
    不办事的整日跳过不计数，止点带几点几分开口点还是几点几分（夜里跨的几个钟头不凑整日）；
  - 不办事日历读 `t_veh_nonwork_day`（`day_kind` 0国家放假 1省里下文停办），经 `VehDueWorkdayCalendar` 逐日剔；
  - 触发时刻已到开口点（**含开口/止点那一刻，压线算到期**）即开口，逾期照催；
  - 时刻读法两处一致：挑桩与回写同走墙钟字符串 `yyyy-MM-dd HH:mm:ss`（避开 JDBC serverTimezone 错位）。
- 旧驱动：开口判定 `isDispatchable` 一处（已改为委托 `VehDueRuler`），`runOnce` 直接照 `listDue`
  那份名单派（两处同源），逐条置"已派出"，空名单/时刻缺省返 0（早先空名单取 `get(0)` 崩），
  单条失败跳过不牵动别条。**旧驱动照旧只出一个数、不添新东西、模样不改**；
  旧驱动从宽：amount 空着/带负号按 0 兜底（`parseAmountLenient`）。

## 4b. 证照到期批派一趟（新名目，一趟只办一批）

- 新表：`t_veh_nonwork_day`（不办事日历）、`t_veh_due_handler`（经办人册，`status` 0在位 1本季休办、
  `quota` 本季定额）、`t_veh_due_handler_kind`（经办人经得来的业户类别，对 `t_veh_ent.site_type`）；
  `t_veh_due_task` 加格：`site_no`/`contact`/`assignee_no`/`dispatched_at`/`block_stage`/`block_reason`
  （旧库补列见 veh.sql 末尾）。
- 服务 `TVehDueDispatchServiceImpl`，**唯一入口** `POST /vehDueDispatch/dispatch?at=yyyy-MM-dd HH:mm:ss`
  （at 缺省按问话当下；控制器只递话，不算一个数）。"要一趟只办一批"的活只由这一名目担，不回头改旧驱动。
- 开口窗口唯一判定 `VehDueRuler.withinWindow`：**[02:00, 05:00)**——两点以前、五点及以后是静默段，
  按下触发只应声 `windowOpen=false`、三数留空（不是 0），不读册、不动库、行上一格也不改；at 缺省同样不动。
- 挑桩（与旧驱动同一把尺）：只挑搁着的（`上趟已递出去/递不动不捞回`），行格先过堂：
  `due_at` 空、`amount` 没数/带负号/大得没边（顶 `MAX_AHEAD_DAYS=3650`）、`item_no` 没落，
  当场置 2 并把卡环（止点时刻/提前日/条目代号）写回 `block_stage`、缘由写 `block_reason`；
  **一桩撑不住不掀翻这一趟**，别桩照旧一桩一桩走完。
- 两类天生递不动（不必等到期、不必空跑）：所对业户在 `t_veh_ent` 已销户(`status=1)`或查无此代号
  （卡环"业户"）；`contact` 空着/空白（卡环"联络人"）。
- 截断：挑中多过 `BATCH_CAP=500` 时，按 `due_at` 先来先办、同止点按 `item_no` 字典序，
  只办头 500，余下留在搁着等下一趟（`leftToNext` 报数、mark 里摆明，不许一口气捞穿整张表）。
- 递到谁名下（`prepareAssignment`/`Assignment.pick`）：按这一季（季头 `VehDueRuler.quarterStart`）
  手上活**最少**的先接（季内已派出条数，含本趟占位；平手按代号排，确定性）；
  本季休办、册里没挂这类业户（经不来）、名下已压着同一 `site_no`（一家业户的几桩不并同一人，
  跨本趟的季内历史也认）、已塞满定额的，逐位顺延；谁都接不下当场置 2，卡环"派单"、缘由写回行上。
  名额在**落库成功后**才 commit 占位；该条写库抛错则不占名额、行留在搁着，不牵动别桩。
- 办过的两边痕迹同进同退：`status` 翻 1 **且** `dispatched_at`=触发那一刻、`assignee_no`=经办人
  同一回 update；只翻词不钉时刻/经办人的，回库点不算办过。
- 收口三数（`VehDueDispatchReceipt`：还欠开口/这一趟递出去/标成递不动）**只报这一趟这一回，
  不并历史**；`recount` 重新回库整册逐条点（按行 id 对本趟、库里确已落成）才出口，
  不拿挑桩时屏上那一遍凑数。还欠开口 = 收口时仍搁着、行格站得住、尺子说还没到开口点
  （超顶留下的到期桩不算欠开口）。一条没挑着 mark="这一趟没有到期的"，寻常收口不当事故。

## 5. 月度报警处置核收册 / 联网清单页（已收口，改动别动其口径）

- 核收册 `t_veh_alarm_batch` + `t_veh_alarm_row`：整册一道门 `importBatch`；
  行结果只许 未勾/收下/退回；回执三数回库现点现取；撤册后册码才让重递；
  空册不发核讫；`archiveStaleRows` 只把隔两个报送年度仍未勾的行挪往期层，往期照旧翻得着。
- 联网清单页 `t_veh_bind_page`：唯一写入判路 `insertTVehBindPage/updateTVehBindPage/saveFromForm`；
  还差几项一律系统按 应联-已联 轧（不手填）；进展随轧差定 新录/已联齐/缺项；
  开屏页头三栏与名单同一条 SQL 同一回点算（`TVehBindPageBoardMapper.xml`）。

## 6. 跑测试

```
mvn -o test
```

不启 Spring、桩掉 Mapper 的单测在 `src/test/java/com/fc/v2/service/`，
需求点过名的坑（压线、同一把舍法、历史窗口、说话次序、停用、悬空代号、状态同源、签批过档、催办窗口、
批派窗口静默段/办事日剔假/废格卡环/销户空联络人/同户相斥/休办定额顺延/双痕迹/回库现点/五百截断、
处置单定性四样/同车同类相斥/紧邻站/知会几笔/回告钟点压线/报文步次只当来意/同站不另起行/同秒两段/
退回清零旧段不抹/收口两说封死只读/台账同算、
核签单三段三规矩/先落笔为准/两名同判/待定不进不终/跳段那笔当它没落/口信不作调和/幂等不另起行/
唯一键挡重笔/抹笔退档旧笔存底/办讫封卷只读/名册同源/翻查一笔不差等）
都有对应用例钉着；改口径先改/加这些用例。处置单尺子的纯算用例在 `VehHandleRulerTest`，
核签单尺子的纯算用例在 `VehXferRulerTest`。
