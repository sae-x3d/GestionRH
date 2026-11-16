package com.sirh.backend.modules.discipline.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class DisciplineStorageServiceTest {
    @TempDir Path root;

    @Test
    void refuseUnePieceTropVolumineuse() {
        DisciplineStorageService storage = new DisciplineStorageService(root.toString());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(15L * 1024 * 1024 + 1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> storage.store(50, file));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("15 Mo"));
    }

    @Test
    void refuseUnTypeNonSupporte() {
        DisciplineStorageService storage = new DisciplineStorageService(root.toString());
        MultipartFile file = file("notes.txt", "ceci n'est pas une image".getBytes(StandardCharsets.UTF_8));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> storage.store(50, file));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("PDF, PNG ou JPEG"));
    }

    @Test
    void accepteUnPdfEtNettoieLeNomDeFichier() throws Exception {
        DisciplineStorageService storage = new DisciplineStorageService(root.toString());
        byte[] pdf = "%PDF-1.4\ncontenu\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        MultipartFile file = file("../../evil.pdf", pdf);

        DisciplineStorageService.StoredDocument document = storage.store(50, file);

        assertEquals("application/pdf", document.mimeType());
        assertEquals("evil.pdf", document.fileName());
        assertEquals(pdf.length, document.size());
        assertTrue(document.key().matches("discipline/50/[a-f0-9-]{36}[.]pdf"));
        assertArrayEquals(pdf, storage.read(document.key(), 50));
    }

    @Test
    void accepteUneImagePngValide() throws Exception {
        DisciplineStorageService storage = new DisciplineStorageService(root.toString());
        byte[] png = pngBytes();
        MultipartFile file = file("capture.png", png);

        DisciplineStorageService.StoredDocument document = storage.store(50, file);

        assertEquals("image/png", document.mimeType());
        assertEquals("capture.png", document.fileName());
        assertArrayEquals(png, storage.read(document.key(), 50));
    }

    @Test
    void refuseUneCleDeLectureInvalide() {
        DisciplineStorageService storage = new DisciplineStorageService(root.toString());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> storage.read("discipline/50/../../secret.pdf", 50));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void refuseUnePieceAbsente() {
        DisciplineStorageService storage = new DisciplineStorageService(root.toString());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> storage.store(50, file));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private static MultipartFile file(String name, byte[] bytes) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) bytes.length);
        try {
            when(file.getBytes()).thenReturn(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        when(file.getOriginalFilename()).thenReturn(name);
        return file;
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff0000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
