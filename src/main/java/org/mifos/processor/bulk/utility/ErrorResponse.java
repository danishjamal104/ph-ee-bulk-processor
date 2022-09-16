package org.mifos.processor.bulk.utility;

import org.json.JSONObject;

public class ErrorResponse extends ApiResponse {

    public ErrorResponse(JSONObject response) {
        super(response);
    }

    public static class Builder {

        @lombok.Builder.Default
        private Integer errorCode;
        private String errorDescription;
        private String developerMessage;


        public Builder setErrorCode(int errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder setErrorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public Builder setDeveloperMessage(String developerMessage) {
            this.developerMessage = developerMessage;
            return this;
        }

        public ErrorResponse build() {
            if (errorCode == null) {
                throw new NullPointerException("Error code cant be null");
            }
            if (errorDescription == null) {
                errorDescription = "Internal Error";
            }
            if (developerMessage == null || developerMessage.isEmpty()) {
                developerMessage = errorDescription;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("errorCode", errorCode);
            jsonObject.put("errorDescription", errorDescription);
            jsonObject.put("developerMessage", developerMessage);

            return new ErrorResponse(jsonObject);
        }


    }

}
