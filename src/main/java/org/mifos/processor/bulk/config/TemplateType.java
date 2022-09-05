package org.mifos.processor.bulk.config;

public class TemplateType {

    private String id, metadata;

    public TemplateType() {
    }

    public TemplateType(String id, String metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    public TemplateType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
