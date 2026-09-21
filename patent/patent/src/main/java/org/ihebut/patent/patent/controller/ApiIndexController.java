package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiIndexController {
    @GetMapping({"", "/"})
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(Map.of(
                "message", "API服务正常",
                "suggest", "请访问 /api/ai/** 或 /api/patents/** 等接口"
        ));
    }
}

