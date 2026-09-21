package org.ihebut.patent.patent.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "#{@patentIndexName}")
public class PatentSearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Long, name = "dataset_id")
    @JsonProperty("dataset_id")
    private Long datasetId;

    @Field(type = FieldType.Text, name = "dataset_name")
    @JsonProperty("dataset_name")
    private String datasetName;

    @Field(type = FieldType.Keyword, name = "record_id")
    @JsonProperty("record_id")
    private String recordId;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword, name = "public_num")
    @JsonProperty("public_num")
    private String publicNum;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text, name = "abstract")
    @JsonProperty("abstract")
    private String abstractText;

    @Field(type = FieldType.Text)
    private String applicant;

    @Field(type = FieldType.Text)
    private String inventor;

    @Field(type = FieldType.Text)
    private String ipc;

    @Field(type = FieldType.Text)
    private String cpc;

    @Field(type = FieldType.Text)
    private String nec;

    @Field(type = FieldType.Keyword, name = "appli_date")
    @JsonProperty("appli_date")
    private String appliDate;

    @Field(type = FieldType.Keyword, name = "public_date")
    @JsonProperty("public_date")
    private String publicDate;

    @Field(type = FieldType.Text, name = "legal_status")
    @JsonProperty("legal_status")
    private String legalStatus;

    @Field(type = FieldType.Text)
    private String status;

    @Field(type = FieldType.Text, name = "patent_details")
    @JsonProperty("patent_details")
    private String patentDetails;

    @Field(type = FieldType.Text, name = "all_text")
    @JsonProperty("all_text")
    private String allText;
}
