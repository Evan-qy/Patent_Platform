package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.SyncJobCreateRequest;
import org.ihebut.patent.patent.entity.DataSyncJob;
import org.ihebut.patent.patent.mapper.DataSyncJobMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/sync-jobs")
public class SyncJobController {
    private final CurrentUser currentUser;
    private final DataSyncJobMapper dataSyncJobMapper;

    public SyncJobController(CurrentUser currentUser, DataSyncJobMapper dataSyncJobMapper) {
        this.currentUser = currentUser;
        this.dataSyncJobMapper = dataSyncJobMapper;
    }

    @PostMapping
    public ApiResponse<DataSyncJob> create(@RequestBody SyncJobCreateRequest request) {
        long userId = currentUser.requireUserId();
        if (request == null || request.getJobType() == null || request.getJobType().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "jobType不能为空");
        }
        DataSyncJob job = new DataSyncJob();
        job.setJobType(request.getJobType().trim());
        job.setTargetCategory(request.getTargetCategory());
        job.setSource(request.getSource());
        job.setCreatedBy(userId);
        return ApiResponse.ok(dataSyncJobMapper.save(job));
    }

    @GetMapping
    public ApiResponse<List<DataSyncJob>> list(@RequestParam(required = false) Boolean mine) {
        long userId = currentUser.requireUserId();
        if (Boolean.TRUE.equals(mine)) {
            return ApiResponse.ok(dataSyncJobMapper.findByCreatedBy(userId));
        }
        return ApiResponse.ok(dataSyncJobMapper.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<DataSyncJob> get(@PathVariable Long id) {
        DataSyncJob job = dataSyncJobMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "任务不存在"));
        return ApiResponse.ok(job);
    }
}

