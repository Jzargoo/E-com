package com.jzargo.media.event;

public interface EventPublisher {
    void publishFileRequestedEvent(FileRequestEvent event);
    void publishFileCreatedSyncEvent(FileCreatedSyncEvent event);
}
