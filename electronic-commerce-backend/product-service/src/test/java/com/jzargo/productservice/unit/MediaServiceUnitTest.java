package com.jzargo.productservice.unit;


import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.PlainFile;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.client.MediaServiceClientImpl;
import com.jzargo.productservice.service.InternalMediaServiceImpl;
import com.jzargo.protobuf.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class MediaServiceUnitTest {

    static private final Long PRODUCT_ID = 1L;
    static private final Integer SHOP_ID = 1;

    Product product = Product.builder().build();

    @Mock
    private MediaServiceClientImpl mediaServiceClient;

    @InjectMocks
    private InternalMediaServiceImpl mediaService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    public void setup(){

        product.setId(PRODUCT_ID);
        product.setShopId(SHOP_ID);

        Mockito.when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(Optional.of(product));

    }

    @Test
    public void addImage_test_success(){

        byte[] content = {5,4,3,2,1};

        String image = "image1.png";

        PlainFile plainFile =
                new PlainFile(new ByteArrayInputStream(content), ContentType.PNG, (long) content.length);

        MultipartFile multipartFile =
                new MockMultipartFile(
                        "image1.png",
                        image,
                        "image/png",
                        content
                );


        try {
            Mockito.when(
                    mediaServiceClient.sendFile(plainFile)
            ).thenReturn(
                    image
            );

            mediaService.addMediaContent(multipartFile, PRODUCT_ID, SHOP_ID);

            Assertions.assertEquals(image, product.getMediaContent());

            Mockito.verify(
                    mediaServiceClient,
                    Mockito.times(1)
            ).sendFile( any() );

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);


        } catch (ProductNotFoundException | ShopDoesNotOwnProductException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

    @Test
    public void addAvatar_test_success() throws ShopDoesNotOwnProductException {
        byte[] content= {1,2,3};

        var image = "image.png";

        var plainFile = new PlainFile(new ByteArrayInputStream(content), ContentType.PNG, (long) content.length);

        try {
            Mockito.when(
                    mediaServiceClient.sendFile(plainFile)
            ).thenReturn(image);

            mediaService.addAvatar(
                    new MockMultipartFile(
                            "image1.png",
                            image,
                            "image/png",
                            content
                    ),
                    PRODUCT_ID, SHOP_ID
            );

            Assertions.assertEquals(image, product.getAvatar());

            Mockito.verify(
                    mediaServiceClient,
                    Mockito.times(1)
            ).sendFile(
                    plainFile
            );

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);


        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

    @Test
    public void getAvatar_test_success(){

        var avatarName = "image.png";
        product.setAvatar(avatarName);

        byte[] content = {1,2};

        try {

            Mockito.when(
                    mediaServiceClient.receiveFile(avatarName)
            ).thenReturn(
                    new PlainFile(
                            new ByteArrayInputStream(content),
                            ContentType.PNG,
                            (long) content.length
                    )
            );

            PlainFile avatar = mediaService.getAvatar(PRODUCT_ID);

            Assertions.assertArrayEquals(content, avatar.getContent().readAllBytes(), "Content of the avatar does not match");

            Mockito.verify(
                    mediaServiceClient,
                    Mockito.times(1)
            ).receiveFile(avatarName);

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);


        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

    @Test
    public void getImages_test_success(){

        List<byte[]> contents = List.of(
                new byte[]{5, 4, 3, 2, 1},
                new byte[]{1, 2, 3, 4, 5}
        );

        List<String> media = List.of("image1.png", "video1.mp4");

        product.addMedia(media.getFirst());
        product.addMedia(media.get(1));

        List<PlainFile> listMockFiles = List.of(
                new PlainFile(
                        new ByteArrayInputStream( contents.getFirst() ), ContentType.PNG, (long) contents.getFirst().length),
                new PlainFile(
                        new ByteArrayInputStream( contents.get(1) ), ContentType.MP4, (long) contents.get(1).length)
        );

        try {

            Mockito.when(
                    mediaServiceClient.receiveFiles(media)
            ).thenReturn(listMockFiles);

            List<PlainFile> returnedImages = mediaService.getMediaContent(PRODUCT_ID);

            Assertions.assertEquals(
                    returnedImages.size(),
                    listMockFiles.size(),
                    "The returned number of images does not match with actual number"
            );

            for (int i = 0; i < returnedImages.size(); i++) {
                Assertions.assertArrayEquals(contents.get(i), returnedImages.get(i).getContent().readAllBytes(), "Content of the images does not match");
            }

            Mockito.verify(
                    mediaServiceClient,
                    Mockito.times(1)
            ).receiveFiles(media);

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);

        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

}
