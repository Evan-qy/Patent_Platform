package org.ihebut.patent.patent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 风能专利表实体：patent_wind。
 */
@Entity
@Table(name = "patent_wind")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatentWind extends PatentBase {
}
