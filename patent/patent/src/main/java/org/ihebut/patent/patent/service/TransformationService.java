package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.entity.TransformationResult;
import org.ihebut.patent.patent.mapper.TransformationResultMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 专利转化成果服务。
 *
 * <p>负责成果记录的新增与查询。</p>
 */
@Service
public class TransformationService {
    private final TransformationResultMapper transformationResultMapper;

    /**
     * 构造器注入。
     *
     * @param transformationResultMapper 数据访问层
     */
    public TransformationService(TransformationResultMapper transformationResultMapper) {
        this.transformationResultMapper = transformationResultMapper;
    }

    /**
     * 查询全部转化成果记录。
     *
     * @return 成果列表
     */
    public List<TransformationResult> getAllResults() {
        return transformationResultMapper.findAll();
    }

    /**
     * 保存转化成果记录。
     *
     * @param result 成果信息
     * @return 保存后的成果
     */
    public TransformationResult saveResult(TransformationResult result) {
        return transformationResultMapper.save(result);
    }
}
