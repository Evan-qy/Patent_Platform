ALTER TABLE patent_valuation_report
    ADD COLUMN dataset_id BIGINT NULL,
    ADD COLUMN record_id VARCHAR(128) NULL;

CREATE INDEX idx_patent_valuation_report_dataset_record
    ON patent_valuation_report (patent_source, dataset_id, record_id);
