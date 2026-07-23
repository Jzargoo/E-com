package com.jzargo.media.event;

import com.jzargo.media.storages.persistent.StorageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecoveryEvent {
    StorageType storageType;
}