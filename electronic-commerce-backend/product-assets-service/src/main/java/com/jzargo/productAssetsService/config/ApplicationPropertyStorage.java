package com.jzargo.productAssetsService.config;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@Component
@Data
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationPropertyStorage {
    @NotNull
    private Media media;
    @NotNull
    private FallbackMedia fallbackMedia;

    @NotNull
    private Server server;

    @NotNull
    private Grpc grpc;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Media{
        private String path;
        @NotNull
        private String defaultAvatarUri;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Grpc {
        private String channelMediaName;
        private Integer portionSize;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FallbackMedia {
        private Integer portionSize;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Server {

        @NotNull(message = "max header size is expected to exist")
        @Min(value = 0, message = "max header size has to be a positive number!")
        private Integer maxHeaderSize;

        @NotNull(message = "max content byte count is expected to exist")
        @Min(value = 0, message = "max content byte count has to be a positive number!")
        private Integer maxContentByteCount;

        @NotNull(message = "max initial length is expected to exist")
        @Min(value = 0, message = "max initial length has to be a positive number!")
        private Integer maxInitialLineLength;

        @NotNull(message = "buffer size is expected to exist")
        @Min(value = 0, message = "buffer size has to be a positive number!")
        private Integer bufferSize;
    }
}
