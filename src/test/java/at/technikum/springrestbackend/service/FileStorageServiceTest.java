package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.exception.InvalidFileTypeException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService")
class FileStorageServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String MINIO_URL = "http://localhost:9000";

    @Mock
    private MinioClient minioClient;

    @Mock
    private MultipartFile multipartFile;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(minioClient, BUCKET, MINIO_URL);
    }

   //  uploadBookImage — validation

    @Nested
    @DisplayName("uploadBookImage — validation")
    class UploadBookImageValidation {

        @Test
        @DisplayName("throws BadRequestException when file is null")
        void throwsForNullFile() {
            assertThatThrownBy(() -> service.uploadBookImage(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws BadRequestException when file is empty")
        void throwsForEmptyFile() {
            when(multipartFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> service.uploadBookImage(multipartFile))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("throws InvalidFileTypeException when content type is null")
        void throwsForNullContentType() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn(null);

            assertThatThrownBy(() -> service.uploadBookImage(multipartFile))
                    .isInstanceOf(InvalidFileTypeException.class)
                    .hasMessageContaining("content type");
        }

        @Test
        @DisplayName("throws InvalidFileTypeException when content type is blank")
        void throwsForBlankContentType() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("   ");

            assertThatThrownBy(() -> service.uploadBookImage(multipartFile))
                    .isInstanceOf(InvalidFileTypeException.class);
        }

        @ParameterizedTest(name = "contentType={0} → InvalidFileTypeException")
        @ValueSource(strings = {"application/pdf", "text/plain", "image/gif", "video/mp4"})
        @DisplayName("throws InvalidFileTypeException for disallowed content types")
        void throwsForDisallowedContentType(String contentType) {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn(contentType);

            assertThatThrownBy(() -> service.uploadBookImage(multipartFile))
                    .isInstanceOf(InvalidFileTypeException.class)
                    .hasMessageContaining("JPG, PNG and WEBP");
        }
    }

   //  uploadBookImage — success

    @Nested
    @DisplayName("uploadBookImage — success")
    class UploadBookImageSuccess {

        @Test
        @DisplayName("uploads JPEG and returns StoredFileResult with correct URL")
        void uploadsJpegSuccessfully() throws Exception {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            FileStorageService.StoredFileResult result = service.uploadBookImage(multipartFile);

            assertThat(result.contentType()).isEqualTo("image/jpeg");
            assertThat(result.size()).isEqualTo(1024L);
            assertThat(result.objectKey()).startsWith("books/");
            assertThat(result.objectKey()).endsWith(".jpg");
            assertThat(result.fileUrl()).startsWith(MINIO_URL + "/" + BUCKET + "/books/");
            assertThat(result.fileUrl()).endsWith(".jpg");
        }

        @Test
        @DisplayName("uploads PNG and returns .png extension")
        void uploadsPngSuccessfully() throws Exception {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getSize()).thenReturn(2048L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            FileStorageService.StoredFileResult result = service.uploadBookImage(multipartFile);

            assertThat(result.objectKey()).endsWith(".png");
        }

        @Test
        @DisplayName("creates bucket when it does not exist yet")
        void createsBucketWhenMissing() throws Exception {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/webp");
            when(multipartFile.getSize()).thenReturn(512L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

            service.uploadBookImage(multipartFile);

            verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        }

        @Test
        @DisplayName("skips bucket creation when bucket already exists")
        void skipsBucketCreationWhenExists() throws Exception {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(512L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            service.uploadBookImage(multipartFile);

            verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        }

        @Test
        @DisplayName("URL is built correctly when minioUrl ends with slash")
        void buildsUrlCorrectlyWithTrailingSlash() throws Exception {
            FileStorageService serviceWithSlash = new FileStorageService(
                    minioClient, BUCKET, "http://localhost:9000/"
            );
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(1L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            FileStorageService.StoredFileResult result = serviceWithSlash.uploadBookImage(multipartFile);

            assertThat(result.fileUrl()).doesNotContain("//test-bucket");
            assertThat(result.fileUrl()).startsWith("http://localhost:9000/" + BUCKET + "/");
        }

        @Test
        @DisplayName("wraps MinIO exception as IllegalStateException")
        void wrapsMinioException() throws Exception {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                    .thenThrow(new RuntimeException("MinIO unavailable"));

            assertThatThrownBy(() -> service.uploadBookImage(multipartFile))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to upload");
        }
    }

    //  deleteObjectQuietly

    @Nested
    @DisplayName("deleteObjectQuietly(String)")
    class DeleteObjectQuietly {

        @ParameterizedTest(name = "key={0} → no-op")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("no-op for null or blank objectKey")
        void noOpForNullOrBlankKey(String key) throws Exception {
            service.deleteObjectQuietly(key);

            verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("calls minioClient.removeObject for valid key")
        void deletesForValidKey() throws Exception {
            service.deleteObjectQuietly("books/some-key.jpg");

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("silently swallows MinIO exception (quiet mode)")
        void swallowsException() throws Exception {
            org.mockito.Mockito.doThrow(new RuntimeException("MinIO down"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            service.deleteObjectQuietly("books/key.jpg");
        }
    }

    //  deleteObject

    @Nested
    @DisplayName("deleteObject(String)")
    class DeleteObject {

        @ParameterizedTest(name = "key={0} → BadRequestException")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("throws BadRequestException for null or blank objectKey")
        void throwsForNullOrBlankKey(String key) {
            assertThatThrownBy(() -> service.deleteObject(key))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("calls minioClient.removeObject for valid key")
        void deletesForValidKey() throws Exception {
            service.deleteObject("books/some-key.jpg");

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("wraps MinIO exception as IllegalStateException")
        void wrapsMinioException() throws Exception {
            org.mockito.Mockito.doThrow(new RuntimeException("MinIO down"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            assertThatThrownBy(() -> service.deleteObject("books/key.jpg"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to delete");
        }
    }
}