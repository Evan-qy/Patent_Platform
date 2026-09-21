package org.ihebut.patent.patent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 锂电专利表实体：patent_lilon。
 */
@Entity
@Table(name = "patent_lilon")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatentLilon extends PatentBase {
}
