package com.loopin.api.media.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.media.completeupload.CompleteMediaUploadCommand;
import com.loopin.api.media.completeupload.CompleteMediaUploadHandler;
import com.loopin.api.media.deletemedia.DeleteMediaCommand;
import com.loopin.api.media.deletemedia.DeleteMediaHandler;
import com.loopin.api.media.dto.request.RequestMediaUploadRequest;
import com.loopin.api.media.dto.response.MediaCompletionResponse;
import com.loopin.api.media.dto.response.MediaUploadResponse;
import com.loopin.api.media.requestupload.RequestMediaUploadCommand;
import com.loopin.api.media.requestupload.RequestMediaUploadHandler;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Management")
@SecurityRequirement(name = "BearerAuth")
public class MediaController {

    private final RequestMediaUploadHandler requestUploadHandler;
    private final CompleteMediaUploadHandler completeUploadHandler;
    private final DeleteMediaHandler deleteMediaHandler;

    @PostMapping("/uploads")
    public ResponseEntity<MediaUploadResponse> requestUpload(
        @Valid @RequestBody RequestMediaUploadRequest request
    ) {
        MediaUploadResponse response = requestUploadHandler.handle(
            new RequestMediaUploadCommand(
                request,
                SecurityUtils.getRequiredCurrentUserEmail()
            )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/uploads/{mediaId}/complete")
    public ResponseEntity<MediaCompletionResponse> completeUpload(
        @PathVariable UUID mediaId
    ) {
        return ResponseEntity.ok(
            completeUploadHandler.handle(
                new CompleteMediaUploadCommand(
                    mediaId,
                    SecurityUtils.getRequiredCurrentUserEmail()
                )
            )
        );
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
        @PathVariable UUID mediaId
    ) {
        deleteMediaHandler.handle(
            new DeleteMediaCommand(
                mediaId,
                SecurityUtils.getRequiredCurrentUserEmail()
            )
        );

        return ResponseEntity.noContent().build();
    }
}
