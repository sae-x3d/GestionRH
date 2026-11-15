package com.sirh.backend.modules.dossier.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeDocumentStorageService {
    private static final long MAX_BYTES = 15L * 1024 * 1024;
    private final Path root;

    public EmployeeDocumentStorageService(@Value("${sirh.storage.root:datas}") String configuredRoot) {
        Path configured = Path.of(configuredRoot);
        Path working = Path.of("").toAbsolutePath().normalize();
        if (configured.isAbsolute()) this.root = configured.normalize();
        else {
            Path local = working.resolve(configured).normalize();
            Path parent = working.resolve("..").resolve(configured).normalize();
            this.root = Files.isDirectory(local) ? local : Files.isDirectory(parent) ? parent : local;
        }
    }

    public StoredDocument store(Integer employeeId, MultipartFile upload) {
        if (upload == null || upload.isEmpty() || upload.getSize() > MAX_BYTES) throw bad("Le document est obligatoire et limité à 15 Mo.");
        byte[] bytes;
        try { bytes = upload.getBytes(); } catch (IOException exception) { throw bad("Le fichier n’a pas pu être lu."); }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw bad("Le document est obligatoire et limité à 15 Mo.");
        String mime;
        String extension;
        if (isPdf(bytes)) { mime = "application/pdf"; extension = "pdf"; }
        else {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1 || image.getWidth() > 6000 || image.getHeight() > 6000
                        || (long) image.getWidth() * image.getHeight() > 20_000_000L) throw bad("Le document doit être un PDF, PNG ou JPEG valide.");
                String format = imageFormat(bytes);
                if (!format.equals("png") && !format.equals("jpg")) throw bad("Le document doit être un PDF, PNG ou JPEG valide.");
                mime = format.equals("png") ? "image/png" : "image/jpeg";
                extension = format;
            } catch (IOException exception) { throw bad("Le document image est illisible."); }
        }
        String key = "dossier-employe/" + employeeId + "/" + UUID.randomUUID() + "." + extension;
        write(key, bytes);
        String supplied = upload.getOriginalFilename() == null ? "document" : upload.getOriginalFilename();
        supplied = supplied.substring(Math.max(supplied.lastIndexOf('/'), supplied.lastIndexOf('\\')) + 1);
        supplied = supplied.replaceAll("[\\p{Cntrl}]", "").trim();
        if (supplied.isBlank()) supplied = "document." + extension;
        if (supplied.length() > 180) supplied = supplied.substring(supplied.length() - 180);
        return new StoredDocument(key, supplied, mime, bytes.length);
    }

    public StoredDocument storePdf(Integer employeeId, MultipartFile upload) {
        if (upload == null || upload.isEmpty() || upload.getSize() > MAX_BYTES) {
            throw bad("Le PDF est obligatoire et limité à 15 Mo.");
        }
        try {
            if (!isPdf(upload.getBytes())) throw bad("La carte d’identité doit être fournie dans un PDF valide contenant le recto et le verso.");
        } catch (IOException exception) {
            throw bad("Le PDF de la carte d’identité n’a pas pu être lu.");
        }
        return store(employeeId, upload);
    }

    public StoredDocument storeGeneratedPdf(Integer employeeId, String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES || !isPdf(bytes)) {
            throw bad("Le PDF généré est invalide ou dépasse 15 Mo.");
        }
        String key = "dossier-employe/" + employeeId + "/" + UUID.randomUUID() + ".pdf";
        write(key, bytes);
        String safeName = fileName == null ? "contrat.pdf" : fileName;
        safeName = safeName.substring(Math.max(safeName.lastIndexOf('/'), safeName.lastIndexOf('\\')) + 1)
            .replaceAll("[\\p{Cntrl}]", "").trim();
        if (safeName.isBlank()) safeName = "contrat.pdf";
        if (safeName.length() > 180) safeName = safeName.substring(safeName.length() - 180);
        return new StoredDocument(key, safeName, "application/pdf", bytes.length);
    }

    public byte[] read(String key, Integer employeeId) {
        if (key == null || !key.matches("dossier-employe/" + employeeId + "/[a-f0-9-]{36}[.]((pdf)|(png)|(jpg))")) throw missing();
        Path file = root.resolve(key).normalize();
        try {
            Path realRoot = root.toRealPath(); Path realFile = file.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile) || Files.size(realFile) > MAX_BYTES) throw missing();
            byte[] content = Files.readAllBytes(realFile);
            String extension = key.substring(key.lastIndexOf('.') + 1);
            if (extension.equals("pdf") && !isPdf(content)) throw missing();
            if ((extension.equals("png") || extension.equals("jpg")) && ImageIO.read(new ByteArrayInputStream(content)) == null) throw missing();
            return content;
        } catch (IOException exception) { throw missing(); }
    }

    private void write(String key, byte[] bytes) {
        Path file = root.resolve(key).normalize(); Path directory = file.getParent();
        if (!file.startsWith(root) || !directory.startsWith(root)) throw bad("Chemin de stockage invalide.");
        try {
            Files.createDirectories(directory);
            if (!directory.toRealPath().startsWith(root.toRealPath())) throw bad("Chemin de stockage invalide.");
            Path temporary = Files.createTempFile(directory, "upload-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file); }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException exception) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Le document n’a pas pu être enregistré."); }
    }

    private boolean isPdf(byte[] bytes) {
        if (bytes.length < 10 || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) return false;
        int start = Math.max(0, bytes.length - 2048);
        return new String(bytes, start, bytes.length - start, java.nio.charset.StandardCharsets.ISO_8859_1).contains("%%EOF");
    }

    private String imageFormat(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71) return "png";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "jpg";
        return "";
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."); }
    public record StoredDocument(String key, String fileName, String mimeType, long size) { }
}
