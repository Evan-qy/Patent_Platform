package org.ihebut.patent.patent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 氢能专利表实体：patent_hydrogen。
 */
@Entity
@Table(name = "patent_hydrogen")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatentHydrogen extends PatentBase {
}
