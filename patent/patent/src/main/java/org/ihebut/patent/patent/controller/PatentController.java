package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.PatentUpsertRequest;
import org.ihebut.patent.patent.entity.PatentBase;
import org.ihebut.patent.patent.service.PatentTableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 专利相关接口。
 *
 * <p>对 wind/solar/biomass/hydrogen/lilon 五张专利表提供统一访问。</p>
 */
@RestController
@RequestMapping("/api/patents")
public class PatentController {
    private final PatentTableService patentTableService;

    /**
     * 构造器注入。
     *
     * @param patentTableService 多表专利服务
     */
    public PatentController(PatentTableService patentTableService) {
        this.patentTableService = patentTableService;
    }

    /**
     * 查询专利。
     *
     * <p>按 category 指定表进行查询；query 为空时返回全部。</p>
     *
     * @param category 表类别：wind/solar/biomass/hydrogen/lilon
     * @param query 查询关键字
     * @return 专利列表
     */
    @GetMapping
    public ApiResponse<Page<?>> searchPatents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (category == null || category.isEmpty()) {
            return ApiResponse.fail("请指定专利类别 (category), 例如: wind, solar, biomass, hydrogen, lilon");
        }
        if (query == null || query.isEmpty()) {
            return ApiResponse.ok(patentTableService.list(category, PageRequest.of(page, size)));
        }
        return ApiResponse.ok(patentTableService.search(category, query, PageRequest.of(page, size)));
    }

    /**
     * 创建专利。
     *
     * @param category 表类别：wind/solar/biomass/hydrogen/lilon
     * @param request 专利信息
     * @return 保存后的专利
     */
    @PostMapping
    public ApiResponse<PatentBase> createPatent(@RequestParam(required = false) String category, @RequestBody PatentUpsertRequest request) {
        if (category == null || category.isEmpty()) {
            return ApiResponse.fail("请指定专利类别 (category), 例如: wind, solar, biomass, hydrogen, lilon");
        }
        return ApiResponse.ok(patentTableService.save(category, request));
    }

    /**
     * 获取单条专利记录。
     *
     * @param category 表类别：wind/solar/biomass/hydrogen/lilon
     * @param publicNum 公开号
     * @return 专利（不存在则为 null）
     */
    @GetMapping("/{category}/{publicNum}")
    public ApiResponse<PatentBase> getPatent(@PathVariable String category, @PathVariable String publicNum) {
        return ApiResponse.ok(patentTableService.get(category, publicNum));
    }
}
