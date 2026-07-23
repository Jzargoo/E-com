package com.jzargo.media.event;

import com.jzargo.media.storages.persistent.StorageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedOperationEvent {
    private StorageType storageType;
    private String fileUri;
}
