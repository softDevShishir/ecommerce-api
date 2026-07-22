package com.shishir.ecommerce.product.controller;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.product.dto.ProductCreateRequest;
import com.shishir.ecommerce.product.dto.ProductResponse;
import com.shishir.ecommerce.product.dto.ProductUpdateRequest;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(Routes.PRODUCTS)
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        log.info("POST {} name={}", Routes.PRODUCTS, request.getName());
        Product product = productService.createProduct(request.getName(), request.getDescription(),
                request.getPrice(), request.getStockQuantity(), request.getCategory());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(product));
    }

    @GetMapping(Routes.PRODUCT_BY_ID)
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        log.info("GET {} id={}", Routes.PRODUCT_BY_ID, id);
        return ResponseEntity.ok(toResponse(productService.getProductById(id)));
    }

    /**
     * Applies at most one filter, in this precedence: name, then category,
     * then price range; falls back to every product if none are given.
     */
    @GetMapping(Routes.PRODUCTS)
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        log.info("GET {} name={} category={} minPrice={} maxPrice={}",
                Routes.PRODUCTS, name, category, minPrice, maxPrice);

        List<Product> products;
        if (name != null) {
            products = productService.searchByName(name);
        } else if (category != null) {
            products = productService.getProductsByCategory(category);
        } else if (minPrice != null && maxPrice != null) {
            products = productService.getProductsByPriceRange(minPrice, maxPrice);
        } else {
            products = productService.getAllProducts();
        }

        return ResponseEntity.ok(products.stream().map(this::toResponse).toList());
    }

    @PutMapping(Routes.PRODUCT_BY_ID)
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProductUpdateRequest request) {
        log.info("PUT {} id={}", Routes.PRODUCT_BY_ID, id);
        Product product = productService.updateProduct(id, request.getName(), request.getDescription(),
                request.getPrice(), request.getStockQuantity(), request.getCategory());
        return ResponseEntity.ok(toResponse(product));
    }

    @DeleteMapping(Routes.PRODUCT_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE {} id={}", Routes.PRODUCT_BY_ID, id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
