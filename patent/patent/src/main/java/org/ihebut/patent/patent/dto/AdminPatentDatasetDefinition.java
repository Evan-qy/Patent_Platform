package org.ihebut.patent.patent.dto;

public class AdminPatentDatasetDefinition {
    private Long id;
    private Long dataSourceId;
    private String code;
    private String name;
    private String category;
    private String tableName;
    private String primaryKeyColumn;
    private String publicNumColumn;
    private String titleColumn;
    private String abstractColumn;
    private String applicantColumn;
    private String inventorColumn;
    private String ipcColumn;
    private String cpcColumn;
    private String appliDateColumn;
    private String publicDateColumn;
    private String legalStatusColumn;
    private String statusColumn;
    private String defaultSortColumn;
    private String defaultSortDirection;
    private String searchFieldsJson;
    private boolean enabled;
    private boolean builtIn;
    private String remarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getPrimaryKeyColumn() { return primaryKeyColumn; }
    public void setPrimaryKeyColumn(String primaryKeyColumn) { this.primaryKeyColumn = primaryKeyColumn; }
    public String getPublicNumColumn() { return publicNumColumn; }
    public void setPublicNumColumn(String publicNumColumn) { this.publicNumColumn = publicNumColumn; }
    public String getTitleColumn() { return titleColumn; }
    public void setTitleColumn(String titleColumn) { this.titleColumn = titleColumn; }
    public String getAbstractColumn() { return abstractColumn; }
    public void setAbstractColumn(String abstractColumn) { this.abstractColumn = abstractColumn; }
    public String getApplicantColumn() { return applicantColumn; }
    public void setApplicantColumn(String applicantColumn) { this.applicantColumn = applicantColumn; }
    public String getInventorColumn() { return inventorColumn; }
    public void setInventorColumn(String inventorColumn) { this.inventorColumn = inventorColumn; }
    public String getIpcColumn() { return ipcColumn; }
    public void setIpcColumn(String ipcColumn) { this.ipcColumn = ipcColumn; }
    public String getCpcColumn() { return cpcColumn; }
    public void setCpcColumn(String cpcColumn) { this.cpcColumn = cpcColumn; }
    public String getAppliDateColumn() { return appliDateColumn; }
    public void setAppliDateColumn(String appliDateColumn) { this.appliDateColumn = appliDateColumn; }
    public String getPublicDateColumn() { return publicDateColumn; }
    public void setPublicDateColumn(String publicDateColumn) { this.publicDateColumn = publicDateColumn; }
    public String getLegalStatusColumn() { return legalStatusColumn; }
    public void setLegalStatusColumn(String legalStatusColumn) { this.legalStatusColumn = legalStatusColumn; }
    public String getStatusColumn() { return statusColumn; }
    public void setStatusColumn(String statusColumn) { this.statusColumn = statusColumn; }
    public String getDefaultSortColumn() { return defaultSortColumn; }
    public void setDefaultSortColumn(String defaultSortColumn) { this.defaultSortColumn = defaultSortColumn; }
    public String getDefaultSortDirection() { return defaultSortDirection; }
    public void setDefaultSortDirection(String defaultSortDirection) { this.defaultSortDirection = defaultSortDirection; }
    public String getSearchFieldsJson() { return searchFieldsJson; }
    public void setSearchFieldsJson(String searchFieldsJson) { this.searchFieldsJson = searchFieldsJson; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
