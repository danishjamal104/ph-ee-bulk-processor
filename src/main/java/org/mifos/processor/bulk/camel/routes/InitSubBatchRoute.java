package org.mifos.processor.bulk.camel.routes;

import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.connector.common.gsma.dto.GsmaParty;
import org.mifos.connector.common.mojaloop.dto.MoneyData;
import org.mifos.connector.common.mojaloop.dto.Party;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.mifos.connector.common.mojaloop.dto.TransactionType;
import org.mifos.connector.common.mojaloop.type.IdentifierType;
import org.mifos.processor.bulk.utility.Utils;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.zeebe.BpmnConfig;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mifos.connector.common.mojaloop.type.InitiatorType.CONSUMER;
import static org.mifos.connector.common.mojaloop.type.Scenario.TRANSFER;
import static org.mifos.connector.common.mojaloop.type.TransactionRole.PAYER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class InitSubBatchRoute extends BaseRouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    private BpmnConfig bpmnConfig;

    @Value("${bpmn.flows.international-remittance-payer}")
    private String internationalRemittancePayer;

    @Override
    public void configure() throws Exception {

        /**
         * Base route for kicking off init sub batch logic. Performs below tasks.
         * 1. Downloads the csv form cloud.
         * 2. Builds the [Transaction] array using [direct:get-transaction-array] route.
         * 3. Loops through each transaction and start the respective workflow
         */
        from(RouteId.INIT_SUB_BATCH.getValue())
                .id(RouteId.INIT_SUB_BATCH.getValue())
                .log("Starting route " + RouteId.INIT_SUB_BATCH.name())
                .to("direct:download-file")
                .to("direct:get-transaction-array")
                .to("direct:start-workflow");

        // Loops through each transaction and start the respective workflow
        from("direct:start-workflow")
                .id("direct:start-flow")
                .log("Starting route direct:start-flow")
                .process(exchange -> {
                    String tenantName = exchange.getProperty(TENANT_NAME, String.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    if (transactionList.get(0).getPayment_mode().equalsIgnoreCase("slcb")) {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put(BATCH_ID, exchange.getProperty(BATCH_ID));
                        variables.put(SUB_BATCH_ID, UUID.randomUUID().toString());
                        variables.put(FILE_NAME, exchange.getProperty(SERVER_FILE_NAME));
                        variables.put(REQUEST_ID, exchange.getProperty(REQUEST_ID));
                        variables.put(PURPOSE, exchange.getProperty(PURPOSE));
                        variables.put(TOTAL_AMOUNT, exchange.getProperty(TOTAL_AMOUNT));
                        variables.put(ONGOING_AMOUNT, exchange.getProperty(ONGOING_AMOUNT));
                        variables.put(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
                        variables.put(COMPLETED_AMOUNT, exchange.getProperty(COMPLETED_AMOUNT));

                        zeebeProcessStarter.startZeebeWorkflow(
                                Utils.getTenantSpecificWorkflowId(bpmnConfig.slcbBpmn, tenantName), variables);
                    } else if (transactionList.get(0).getPayment_mode().equalsIgnoreCase("gsma")) {
                        for (Transaction transaction: transactionList) {
                            GSMATransaction gsmaChannelRequest = new GSMATransaction();
                            gsmaChannelRequest.setAmount(transaction.getAmount());
                            gsmaChannelRequest.setCurrency(transaction.getCurrency());
                            gsmaChannelRequest.setRequestingLei("ibank-usa");
                            gsmaChannelRequest.setReceivingLei("ibank-india");
                            GsmaParty creditParty = new GsmaParty();
                            creditParty.setKey("msisdn");
                            creditParty.setValue(transaction.getAccount_number());
                            GsmaParty debitParty = new GsmaParty();
                            debitParty.setKey("msisdn");
                            debitParty.setValue(transaction.getAccount_number());
                            gsmaChannelRequest.setCreditParty(new GsmaParty[]{creditParty});
                            gsmaChannelRequest.setDebitParty(new GsmaParty[]{debitParty});

                            TransactionChannelRequestDTO channelRequest = new TransactionChannelRequestDTO(); // Fineract Object
                            Party payee = new Party(new PartyIdInfo(IdentifierType.MSISDN, transaction.getAccount_number()));
                            Party payer = new Party(new PartyIdInfo(IdentifierType.MSISDN, "7543010"));

                            MoneyData moneyData = new MoneyData();
                            moneyData.setAmount(transaction.getAmount());
                            moneyData.setCurrency(transaction.getCurrency());

                            channelRequest.setPayer(payer);
                            channelRequest.setPayee(payee);
                            channelRequest.setAmount(moneyData);

                            TransactionType transactionType = new TransactionType();
                            transactionType.setInitiator(PAYER);
                            transactionType.setInitiatorType(CONSUMER);
                            transactionType.setScenario(TRANSFER);

                            Map<String, Object> extraVariables = new HashMap<>();
                            extraVariables.put(IS_RTP_REQUEST, false);
                            extraVariables.put(TRANSACTION_TYPE, "inttransfer");
                            extraVariables.put(TENANT_ID, tenantName);

                            extraVariables.put(BATCH_ID, transaction.getBatchId());

                            String tenantSpecificBpmn = internationalRemittancePayer.replace("{dfspid}", tenantName);
                            channelRequest.setTransactionType(transactionType);

                            PartyIdInfo requestedParty = (boolean)extraVariables.get(IS_RTP_REQUEST) ? channelRequest.getPayer().getPartyIdInfo() : channelRequest.getPayee().getPartyIdInfo();
                            extraVariables.put(PARTY_ID_TYPE, requestedParty.getPartyIdType());
                            extraVariables.put(PARTY_ID, requestedParty.getPartyIdentifier());

                            extraVariables.put(GSMA_CHANNEL_REQUEST, objectMapper.writeValueAsString(gsmaChannelRequest));
                            extraVariables.put(PARTY_LOOKUP_FSPID, gsmaChannelRequest.getReceivingLei());
                            extraVariables.put(INITIATOR_FSPID, gsmaChannelRequest.getRequestingLei());

                            String transactionId = zeebeProcessStarter.startZeebeWorkflow(tenantSpecificBpmn,
                                    objectMapper.writeValueAsString(channelRequest),
                                    extraVariables);

                            logger.info("GSMA Transaction Started with: {}", transactionId);
                        }
                    } else {
                        logger.info("Payment mode {} not configured", transactionList.get(0).getPayment_mode());
                    }

                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
                });
    }

}
