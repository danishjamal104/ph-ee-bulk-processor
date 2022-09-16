package org.mifos.processor.bulk.camel.routes;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.json.JSONObject;
import org.mifos.processor.bulk.config.TemplateType;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.DownloadTemplateResponse;
import org.mifos.processor.bulk.utility.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;
import static org.mifos.processor.bulk.utility.Utils.strip;

@Component
public class DownloadTemplateRoute extends BaseRouteBuilder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() throws Exception {

        from("rest:GET:/template")
                .id("GET:template")
                .log("Starting incoming template API request handling")
                .process(exchange -> {
                    String type = exchange.getIn().getHeader("types", String.class);
                    if (type == null) {
                        JSONObject errorResponse = new ErrorResponse.Builder()
                                .setErrorCode(500)
                                .setErrorDescription("Invalid request parameters")
                                .setDeveloperMessage("Query parameter \"types\" might be missing in the request")
                                .build()
                                .getResponse();
                        exchange.getIn().setHeader("Content-Type",
                                constant("application/json;charset=UTF-8"));
                        exchange.getIn().setBody(errorResponse.toString());
                        return;
                    }
                    type = strip(type).replace(" ", "");

                    List<String> types = Arrays.asList(type.split(","));

                    String filepath = createDefaultCSV();
                    filterHeadersBasedOnTypes(filepath, types);

                    BufferedReader reader =  new BufferedReader(new FileReader(filepath));
                    String data = reader.readLine();
                    reader.close();
                    new File(filepath).deleteOnExit();

                    JSONObject response = new DownloadTemplateResponse.Builder()
                            .setFields(data)
                            .build().getResponse();
                    exchange.getIn().setHeader("Content-Type", "application/json;charset=UTF-8");
                    exchange.getIn().setBody(response.toString());
                });
    }

    private String createDefaultCSV() throws IOException {
        Transaction transaction = new Transaction();

        CsvSchema csvSchema = csvMapper.schemaFor(Transaction.class).withHeader();

        File file = new File("template.csv");
        SequenceWriter writer = csvMapper.writerWithSchemaFor(Transaction.class).with(csvSchema).writeValues(file);
        writer.write(transaction);

        return file.getAbsolutePath();
    }

    private void filterHeadersBasedOnTypes(String filepath, List<String> types) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        String header = reader.readLine();
        reader.close();

        List<String> headers = new LinkedList<>(Arrays.asList(header.split(",")));

        List<String> allTypes = new ArrayList<>();

        for (TemplateType templateType: templateTypeProperties.getTypes()) {
            allTypes.add(templateType.getId());
        }

        allTypes.retainAll(types);

        for (TemplateType templateType: templateTypeProperties.getTypes()) {
            if (!allTypes.contains(templateType.getId()) && allTypes.size() > 0) {
                for (String metadata: templateType.getMetadata().split(",")) {
                    headers.remove(metadata);
                }
            }
        }

        StringBuilder newHeader = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            if (i != 0) {
                newHeader.append(",");
            }
            newHeader.append(headers.get(i));
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
        writer.write(newHeader.toString());
        writer.close();
    }

}
