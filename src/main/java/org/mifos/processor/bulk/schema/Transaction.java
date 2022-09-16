package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "request_id", "payment_mode", "account_number", "payer_identifier_type", "payer_identifier", "payee_identifier_type", "payee_identifier", "amount", "currency", "note", "program_shortcode", "cycle" })
public class Transaction implements CsvSchema {

    @JsonProperty("id")
    private int id;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("account_number")
    @JsonIgnore
    private String accountNumber;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;
    @JsonProperty("note")
    private String note;

    @JsonProperty(value = "payer_identifier_type")
    private String payerIdentifierType;

    @JsonProperty("payer_identifier")
    private String payerIdentifier;

    @JsonProperty("payee_identifier_type")
    private String payeeIdentifierType;

    @JsonProperty("payee_identifier")
    private String payeeIdentifier;

    @JsonProperty("program_shortcode")
    private String programShortCode;

    @JsonProperty("cycle")
    private String cycle;

    @JsonIgnore
    private String batchId;

    @JsonIgnore
    public String getAccountNumber() {
        return accountNumber;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", request_id='" + requestId + '\'' +
                ", payment_mode='" + paymentMode + '\'' +
                ", account_number='" + accountNumber + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", note='" + note + '\'' +
                ", batchId='" + batchId + '\'' +
                '}';
    }

    @JsonIgnore
    @Override
    public String getCsvString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s", id, requestId, paymentMode, accountNumber, amount, currency, note);
    }

    @JsonIgnore
    @Override
    public String getCsvHeader() {
        return "id,request_id,payment_mode,account_number,amount,currency,note";
    }

}
