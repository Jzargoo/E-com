package com.jzargo.productAssetsService.config;

import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyServerConfigurer {

    @Bean
    public NettyServerCustomizer  nettyServerCustomizer(ApplicationPropertyStorage applicationPropertyStorage){
        ApplicationPropertyStorage.Server server = applicationPropertyStorage.getServer();

        return httpServer ->
                httpServer
                        .httpRequestDecoder(
                                decoder -> {
                                    decoder.maxHeaderSize(
                                            server.getMaxHeaderSize()
                                    );
                                    decoder.maxInitialLineLength(
                                            server.getMaxInitialLineLength()
                                    );

                                    return decoder;
                                }
                        )
                        .option(
                                ChannelOption.RECVBUF_ALLOCATOR,
                                new FixedRecvByteBufAllocator(server.getBufferSize())
                        );
    }
}
