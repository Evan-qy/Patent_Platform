package org.ihebut.patent.patent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 光伏专利表实体：patent_solar。
 */
@Entity
@Table(name = "patent_solar")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatentSolar extends PatentBase {
}
