package com.campus.campusqaweb.controller.user;

import com.campus.campusqacommon.annotation.RequireAdmin;
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
     * 手动触发全量重算（运营/调试用）
     * 鉴权：仅管理员。普通用户能看到榜单，但不能触发重算（重算是高成本全表扫描操作，
     * 一旦匿名可调，被恶意刷接口会把 DB 打满——"可见"与"可操作"必须分级）
     */
    @PostMapping("/refresh")
    @Operation(summary = "手动重算热榜（管理员）")
    @RequireAdmin
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
     * 手动触发优秀回答者榜重算（运营/调试用），仅管理员，理由同 refresh
     */
    @PostMapping("/answerer/refresh")
    @Operation(summary = "手动重算优秀回答者榜（管理员）")
    @RequireAdmin
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
