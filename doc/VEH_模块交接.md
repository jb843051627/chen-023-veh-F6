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

## 3b. 移交执法核签单（三节点新走法，另起的一套）

> 旧 §3 `t_veh_move_bill` 的 approve/reject/rollback **一字未动**；这套新走法另起表、另起服务接着，
> 不回头改它的模样——宁可另起一套，也不去动那一套。

- 表：`t_veh_sign_bill`（卷）+ `t_veh_sign_ink`（落笔流水，不留手填计数格）+
  `t_veh_transfer_register`（移交名册）；服务 `TVehSignBillServiceImpl`；唯一一把尺
  `VehSignRuler`（纯算，不碰库）。
- 三段 `node_no` 0监督处监控岗初审 / 1市处法规科复核 / 2省中心分管负责人终审，**口径互不相同不许抄**
  （`VehSignRuler.SignMode`）：
  - 初审 `FIRST` 应到 1：一名就够，谁先落这一段便算齐；
  - 复核 `FIRST_WINS` 应到 2：取**先落笔**那位（按 ink_at,id）的话为准，后一名照数入账、票次累加，
    两人各写了什么屏上都看得见；
  - 终审 `CONSENSUS` 应到 2：两笔并发前后脚，两人都写 MOVE 才齐；一可一不可 → `PENDING` 挂待定，
    不进下段也不终止、此期间不收笔；两笔都 REJECT → 定论不予移交压回。
- 情形 `status` 0在签 1已办讫 2已驳回 3待定。`round_no` 是本段重走第几轮（新旧份分开放的凭据）。
- **四问只认一处**（`VehSignRuler.evaluate` + 服务）：这一段算不算齐、下段开不开、压回来清哪一段、
  点齐后还收不收笔。别处问不出第二套。
- **已落几人只点不填**：服务顺着落笔先后点 `t_veh_sign_ink` 当轮在册(ink_state=0)条数，
  条数与应到人数持平才叫齐；屏上数字只是这一处点完的回显。回读逐节点挂 `TVehSignNodeView`
  （当轮 current、历轮/被抹 history）。
- 落笔唯一口子 `POST /vehSignBill/sign`（`VehSignForm`→`VehSignReceipt`）：
  - **节点只认系统**：卷上 `stage` 是唯一来处；报文 `claimNode` 只当口信（落 ink.claim_node 留痕），
    对不上不作调和、不旁处按号重排，这一笔当没落，单照旧停在系统那一段（跳过复核径到终审即此）；
    不带口信(null)照落。
  - 走只进紧邻下段（上一段没齐下段笔不作数）；`nextRoundNo` 进/退到某段取该段已有轮次+1。
  - **同人同轮唯一键** `uk_veh_sign_ink_signer(bill_id,node_no,round_no,signer_no)` 挡重复落笔
    （服务先查一道、库唯一键挡并发那道，抛 DuplicateKeyException 回库认头笔）。
  - **幂等** `request_id`：流水 `uk_veh_sign_ink_request` 认同键头一笔，重递 accepted 但 newInk=false；
    卷上只挂最近一回 request_id。
  - **判不予移交只收一格**：`OUT_PRESS_BACK` 时把**本段本轮**在册各笔置 ink_state=1（留痕不物理删），
    同时退段（初审退仍在初审、另起第 2 轮；复核退初审；终审退复核），抹笔与退档同处一个
    `@Transactional`，要么一起成要么一起不成；更早各节点、更早各轮旧笔原样存底。
  - 终审两笔同判 MOVE → `status=1` 办讫、final_decision=MOVE、closed_at 钉封，
    **同一事务**往 `t_veh_transfer_register` 回算一行（`writeRegister`）：屏上终审结论与名册两处
    跨模块同源，不许一处取数一处手填。
- **整单驳回** `POST /vehSignBill/dismiss`（`VehSignDismissForm`，驳回人+缘由必给）：
  与"判不予移交压回"是两件事——驳回即 `status=2` 终态，全单止住、后面各段不再收笔；
  口信节点对不上不落；同 requestId 重递按头一回回。
- **封档只读**：办讫/驳回后加不进一笔、改不了一字、整张抽不掉（无删除/更新口子）。
- **在签张数**：立案 +1、办讫/驳回 -1（待定仍占数）。值班屏 `GET /vehSignBill/list`
  一条 SQL（`TVehSignBillBoardMapper.xml`）：名单分页与屏头数同一快照
  （全册 del_flag=0 且 status IN (0,3) 标量子查询），与逐节点点出的同一回计算；
  `ResultTable.signTotal/signActive`；另有 `GET /activeCount` 回库现点对得齐。
- 事后翻查 `GET /vehSignBill/detail`：从终审逐节点点回初审，笔数与受理簿一笔不差，单抽一段也站得住；
  名册 `GET /vehSignBill/register`。立案 `POST /open`（线索代号 clue_no 必给，单号 HS+雪花，自带号不认）。
- 格错（空落笔人/话非 MOVE·REJECT/无线索）抛 `VehSignRejectException`（点明哪格，全局解析照原话回）；
  查无/封档/待定/跳关/重笔这类"不收"由回执 accepted=false·newInk=false 回话，卷上不动。

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
退回清零旧段不抹/收口两说封死只读/台账同算等、
核签新走法三段口径互异/先落为准/同判与分歧待定/跳关口信不落/同人同轮唯一键/并发唯一键/重递幂等/
判不可只抹本轮同事务旧笔存底/新旧轮分开/驳回终止/办讫封档只读/终审名册同源/在签数同算等）
都有对应用例钉着；改口径先改/加这些用例。处置单尺子的纯算用例在 `VehHandleRulerTest`，
核签新走法尺子的纯算用例在 `VehSignRulerTest`。
