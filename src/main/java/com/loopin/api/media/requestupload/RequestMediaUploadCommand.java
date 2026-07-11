package com.loopin.api.media.requestupload;

import com.loopin.api.media.dto.request.RequestMediaUploadRequest;

public record RequestMediaUploadCommand(
    RequestMediaUploadRequest request,
    String currentUserEmail
) {
}
