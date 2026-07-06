package com.jzargo.media.event;

public interface DlqPublisher {
    void reserveUnprocessedEventUnavailableService(FileCreatedSyncEvent event);
}
