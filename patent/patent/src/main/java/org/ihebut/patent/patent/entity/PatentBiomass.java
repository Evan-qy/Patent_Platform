package org.ihebut.patent.patent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生物质专利表实体：patent_biomass。
 */
@Entity
@Table(name = "patent_biomass")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatentBiomass extends PatentBase {
}
