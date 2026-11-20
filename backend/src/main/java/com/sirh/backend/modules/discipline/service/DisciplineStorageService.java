package com.sirh.backend.modules.discipline.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
public class DisciplineStorageService {
    private static final long MAX_BYTES = 15L * 1024 * 1024;
    private final Path root;

    public DisciplineStorageService(@Value("${sirh.storage.root:datas}") String configuredRoot) {
        Path configured = Path.of(configuredRoot);
        Path working = Path.of("").toAbsolutePath().normalize();
        if (configured.isAbsolute()) this.root = configured.normalize();
        else {
            Path local = working.resolve(configured).normalize();
            Path parent = working.resolve("..").resolve(configured).normalize();
            this.root = Files.isDirectory(local) ? local : Files.isDirectory(parent) ? parent : local;
        }
    }

    public StoredDocument store(Integer incidentId, MultipartFile upload) {
        if (upload == null || upload.isEmpty() || upload.getSize() > MAX_BYTES) {
            throw bad("La pièce est obligatoire et limitée à 15 Mo.");
        }
        byte[] bytes;
        try { bytes = upload.getBytes(); } catch (IOException exception) { throw bad("Le fichier n’a pas pu être lu."); }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw bad("La pièce est obligatoire et limitée à 15 Mo.");
        String mime;
        String extension;
        if (isPdf(bytes)) { mime = "application/pdf"; extension = "pdf"; }
        else {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1 || image.getWidth() > 6000 || image.getHeight() > 6000
                        || (long) image.getWidth() * image.getHeight() > 20_000_000L) throw bad("La pièce doit être un PDF, PNG ou JPEG valide.");
                String format = imageFormat(bytes);
                if (!format.equals("png") && !format.equals("jpg")) throw bad("La pièce doit être un PDF, PNG ou JPEG valide.");
                mime = format.equals("png") ? "image/png" : "image/jpeg";
                extension = format;
            } catch (IOException exception) { throw bad("La pièce image est illisible."); }
        }
        String key = "discipline/" + incidentId + "/" + UUID.randomUUID() + "." + extension;
        write(key, bytes);
        String supplied = upload.getOriginalFilename() == null ? "piece" : upload.getOriginalFilename();
        supplied = supplied.substring(Math.max(supplied.lastIndexOf('/'), supplied.lastIndexOf('\\')) + 1);
        supplied = supplied.replaceAll("[\\p{Cntrl}]", "").trim();
        if (supplied.isBlank()) supplied = "piece." + extension;
        if (supplied.length() > 180) supplied = supplied.substring(supplied.length() - 180);
        return new StoredDocument(key, supplied, mime, bytes.length);
    }

    public byte[] read(String key, Integer incidentId) {
        if (key == null || !key.matches("discipline/" + incidentId + "/[a-f0-9-]{36}[.]((pdf)|(png)|(jpg))")) throw missing();
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
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La pièce n’a pas pu être enregistrée.");
        }
    }

    private boolean isPdf(byte[] bytes) {
        if (bytes.length < 10 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) return false;
        int start = Math.max(0, bytes.length - 2048);
        return new String(bytes, start, bytes.length - start, StandardCharsets.ISO_8859_1).contains("%%EOF");
    }

    private String imageFormat(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71) return "png";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "jpg";
        return "";
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce introuvable."); }

    public record StoredDocument(String key, String fileName, String mimeType, long size) { }
}
