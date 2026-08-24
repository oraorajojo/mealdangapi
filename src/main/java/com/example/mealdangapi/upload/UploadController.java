package com.example.mealdangapi.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    // 로그인한 사용자만 업로드 가능 (SecurityConfig에서 이 경로는 인증 필요 - GET /uploads/**만 공개).
    @PostMapping("/images")
    public ResponseEntity<UploadResponse> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        String storedFileName = fileStorageService.store(file);
        String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(storedFileName)
                .toUriString();

        return ResponseEntity.ok(new UploadResponse(imageUrl));
    }
}
