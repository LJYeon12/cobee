package org.example.common.util;

import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

public class ImageValidationUtil {
    private static final int MAX_MULTIPLE_COUNT = 3;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    public static void validateSingleImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 유효하지 않습니다."); // (기존 CustomException -> IllegalArgumentException)
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과합니다."); // (기존 CustomException -> IllegalArgumentException)
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpeg, jpg, png, webp만 가능)"); // (기존 CustomException -> IllegalArgumentException)
        }
    }

    public static void validateMultipleImageFiles(MultipartFile[] files)
    {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다."); // (기존 CustomException -> IllegalArgumentException)
        }
        if (files.length > MAX_MULTIPLE_COUNT) {
            throw new IllegalArgumentException("한 번에 최대 3개의 파일만 업로드할 수 있습니다."); // (기존 CustomException -> IllegalArgumentException)
        }
        for (MultipartFile file : files) {
            validateSingleImageFile(file);
        }
    }
}