package com.loopin.api.media.shared.cleanup;

import com.loopin.api.media.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaObjectDeletionListener {

    private final ObjectStorage objectStorage;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(MediaObjectDeletionRequested event) {
        try {
            objectStorage.delete(event.objectKey());

            log.info(
                "Deleted detached media object: mediaId={}, objectKey={}",
                event.mediaId(),
                event.objectKey()
            );
        } catch (RuntimeException exception) {

            log.error(
                "Could not delete detached media object: mediaId={}, objectKey={}",
                event.mediaId(),
                event.objectKey(),
                exception
            );
        }
    }
}
