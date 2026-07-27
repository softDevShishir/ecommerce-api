package com.shishir.ecommerce.product.controller;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.product.dto.ProductCreateRequest;
import com.shishir.ecommerce.product.dto.ProductResponse;
import com.shishir.ecommerce.product.dto.ProductUpdateRequest;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Product Management", description = "Product catalog, search, and filtering")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(Routes.PRODUCTS)
    @Operation(summary = "Create product", description = "Create new product (ADMIN only)")
    @ApiResponse(responseCode = "201", description = "Product created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid product data")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        log.info("POST {} name={}", Routes.PRODUCTS, request.getName());
        Product product = productService.createProduct(request.getName(), request.getDescription(),
                request.getPrice(), request.getStockQuantity(), request.getCategory());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(product));
    }

    @GetMapping(Routes.PRODUCT_BY_ID)
    @Operation(summary = "Get product by ID", description = "Retrieve product details")
    @ApiResponse(responseCode = "200", description = "Product found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        log.info("GET {} id={}", Routes.PRODUCT_BY_ID, id);
        return ResponseEntity.ok(toResponse(productService.getProductById(id)));
    }

    /**
     * Applies at most one filter, in this precedence: name, then category,
     * then price range; falls back to every product if none are given.
     */
    @GetMapping(Routes.PRODUCTS)
    @Operation(summary = "Get all products", description = "Retrieve products with optional search and filtering")
    @Parameters({
            @Parameter(name = "name", description = "Search by product name"),
            @Parameter(name = "category", description = "Filter by category"),
            @Parameter(name = "minPrice", description = "Filter by minimum price"),
            @Parameter(name = "maxPrice", description = "Filter by maximum price")
    })
    @ApiResponse(responseCode = "200", description = "Products retrieved",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class))))
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
    @Operation(summary = "Update product", description = "Update product details (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Product updated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProductUpdateRequest request) {
        log.info("PUT {} id={}", Routes.PRODUCT_BY_ID, id);
        Product product = productService.updateProduct(id, request.getName(), request.getDescription(),
                request.getPrice(), request.getStockQuantity(), request.getCategory());
        return ResponseEntity.ok(toResponse(product));
    }

    @DeleteMapping(Routes.PRODUCT_BY_ID)
    @Operation(summary = "Delete product", description = "Delete product by ID (ADMIN only)")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE {} id={}", Routes.PRODUCT_BY_ID, id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private ProductResponse toResponse(Product product) {
        var ratingStats = productService.getRatingStats(product.getId());
        Double averageRating = ratingStats.getAverageRating();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .averageRating(averageRating == null ? 0.0 : Math.round(averageRating * 10) / 10.0)
                .totalReviews(ratingStats.getTotalReviews().intValue())
                .build();
    }
}
