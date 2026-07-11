package com.loopin.api.media.dto.request;

import com.loopin.api.media.enums.MediaPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestMediaUploadRequest {

    @NotNull(message = "Media purpose is required")
    private MediaPurpose purpose;

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Size(max = 100, message = "Content type must not exceed 100 characters")
    private String contentType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be greater than zero")
    private Long sizeBytes;
}
