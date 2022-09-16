package org.mifos.processor.bulk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "template")
public class TemplateTypeProperties {

    List<TemplateType> types = new ArrayList<>();

    public TemplateTypeProperties() {
    }

    public List<TemplateType> getTypes() {
        return types;
    }

    public void setTypes(List<TemplateType> types) {
        this.types = types;
    }

    public TemplateType getById(String templateId) {
        return getTypes().stream()
                .filter(p -> p.getId().equals(templateId))
                .findFirst()
                .orElse(new TemplateType(""));
    }

}
