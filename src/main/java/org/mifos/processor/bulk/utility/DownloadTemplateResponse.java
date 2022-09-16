package org.mifos.processor.bulk.utility;

import org.json.JSONObject;

public class DownloadTemplateResponse extends ApiResponse {

    public DownloadTemplateResponse(JSONObject response) {
        super(response);
    }

    public static class Builder {
        private String fields;

        public Builder setFields(String fields) {
            this.fields = fields;
            return this;
        }

        public DownloadTemplateResponse build() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fields", this.fields);


            return new DownloadTemplateResponse(jsonObject);
        }

    }

}
