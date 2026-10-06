package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehXferBill;
import com.fc.v2.model.auto.TVehXferRoster;
import com.fc.v2.model.custom.VehXferAuditBoard;
import com.fc.v2.model.custom.VehXferBoard;
import com.fc.v2.model.custom.VehXferOpenForm;
import com.fc.v2.model.custom.VehXferQuery;
import com.fc.v2.model.custom.VehXferSignForm;
import com.fc.v2.model.custom.VehXferSignReceipt;
import com.fc.v2.service.ITVehXferBillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 移交执法核签单（系统盯办一套）Controller（控制器只递话，不算一个数）。
 *
 * 这一节点算不算齐、下一节点开不开、被压回来时清哪一段、点齐之后还收不收笔——
 * 四问只认服务层 {@link ITVehXferBillService} 与 VehXferRuler 一处给的答案，此处不另答。
 * 递单带进来的节点号只当口信，与系统数出来的对不上时不作调和，只认服务层回来的那一份。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Api(value = "移交执法核签单（系统盯办）")
@Controller
@RequestMapping("/vehXferBill")
public class VehXferBillController extends BaseController {

    private final String prefix = "admin/vehXferBill";

    /** 墙钟时刻写法（与落库同一套说法，避开 JDBC 时区错位） */
    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private ITVehXferBillService vehXferBillService;

    @ApiOperation(value = "核签台账跳转", notes = "核签台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("vehXferBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "核签单台账", action = "board")
    @ApiOperation(value = "台账开屏",
            notes = "屏头在核张数（立案加一、办讫减一）与逐节点张数、名单同一条 SQL 同一回点算；"
                    + "每单回挂当段应落/已落与还收不收笔，全照服务层那一把尺")
    @GetMapping("/list")
    @RequiresPermissions("vehXferBill:list")
    @ResponseBody
    public ResultTable list(VehXferQuery query) {
        VehXferBoard board = vehXferBillService.openBoard(query);
        ResultTable table = ResultTable.pageTable(board.getTotalCount(), board.getBills());
        // 屏头两个数照同一回点算的结果摆，接口层不另点一遍
        table.setHandleTotal(board.getTotalCount());
        table.setHandleActive(board.getActiveCount());
        return table;
    }

    @ApiOperation(value = "单卷回读", notes = "三段逐段回显：应落几人、算齐口径、已落几人、各笔写了什么，全照系统点出来的挂")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("vehXferBill:list")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        TVehXferBill r = vehXferBillService.selectTVehXferBillById(id);
        return r == null ? AjaxResult.error("查无这张核签单") : AjaxResult.success(200, "卷情回读", r);
    }

    @Log(title = "核签单立案", action = "open")
    @ApiOperation(value = "立案", notes = "表单照现行那张，不另起式样；单代号系统排，从头一段第一轮起")
    @PostMapping("/open")
    @RequiresPermissions("vehXferBill:open")
    @ResponseBody
    public AjaxResult open(VehXferOpenForm form) {
        return AjaxResult.success(200, "核签单已立", vehXferBillService.openBill(form));
    }

    @Log(title = "核签单递单落笔", action = "sign")
    @ApiOperation(value = "递单落笔",
            notes = "口信节点号只当来意，对不上那一笔当它没落；重复递交按幂等；"
                    + "落够人数按当段口径结算：过、压回（不予移交）或挂待定")
    @PostMapping("/sign")
    @RequiresPermissions("vehXferBill:sign")
    @ResponseBody
    public AjaxResult sign(VehXferSignForm form, String at) {
        VehXferSignReceipt rc = vehXferBillService.sign(form, parseWallClock(at));
        return AjaxResult.success(200, rc.getMark() == null ? "落笔收口" : rc.getMark(), rc);
    }

    @Log(title = "核签单不予移交", action = "pushBack")
    @ApiOperation(value = "不予移交（收只收一格）",
            notes = "本段本轮落笔当场抹去、单子压回一格，抹笔与退档同一事务；更早各段旧笔原样存底")
    @PostMapping("/pushBack")
    @RequiresPermissions("vehXferBill:pushBack")
    @ResponseBody
    public AjaxResult pushBack(Long id, String comment) {
        return toAjax(vehXferBillService.pushBack(id, comment) != null ? 1 : 0);
    }

    @Log(title = "核签单驳回", action = "reject")
    @ApiOperation(value = "驳回", notes = "全单就此止住，后面各段不再收笔；已封卷的驳不动")
    @PostMapping("/reject")
    @RequiresPermissions("vehXferBill:reject")
    @ResponseBody
    public AjaxResult reject(Long id, String comment) {
        return toAjax(vehXferBillService.reject(id, comment) != null ? 1 : 0);
    }

    @Log(title = "核签单改备注", action = "updateContent")
    @ApiOperation(value = "改备注", notes = "封卷后改不了一个字")
    @PostMapping("/updateContent")
    @RequiresPermissions("vehXferBill:update")
    @ResponseBody
    public AjaxResult updateContent(Long id, String remark) {
        return toAjax(vehXferBillService.updateContent(id, remark) ? 1 : 0);
    }

    @Log(title = "核签单撤单", action = "remove")
    @ApiOperation(value = "撤单", notes = "封卷后整张抽不掉；只有在核的才撤得掉")
    @PostMapping("/remove")
    @RequiresPermissions("vehXferBill:remove")
    @ResponseBody
    public AjaxResult remove(Long id) {
        return toAjax(vehXferBillService.remove(id) ? 1 : 0);
    }

    @ApiOperation(value = "移交名册", notes = "办讫一张进一行，终审结论与三段笔数快照同源一次回算，不许手填")
    @GetMapping("/roster")
    @RequiresPermissions("vehXferBill:list")
    @ResponseBody
    public AjaxResult roster(String billNo, String day) {
        List<TVehXferRoster> rows = vehXferBillService.listRoster(billNo, day);
        return AjaxResult.success(200, "移交名册", rows);
    }

    @ApiOperation(value = "事后翻查",
            notes = "从第三段一节点一节点数回第一段，笔数与名册一笔不差才算对得上；对不上指名道姓")
    @GetMapping("/audit")
    @RequiresPermissions("vehXferBill:list")
    @ResponseBody
    public AjaxResult audit(String day) {
        VehXferAuditBoard board = vehXferBillService.audit(day);
        return AjaxResult.success(200, "事后翻查", board);
    }

    /** 墙钟字符串解析：只按落库同一套墙钟说法解，不交给 JDBC 绑参 */
    private Date parseWallClock(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_TS);
        sdf.setLenient(false);
        try {
            return sdf.parse(text.trim());
        } catch (ParseException e) {
            return null;
        }
    }
}
