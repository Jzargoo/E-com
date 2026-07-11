package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.*;


public interface ProductService {
    ProductDetails getProductById(Long id) throws ProductNotFoundException;
    Long createProduct(CreateAndUpdateProductDetails createProductDetails);
    ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails, Integer shopId) throws ProductNotFoundException, ShopDoesNotOwnProductException, InvalidUpdateRequest;
    String deleteProduct(Long productId) throws ProductNotFoundException;

}
