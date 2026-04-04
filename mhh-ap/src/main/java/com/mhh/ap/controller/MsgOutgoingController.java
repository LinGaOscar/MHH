package com.mhh.ap.controller;

import com.mhh.ap.annotation.LogAction;
import com.mhh.common.entity.MsgOutgoing;
import com.mhh.common.entity.MsgOutgoingTx;
import com.mhh.common.repository.MsgOutgoingRepository;
import com.mhh.common.repository.MsgOutgoingTxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 出電查詢 API。
 *
 * 列表查詢（GET /api/outgoing）：只存取 MSG_OUTGOING 搜尋表，不載入電文大欄位。
 * 電文內容（GET /api/outgoing/{messageId}/content）：存取 MSG_OUTGOING_TX 電文表。
 */
@RestController
@RequestMapping("/api/outgoing")
@RequiredArgsConstructor
public class MsgOutgoingController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final MsgOutgoingRepository outgoingRepository;
    private final MsgOutgoingTxRepository outgoingTxRepository;

    /** 出電列表查詢（分頁，僅搜尋表欄位） */
    @GetMapping
    @LogAction(action = "SEARCH_OUTGOING", description = "查詢出電列表")
    public Map<String, Object> list(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String msgType,
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) String amountFrom,
            @RequestParam(required = false) String amountTo,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String tag20,
            @RequestParam(required = false) String tag21,
            @RequestParam(required = false) String isnFrom,
            @RequestParam(required = false) String isnTo,
            @RequestParam(required = false) String unitCode,
            @RequestParam(required = false) String amlFlag,
            @RequestParam(required = false) String amlStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDateTime from = parse(dateFrom);
        LocalDateTime to   = parse(dateTo);
        String typePrefix  = toMsgTypePrefix(msgType);
        BigDecimal amtFrom = parseBigDecimal(amountFrom);
        BigDecimal amtTo   = parseBigDecimal(amountTo);

        Page<MsgOutgoing> result = outgoingRepository.findByFilters(
                from, to, typePrefix,
                blank(sender), blank(receiver),
                amtFrom, amtTo,
                blank(reference), blank(tag20), blank(tag21),
                blank(isnFrom), blank(isnTo),
                blank(unitCode), blank(amlFlag), blank(amlStatus),
                PageRequest.of(page, size));

        List<Map<String, Object>> rows = result.getContent().stream()
                .map(this::toRow).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("content",       rows);
        resp.put("totalElements", result.getTotalElements());
        resp.put("totalPages",    result.getTotalPages());
        resp.put("page",          result.getNumber());
        resp.put("size",          result.getSize());
        return resp;
    }

    /** 出電電文內容（存取 MSG_OUTGOING_TX） */
    @GetMapping("/{messageId}/content")
    public ResponseEntity<Map<String, Object>> content(@PathVariable String messageId) {
        Optional<MsgOutgoingTx> tx = outgoingTxRepository.findByMessageId(messageId);
        if (tx.isEmpty()) return ResponseEntity.notFound().build();

        Map<String, Object> resp = new HashMap<>();
        resp.put("messageId",  messageId);
        resp.put("mtContent",  tx.get().getMtContent());
        resp.put("mxContent",  tx.get().getMxContent());
        return ResponseEntity.ok(resp);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Map<String, Object> toRow(MsgOutgoing m) {
        Map<String, Object> r = new HashMap<>();
        r.put("messageId",   m.getMessageId());
        r.put("messageType", m.getMessageType());
        r.put("mtType",      m.getMtType());
        r.put("mxType",      m.getMxType());
        r.put("osn",         m.getOsn());
        r.put("tag20",       m.getTag20());
        r.put("tag21",       m.getTag21());
        r.put("amlFlag",     m.getAmlFlag());
        r.put("amlStatus",   m.getAmlStatus());
        r.put("sender",      m.getSender());
        r.put("receiver",    m.getReceiver());
        r.put("valueDate",   m.getValueDate());
        r.put("currency",    m.getCurrency());
        r.put("amount",      m.getAmount());
        r.put("flowStatus",  m.getFlowStatus());
        r.put("reference",   m.getReference());
        r.put("unitCode",    m.getUnitCode());
        r.put("pdeFlag",     m.getPdeFlag());
        r.put("msgDate",     m.getMsgDate() != null ? m.getMsgDate().toString().replace("T", " ") : "");
        r.put("syncTime",    m.getSyncTime() != null ? m.getSyncTime().toString().replace("T", " ") : "");
        return r;
    }

    private String toMsgTypePrefix(String msgType) {
        if (msgType == null || msgType.isBlank()) return null;
        return "fin".equalsIgnoreCase(msgType) ? "MT" : msgType;
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s, FMT); } catch (Exception e) { return null; }
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (Exception e) { return null; }
    }

    private String blank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }
}
