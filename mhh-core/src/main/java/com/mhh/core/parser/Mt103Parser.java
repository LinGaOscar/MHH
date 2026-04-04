package com.mhh.core.parser;

import com.mhh.common.entity.MsgIncoming;
import com.mhh.common.entity.SwiftMessageBase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SWIFT MT103 (Customer Credit Transfer) messages.
 * MT103 is always an incoming payment instruction → returns {@link MsgIncoming}.
 */
@Component
public class Mt103Parser extends AbstractPdfParser {

    @Override
    public boolean supports(String text) {
        return contains(text, "MT 103") || contains(text, "MT103");
    }

    @Override
    public int getPriority() {
        return 50; // higher priority than MtGenericParser (200)
    }

    @Override
    public SwiftMessageBase parse(String text) {
        MsgIncoming msg = new MsgIncoming();
        msg.setMtType("103");
        msg.setMessageType("MT103");

        // :20: — Sender's Reference (Message ID)
        msg.setMessageId(extract(text, ":20:\\s*(\\S+)"));

        // :50A/K: — Ordering Customer (sender), skip account line starting with /
        msg.setSender(extractPartyName(text, "50[AK]?"));

        // :59/59A: — Beneficiary Customer (receiver)
        msg.setReceiver(extractPartyName(text, "59[A]?"));

        // :70: — Remittance Information (reference)
        msg.setReference(extract(text, ":70:\\s*([^\\n:]+)"));

        // :32A: — YYMMDDCCCAMOUNT
        String field32 = extract(text, ":32A:\\s*(\\S+)");
        if (field32 != null) {
            Matcher m = Pattern.compile("^\\d{6}([A-Z]{3})([\\d,]+)")
                    .matcher(field32);
            if (m.find()) {
                msg.setCurrency(m.group(1));
                try {
                    msg.setAmount(new BigDecimal(m.group(2).replace(",", ".")));
                } catch (NumberFormatException ignored) {}
            }
        }

        msg.setMtContent(text);
        msg.setSyncTime(LocalDateTime.now());

        return msg;
    }

    private String extractPartyName(String text, String tagRegex) {
        String raw = extract(text, ":" + tagRegex + ":\\s*(.*?)(?=:\\d{2}[A-Z]?:|$)");
        if (raw == null) return null;
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("/")) return t;
        }
        return raw.trim();
    }
}
