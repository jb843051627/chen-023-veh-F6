package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehSignBill;
import com.fc.v2.model.custom.VehSignBoard;
import com.fc.v2.model.custom.VehSignDismissForm;
import com.fc.v2.model.custom.VehSignForm;
import com.fc.v2.model.custom.VehSignOpenForm;
import com.fc.v2.model.custom.VehSignQuery;
import com.fc.v2.model.custom.VehSignReceipt;
import com.fc.v2.service.ITVehSignBillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 核签单（三节点新走法）Controller。哪一关该几个人说话、压回来清哪一段、点齐之后还动得动不动，
 * 只认服务层 {@link ITVehSignBillService} 与 VehSignRuler 一处给的答案，控制器只递话。
 * 旧 /vehMoveBill approve/reject/rollback 那套模样不改，这是另起接着的一套，不回头动它。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Api(value = "核签单三节点签批")
@Controller
@RequestMapping("/vehSignBill")
public class VehSignBillController extends BaseController {

    private final String prefix = "admin/vehSignBill";

    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private ITVehSignBillService vehSignBillService;

    @ApiOperation(value = "核签台账跳转", notes = "核签台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("vehSignBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "核签单值班台账", action = "board")
    @ApiOperation(value = "值班台账开屏",
            notes = "屏头在签张数与名单同一条 SQL 同一回点算；每条逐节点点条数回挂，齐不齐只问尺子")
    @GetMapping("/list")
    @RequiresPermissions("vehSignBill:list")
    @ResponseBody
    public ResultTable list(VehSignQuery query) {
        VehSignBoard board = vehSignBillService.openBoard(query);
        ResultTable table = ResultTable.pageTable(board.getTotalCount() == null ? 0 : board.getTotalCount(),
                board.getBills());
        // 屏头两个数照同一回点算的结果摆，接口层不另点一遍
        table.setSignTotal(board.getTotalCount());
        table.setSignActive(board.getActiveCount());
        return table;
    }

    @ApiOperation(value = "单情回读", notes = "三段逐节点点条数挂齐；当轮旧轮分开，被抹的旧笔原样在 history")
    @GetMapping("/detail")
    @RequiresPermissions("vehSignBill:list")
    @ResponseBody
    public AjaxResult detail(String billNo) {
        TVehSignBill b = vehSignBillService.detail(billNo);
        return b == null ? AjaxResult.error("查无这张核签单") : AjaxResult.success(200, "单情回读", b);
    }

    @Log(title = "核签单立案", action = "open")
    @ApiOperation(value = "立案", notes = "督办线索代号必给；单号系统排 HS+雪花；立案即在签占数")
    @PostMapping("/open")
    @RequiresPermissions("vehSignBill:open")
    @ResponseBody
    public AjaxResult open(VehSignOpenForm form) {
        return AjaxResult.success(200, "核签单已立", vehSignBillService.openBill(form));
    }

    @Log(title = "核签单落笔", action = "sign")
    @ApiOperation(value = "落一笔",
            notes = "口信节点与系统当段对不上这笔当没落；同人同节点同轮唯一键挡；重递幂等；"
                    + "判不予移交只抹本轮笔并压回，旧笔存底；终审同判可移交即办讫并同次回算落名册")
    @PostMapping("/sign")
    @RequiresPermissions("vehSignBill:sign")
    @ResponseBody
    public AjaxResult sign(VehSignForm form) {
        VehSignReceipt rc = vehSignBillService.sign(form);
        return AjaxResult.success(200, rc.getMark() == null ? "落笔收口" : rc.getMark(), rc);
    }

    @Log(title = "核签单整单驳回", action = "dismiss")
    @ApiOperation(value = "整单驳回", notes = "某一节点驳回，全单止住，后面各节点不再收笔；口信节点对不上不落")
    @PostMapping("/dismiss")
    @RequiresPermissions("vehSignBill:dismiss")
    @ResponseBody
    public AjaxResult dismiss(VehSignDismissForm form) {
        VehSignReceipt rc = vehSignBillService.dismiss(form);
        return AjaxResult.success(200, rc.getMark() == null ? "驳回收口" : rc.getMark(), rc);
    }

    @ApiOperation(value = "移交名册翻查", notes = "每一行都来自终审同判可移交那一次同事务回算")
    @GetMapping("/register")
    @RequiresPermissions("vehSignBill:list")
    @ResponseBody
    public AjaxResult register() {
        return AjaxResult.success(200, "移交名册", vehSignBillService.listRegister());
    }

    @ApiOperation(value = "在签张数回库现点", notes = "立案加一、办讫/驳回减一；与台账屏头同口径，对得齐")
    @GetMapping("/activeCount")
    @RequiresPermissions("vehSignBill:list")
    @ResponseBody
    public AjaxResult activeCount() {
        return AjaxResult.success(200, "在签张数", vehSignBillService.countActive());
    }
}
