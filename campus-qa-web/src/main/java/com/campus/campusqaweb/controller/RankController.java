package com.campus.campusqaweb.controller;

import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.vo.AnswererRankVO;
import com.campus.campusqapojo.vo.HotQuestionVO;
import com.campus.campusqaservice.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ClassName: RankController
 * Description: 热榜接口
 * Author: SuperXia
 * Datetime :2026/9/24
 * Version:1.0
 */
@RestController
@Tag(name = "热榜接口")
@RequestMapping("/rank")
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    /**
     * 手动触发全量重算（调试用）
     * TODO 上线前删除，或改成 @RequireAdmin —— 定时任务接管后这个接口不该再对外暴露
     */
    @PostMapping("/refresh")
    @Operation(summary = "手动重算热榜（调试用）")
    @RequireLogin
    public Result<Void> refresh() {
        rankService.refreshHotRank();
        return Result.success();
    }

    @GetMapping("/hot")
    @Operation(summary = "热门问题榜")
    public Result<List<HotQuestionVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(rankService.getHotRank(limit));
    }

    /**
     * 手动触发优秀回答者榜重算（调试用）
     * TODO 上线前删除，或改成 @RequireAdmin
     */
    @PostMapping("/answerer/refresh")
    @Operation(summary = "手动重算优秀回答者榜（调试用）")
    @RequireLogin
    public Result<Void> refreshAnswerer() {
        rankService.refreshAnswererRank();
        return Result.success();
    }

    @GetMapping("/answerer")
    @Operation(summary = "优秀回答者榜")
    public Result<List<AnswererRankVO>> answerer(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(rankService.getAnswererRank(limit));
    }
}
