package com.sirh.backend.modules.candidatures.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CandidateFileStorageService {
    private static final long MAX_CV_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PHOTO_BYTES = 4L * 1024 * 1024;
    private final Path root;

    public CandidateFileStorageService(@Value("${sirh.storage.root:datas}") String configuredRoot) {
        Path configured = Path.of(configuredRoot);
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if (configured.isAbsolute()) {
            this.root = configured.normalize();
        } else {
            Path local = workingDirectory.resolve(configured).normalize();
            Path parent = workingDirectory.resolve("..").resolve(configured).normalize();
            this.root = Files.isDirectory(local) ? local : Files.isDirectory(parent) ? parent : local;
        }
    }

    public PreparedFile prepareCv(String username, MultipartFile upload) {
        byte[] bytes = readUpload(upload, MAX_CV_BYTES, "Le CV est obligatoire et ne doit pas dépasser 10 Mo.");
        if (!isPdf(bytes) || (upload.getContentType() != null && !upload.getContentType().isBlank()
                && !"application/pdf".equalsIgnoreCase(upload.getContentType()))) {
            throw badRequest("Le CV doit être un véritable fichier PDF.");
        }
        String relative = "cv/cv_" + safeUsername(username) + ".pdf";
        return new PreparedFile(relative, bytes);
    }

    public PreparedFile preparePhoto(String username, MultipartFile upload) {
        byte[] bytes = readUpload(upload, MAX_PHOTO_BYTES, "La photo ne doit pas dépasser 4 Mo.");
        byte[] normalizedPng = normalizePng(bytes, upload.getContentType());
        String relative = "pdp/pdp_" + safeUsername(username) + ".png";
        return new PreparedFile(relative, normalizedPng);
    }

    public void save(PreparedFile prepared) {
        write(prepared.relativePath(), prepared.content());
    }

    public byte[] readCv(String relativePath) {
        if (relativePath == null || !relativePath.matches("cv/cv_[A-Za-z0-9._-]+[.]pdf")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV introuvable.");
        }
        Path file = checkedFile(relativePath);
        try {
            Path realRoot = root.toRealPath();
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile) || Files.size(realFile) > MAX_CV_BYTES) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV introuvable.");
            }
            byte[] bytes = Files.readAllBytes(realFile);
            if (!isPdf(bytes)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV illisible.");
            return bytes;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV introuvable.");
        }
    }

    public byte[] readPhoto(String relativePath) {
        if (relativePath == null || !relativePath.matches("pdp/pdp_[A-Za-z0-9._-]+[.]png")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo introuvable.");
        }
        Path file = checkedFile(relativePath);
        try {
            Path realRoot = root.toRealPath();
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile) || Files.size(realFile) > MAX_PHOTO_BYTES) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo introuvable.");
            }
            return normalizePng(Files.readAllBytes(realFile), "image/png");
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo introuvable.");
        }
    }

    private byte[] readUpload(MultipartFile upload, long maxBytes, String emptyMessage) {
        if (upload == null || upload.isEmpty()) throw badRequest(emptyMessage);
        if (upload.getSize() > maxBytes) throw badRequest(emptyMessage);
        try {
            byte[] bytes = upload.getBytes();
            if (bytes.length == 0 || bytes.length > maxBytes) throw badRequest(emptyMessage);
            return bytes;
        } catch (IOException exception) {
            throw badRequest("Le fichier envoyé n’a pas pu être lu.");
        }
    }

    private byte[] normalizePng(byte[] bytes, String contentType) {
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < 24 || !Arrays.equals(signature, Arrays.copyOf(bytes, 8))
                || (contentType != null && !contentType.isBlank() && !"image/png".equalsIgnoreCase(contentType))) {
            throw badRequest("La photo de profil doit être un fichier PNG.");
        }
        ByteBuffer dimensions = ByteBuffer.wrap(bytes, 16, 8).order(ByteOrder.BIG_ENDIAN);
        long width = Integer.toUnsignedLong(dimensions.getInt());
        long height = Integer.toUnsignedLong(dimensions.getInt());
        if (width == 0 || height == 0 || width > 6000 || height > 6000 || width * height > 20_000_000L) {
            throw badRequest("Les dimensions de la photo sont trop importantes.");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() != width || image.getHeight() != height) {
                throw badRequest("Le fichier de photo PNG est invalide.");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) throw badRequest("Le fichier PNG n’a pas pu être traité.");
            if (output.size() > MAX_PHOTO_BYTES) throw badRequest("La photo convertie dépasse la taille autorisée.");
            return output.toByteArray();
        } catch (IOException exception) {
            throw badRequest("Le fichier de photo PNG est illisible.");
        }
    }

    private boolean isPdf(byte[] bytes) {
        if (bytes.length < 10 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) return false;
        int start = Math.max(0, bytes.length - 1024);
        return new String(bytes, start, bytes.length - start, StandardCharsets.ISO_8859_1).contains("%%EOF");
    }

    private void write(String relativePath, byte[] bytes) {
        Path directory = root.resolve(relativePath.substring(0, relativePath.indexOf('/'))).normalize();
        Path file = checkedFile(relativePath);
        if (!directory.startsWith(root)) throw badRequest("Chemin de stockage invalide.");
        try {
            Files.createDirectories(directory);
            if (!directory.toRealPath().startsWith(root.toRealPath())) throw badRequest("Chemin de stockage invalide.");
            Path temporary = Files.createTempFile(directory, "sirh-upload-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Le fichier n’a pas pu être enregistré.");
        }
    }

    private Path checkedFile(String relativePath) {
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) throw badRequest("Chemin de stockage invalide.");
        return file;
    }

    private String safeUsername(String username) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L’identifiant du compte n’est pas utilisable pour nommer les fichiers.");
        }
        return username.toLowerCase(java.util.Locale.ROOT);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record PreparedFile(String relativePath, byte[] content) { }
}
