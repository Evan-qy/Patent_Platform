package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.PatentValuationModelParam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatentValuationModelParamMapper extends JpaRepository<PatentValuationModelParam, Long> {
    Optional<PatentValuationModelParam> findByParamKey(String paramKey);
}

