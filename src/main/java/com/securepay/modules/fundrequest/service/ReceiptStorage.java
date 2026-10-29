package com.securepay.modules.fundrequest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Stores fund-request receipt images (port of saveUploadedFile / saveBase64Image /
 * saveBase64ImageChunks). Only jpg/jpeg/png up to 2MB are accepted; the real type is detected
 * from the file's magic bytes (not the client-supplied name), like PHP's finfo.
 *
 * <p>Returned paths use the legacy {@code storage/images/<file>} form so existing clients and the
 * secure-images route keep working.</p>
 */
@Component
public class ReceiptStorage {

    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final Path root;

    public ReceiptStorage(@Value("${securepay.storage.dir:./storage/public}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    /** Save decoded image bytes; returns "storage/images/xxx.ext" or null if invalid. */
    public String saveBytes(byte[] data, String prefix) {
        if (data == null || data.length == 0 || data.length > MAX_BYTES) {
            return null;
        }
        String ext = detectExtension(data);
        if (ext == null) {
            return null;
        }
        String filename = prefix + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = root.resolve("images");
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), data);
            return "storage/images/" + filename;
        } catch (IOException e) {
            return null;
        }
    }

    /** "data:image/png;base64,...." (single data-URI) → saved path or null. */
    public String saveDataUri(String dataUri, String prefix) {
        if (dataUri == null || !dataUri.matches("(?s)^data:image/(jpeg|jpg|png);base64,.+")) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
            return saveBytes(bytes, prefix);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** receipt1, receipt2, ... base64 chunks concatenated in numeric order → saved path or null. */
    public String saveBase64Chunks(Map<String, Object> request, String prefix) {
        java.util.TreeMap<Integer, String> chunks = new java.util.TreeMap<>();
        for (Map.Entry<String, Object> e : request.entrySet()) {
            if (e.getKey().matches("^receipt\\d+$") && e.getValue() != null) {
                chunks.put(Integer.parseInt(e.getKey().substring("receipt".length())), e.getValue().toString());
            }
        }
        if (chunks.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(String.join("", chunks.values()));
            return saveBytes(bytes, prefix);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Delete a previously stored receipt ("storage/images/x.png"); silently ignores missing. */
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        try {
            String name = Paths.get(storedPath).getFileName().toString();
            Files.deleteIfExists(root.resolve("images").resolve(name));
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    /** Resolve a stored filename to a real file (path-traversal safe); null if absent. */
    public Path resolve(String filename) {
        String safe = Paths.get(filename).getFileName().toString();
        Path p = root.resolve("images").resolve(safe).normalize();
        return p.startsWith(root) && Files.exists(p) ? p : null;
    }

    public String mimeOf(Path path) throws IOException {
        byte[] head = Files.readAllBytes(path);
        String ext = detectExtension(head);
        return ext == null ? null : ("png".equals(ext) ? "image/png" : "image/jpeg");
    }

    private static String detectExtension(byte[] d) {
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "png";
        }
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        return null;
    }
}
