package com.mhh.core.parser;

import com.mhh.common.entity.MessageHistory;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Example parser for SWIFT MT103 messages.
 */
@Component
public class Mt103Parser extends AbstractPdfParser {

    @Override
    public boolean supports(String text) {
        return contains(text, "MT 103") || contains(text, "MT103");
    }

    @Override
    public MessageHistory parse(String text) {
        MessageHistory msg = new MessageHistory();
        msg.setMessageType("MT103");
        msg.setSender(extract(text, "Sender:\\s*(\\S+)"));
        msg.setReceiver(extract(text, "Receiver:\\s*(\\S+)"));
        msg.setReference(extract(text, "Reference:\\s*(\\S+)"));
        
        String amountStr = extract(text, "Amount:\\s*(\\d+\\.?\\d*)");
        if (amountStr != null) {
            msg.setAmount(new BigDecimal(amountStr));
        }
        
        msg.setCurrency(extract(text, "Currency:\\s*([A-Z]{3})"));
        msg.setContent(text);
        msg.setSyncTime(LocalDateTime.now());
        msg.setSource("PDF");
        
        return msg;
    }
}
