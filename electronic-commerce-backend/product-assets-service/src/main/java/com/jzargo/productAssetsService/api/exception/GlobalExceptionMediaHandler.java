package com.jzargo.productAssetsService.api.exception;

import com.jzargo.productAssetsService.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionMediaHandler {

    @ExceptionHandler
    public Mono<ErrorResponse> handleAssetNotFoundException(AssetNotFoundException ex) {

        ErrorResponse.Builder builder = ErrorResponse.builder(
                ex, HttpStatus.NOT_FOUND, ex.getMessage()
        );

        return Mono.just(builder.build());
    }


    @ExceptionHandler
    public Mono<ErrorResponse> handleHttpServerErrorException(WebExchangeBindException ex) {

        ErrorResponse build = ErrorResponse.builder(
                ex, HttpStatus.BAD_REQUEST, ex.getMessage()
        ).build();

        return Mono.just(build);
    }

    @ExceptionHandler
    public Mono<ErrorResponse> handleException(Exception ex) {

        ErrorResponse build = ErrorResponse.builder(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error occurred."
        ).build();

        return Mono.just(build);
    }

    @ExceptionHandler
    public Mono<ErrorResponse> handleUnsupportedTypeExeption(UnsupportedContentType ex) {

        ErrorResponse build = ErrorResponse.builder(
                ex,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "There was provided invalid content type." + ex.getMessage()
        ).build();

        return Mono.just(build);
    }

    @ExceptionHandler
    public Mono<ErrorResponse> handleCannotAddMediaFileException(CannotAddMediaFileException ex) {

        ErrorResponse build = ErrorResponse.builder(
                ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()
        ).build();

        return Mono.just(build);
    }

    @ExceptionHandler
    public Mono<ErrorResponse> handleCreatedInFallbackException(CreatedInFallbackException ex){

        ErrorResponse build = ErrorResponse.builder(
                ex, HttpStatus.CREATED,
                "Media file was saved; however, changes would be applied after some amount of time due to internal server error"
        ).build();

        return Mono.just(build);
    }

    @ExceptionHandler
    public Mono<ErrorResponse> handleShopDoesNotOwnProductException(ShopDoesNotOwnProductException ex){

        ErrorResponse build = ErrorResponse.builder(
                ex, HttpStatus.BAD_REQUEST,
                "Shop does not own this product, thus changes cannot be applied! Please select one of a product that a shop owns")
                .build();

        return Mono.just(build);
    }


    @ExceptionHandler
    public Mono<ErrorResponse> handleIOException(IOException ex){

        ErrorResponse build = ErrorResponse.builder(
                ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()
                ).build();

        return Mono.just(build);

    }

}