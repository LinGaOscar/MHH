package com.mhh.batch.job;

import com.mhh.common.entity.MessageHistory;
import com.mhh.common.repository.MessageHistoryRepository;
import com.mhh.core.parser.ParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Job to scan folders (MX/MT), extract PDF text, parse and save to DB.
 */
@Component("pdfImportJob")
@Slf4j
public class PdfImportJob implements MhhJob {

    @Override
    public String getJobName() {
        return "PdfImportJob";
    }

    private final ParserFactory parserFactory;
    private final MessageHistoryRepository repository;

    @Value("${mhh.paths.mx}")
    private String mxPath;

    @Value("${mhh.paths.mt}")
    private String mtPath;

    @Value("${mhh.paths.archive}")
    private String archivePath;

    @Autowired
    public PdfImportJob(ParserFactory parserFactory, MessageHistoryRepository repository) {
        this.parserFactory = parserFactory;
        this.repository = repository;
    }

    public void execute() {
        log.info("Starting PdfImportJob...");
        processFolder(mxPath);
        processFolder(mtPath);
        log.info("PdfImportJob completed.");
    }

    private void processFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Folder does not exist or is not a directory: {}", folderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) {
            log.info("No PDF files found in {}.", folderPath);
            return;
        }

        for (File file : files) {
            processFile(file);
        }
    }

    private void processFile(File file) {
        log.info("Processing PDF: {}", file.getName());
        try {
            String text = extractText(file);
            Optional<MessageHistory> result = parserFactory.parse(text);

            if (result.isPresent()) {
                MessageHistory msg = result.get();
                msg.setMessageId(msg.getMessageId() != null ? msg.getMessageId() : file.getName());
                msg.setSource("PDF");
                msg.setSyncTime(LocalDateTime.now());
                repository.save(msg);
                log.info("Successfully imported {} as {}", file.getName(), msg.getMessageType());
                archiveFile(file);
            } else {
                log.warn("No suitable parser found for PDF: {}", file.getName());
            }
        } catch (Exception e) {
            log.error("Failed to process PDF {}: {}", file.getName(), e.getMessage(), e);
        }
    }

    private String extractText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private void archiveFile(File file) {
        try {
            Path target = Paths.get(archivePath, file.getName());
            Files.createDirectories(target.getParent());
            Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archived processed file to: {}", target);
        } catch (IOException e) {
            log.error("Failed to archive file {}: {}", file.getName(), e.getMessage());
        }
    }
}
