package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${app.allowed-root:./allowed_root}")
    private String allowedRoot;

    public static class DeleteRequest {
        public List<String> paths = new ArrayList<>();
    }

    public static class DeleteResult {
        public String path;
        public boolean deleted;
        public String reason;
        public DeleteResult(String path, boolean deleted, String reason) {
            this.path = path; this.deleted = deleted; this.reason = reason;
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody DeleteRequest req) {
        Path root = Paths.get(allowedRoot).toAbsolutePath().normalize();
        List<DeleteResult> results = new ArrayList<>();
        for (String p : req.paths) {
            results.add(handleOne(root, p));
        }
        return ResponseEntity.ok(Map.of("results", results));
    }

    private DeleteResult handleOne(Path root, String raw) {
        if (raw == null || raw.isBlank()) {
            return new DeleteResult(raw, false, "empty path");
        }
        Path target = Paths.get(raw).toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            return new DeleteResult(raw, false, "outside allowed_root");
        }
        if (Files.isSymbolicLink(target)) {
            return new DeleteResult(raw, false, "symlink refused");
        }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return new DeleteResult(raw, false, "does not exist");
        }
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            return new DeleteResult(raw, false, "not a regular file");
        }
        try {
            Files.delete(target);
            return new DeleteResult(raw, true, "deleted");
        } catch (IOException e) {
            return new DeleteResult(raw, false, "io error: " + e.getMessage());
        }
    }
}
