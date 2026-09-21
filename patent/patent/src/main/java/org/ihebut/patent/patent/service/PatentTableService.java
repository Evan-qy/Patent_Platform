package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.dto.PatentUpsertRequest;
import org.ihebut.patent.patent.entity.PatentBase;
import org.ihebut.patent.patent.entity.PatentBiomass;
import org.ihebut.patent.patent.entity.PatentHydrogen;
import org.ihebut.patent.patent.entity.PatentLilon;
import org.ihebut.patent.patent.entity.PatentSolar;
import org.ihebut.patent.patent.entity.PatentWind;
import org.ihebut.patent.patent.mapper.PatentBiomassMapper;
import org.ihebut.patent.patent.mapper.PatentHydrogenMapper;
import org.ihebut.patent.patent.mapper.PatentLilonMapper;
import org.ihebut.patent.patent.mapper.PatentSolarMapper;
import org.ihebut.patent.patent.mapper.PatentWindMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PatentTableService {
    private final PatentWindMapper patentWindMapper;
    private final PatentSolarMapper patentSolarMapper;
    private final PatentBiomassMapper patentBiomassMapper;
    private final PatentHydrogenMapper patentHydrogenMapper;
    private final PatentLilonMapper patentLilonMapper;

    public PatentTableService(
            PatentWindMapper patentWindMapper,
            PatentSolarMapper patentSolarMapper,
            PatentBiomassMapper patentBiomassMapper,
            PatentHydrogenMapper patentHydrogenMapper,
            PatentLilonMapper patentLilonMapper
    ) {
        this.patentWindMapper = patentWindMapper;
        this.patentSolarMapper = patentSolarMapper;
        this.patentBiomassMapper = patentBiomassMapper;
        this.patentHydrogenMapper = patentHydrogenMapper;
        this.patentLilonMapper = patentLilonMapper;
    }

    private static final String VALID_CATEGORIES = "wind, solar, biomass, hydrogen, lilon";

    public boolean isSupportedCategory(String category) {
        return switch (normalizeCategory(category)) {
            case "wind", "solar", "biomass", "hydrogen", "lilon" -> true;
            default -> false;
        };
    }

    private RuntimeException invalidCategory(String category) {
        return new ResponseStatusException(
                BAD_REQUEST,
                String.format("不支持的category: '%s'. 有效值: [%s]", category, VALID_CATEGORIES)
        );
    }

    public Page<?> list(String category, Pageable pageable) {
        return switch (normalizeCategory(category)) {
            case "wind" -> patentWindMapper.findAll(pageable);
            case "solar" -> patentSolarMapper.findAll(pageable);
            case "biomass" -> patentBiomassMapper.findAll(pageable);
            case "hydrogen" -> patentHydrogenMapper.findAll(pageable);
            case "lilon" -> patentLilonMapper.findAll(pageable);
            default -> throw invalidCategory(category);
        };
    }

    public List<?> list(String category) {
        return list(category, Pageable.unpaged()).getContent();
    }

    public Page<?> search(String category, String query, Pageable pageable) {
        String q = query == null ? "" : query;
        return switch (normalizeCategory(category)) {
            case "wind" -> patentWindMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q, pageable);
            case "solar" -> patentSolarMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q, pageable);
            case "biomass" -> patentBiomassMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q, pageable);
            case "hydrogen" -> patentHydrogenMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q, pageable);
            case "lilon" -> patentLilonMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q, pageable);
            default -> throw invalidCategory(category);
        };
    }

    public List<?> search(String category, String query) {
        String q = query == null ? "" : query;
        return switch (normalizeCategory(category)) {
            case "wind" -> patentWindMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q);
            case "solar" -> patentSolarMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q);
            case "biomass" -> patentBiomassMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q);
            case "hydrogen" -> patentHydrogenMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q);
            case "lilon" -> patentLilonMapper
                    .findByPublicNumContainingOrTitleContainingOrAbstractTextContainingOrApplicantContainingOrInventorContaining(q, q, q, q, q);
            default -> throw invalidCategory(category);
        };
    }

    public PatentBase get(String category, String publicNum) {
        return switch (normalizeCategory(category)) {
            case "wind" -> patentWindMapper.findById(publicNum).orElse(null);
            case "solar" -> patentSolarMapper.findById(publicNum).orElse(null);
            case "biomass" -> patentBiomassMapper.findById(publicNum).orElse(null);
            case "hydrogen" -> patentHydrogenMapper.findById(publicNum).orElse(null);
            case "lilon" -> patentLilonMapper.findById(publicNum).orElse(null);
            default -> throw invalidCategory(category);
        };
    }

    public PatentBase save(String category, PatentUpsertRequest request) {
        if (request == null || request.getPublicNum() == null || request.getPublicNum().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "publicNum不能为空");
        }

        PatentBase entity = switch (normalizeCategory(category)) {
            case "wind" -> new PatentWind();
            case "solar" -> new PatentSolar();
            case "biomass" -> new PatentBiomass();
            case "hydrogen" -> new PatentHydrogen();
            case "lilon" -> new PatentLilon();
            default -> throw invalidCategory(category);
        };

        entity.setPublicNum(request.getPublicNum());
        entity.setLegalStatus(request.getLegalStatus());
        entity.setLatestLegalStatus(request.getLatestLegalStatus());
        entity.setStatus(request.getStatus());
        entity.setTitle(request.getTitle());
        entity.setType(request.getType());
        entity.setAbstractText(request.getAbstractText());
        entity.setAppliNum(request.getAppliNum());
        entity.setAppliDate(request.getAppliDate());
        entity.setPublicDate(request.getPublicDate());
        entity.setApplicant(request.getApplicant());
        entity.setApplicantAddress(request.getApplicantAddress());
        entity.setPatentee(request.getPatentee());
        entity.setPatenteeAddress(request.getPatenteeAddress());
        entity.setInventor(request.getInventor());
        entity.setAgent(request.getAgent());
        entity.setIpc(request.getIpc());
        entity.setCpc(request.getCpc());
        entity.setNec(request.getNec());
        entity.setPatentDetails(request.getPatentDetails());

        return switch (normalizeCategory(category)) {
            case "wind" -> patentWindMapper.save((PatentWind) entity);
            case "solar" -> patentSolarMapper.save((PatentSolar) entity);
            case "biomass" -> patentBiomassMapper.save((PatentBiomass) entity);
            case "hydrogen" -> patentHydrogenMapper.save((PatentHydrogen) entity);
            case "lilon" -> patentLilonMapper.save((PatentLilon) entity);
            default -> throw invalidCategory(category);
        };
    }

    private String normalizeCategory(String category) {
        return category == null ? "" : category.trim().toLowerCase();
    }
}
