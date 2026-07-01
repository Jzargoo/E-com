package com.jzargo.media.service;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.ErrorDuringAddingContent;
import com.jzargo.protobuf.PlainFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class MediaStorageServiceImpl implements MediaPrimaryStorageService {

}
