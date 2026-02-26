package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.exception.InvalidFileTypeException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final MinioClient minioClient;
    private final String bucketName;
    private final String minioUrl;

    public FileStorageService(
            final MinioClient minioClient,
            @Value("${app.minio.bucket-name}") final String bucketName,
            @Value("${app.minio.url}") final String minioUrl
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.minioUrl = minioUrl;
    }

    public StoredFileResult uploadBookImage(final MultipartFile file) {
        validateImageFile(file);

        String contentType = file.getContentType();
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        String objectKey = "books/" + UUID.randomUUID() + "." + extension;

        try {
            ensureBucketExists();

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(contentType)
                                .build()
                );
            }

            String imageUrl = buildObjectUrl(objectKey);

            return new StoredFileResult(
                    objectKey,
                    imageUrl,
                    contentType,
                    file.getSize()
            );

        } catch (Exception exception) {
            throw new IllegalStateException("Failed to upload file to object storage", exception);
        }
    }

    public void deleteObjectQuietly(final String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            // Intentionally ignored for cleanup scenarios (replacement/delete).
            // You can log this later if you add logging.
        }
    }

    public void deleteObject(final String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BadRequestException("Object key is required");
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete file from object storage", exception);
        }
    }

    private void validateImageFile(final MultipartFile file) {
        if (file == null) {
            throw new BadRequestException("File is required");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            throw new InvalidFileTypeException("Uploaded file must have a valid content type");
        }

        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        }
    }

    private String buildObjectUrl(final String objectKey) {
        String normalizedBase = minioUrl.endsWith("/")
                ? minioUrl.substring(0, minioUrl.length() - 1)
                : minioUrl;

        return normalizedBase + "/" + bucketName + "/" + objectKey;
    }

    public record StoredFileResult(
            String objectKey,
            String fileUrl,
            String contentType,
            long size
    ) {
    }
}