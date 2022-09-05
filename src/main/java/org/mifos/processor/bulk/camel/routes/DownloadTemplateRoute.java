package org.mifos.processor.bulk.camel.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DownloadTemplateRoute extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() throws Exception {

        from("rest:GET:/template")
                .id("GET:template")
                .log("Starting incoming template API request handling")
                .process(exchange -> {
                    String requestId = exchange.getIn().getHeader("X-CorrelationID", String.class);
                    String tenant = exchange.getIn().getHeader("Platform-TenantId", String.class);
                    String type = exchange.getIn().getHeader("type", String.class);

                    exchange.getIn().setBody(requestId + tenant + type);
                });
    }

}
