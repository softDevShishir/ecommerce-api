package com.shishir.ecommerce.user.wishlist.service;

import com.shishir.ecommerce.exception.BadRequestException;
import com.shishir.ecommerce.exception.ResourceNotFoundException;
import com.shishir.ecommerce.product.dto.ProductResponse;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.repository.ProductRepository;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.repository.UserRepository;
import com.shishir.ecommerce.user.wishlist.dto.WishlistResponse;
import com.shishir.ecommerce.user.wishlist.entity.Wishlist;
import com.shishir.ecommerce.user.wishlist.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository, UserRepository userRepository,
                            ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
                    return wishlistRepository.save(Wishlist.builder().user(user).build());
                });
    }

    public WishlistResponse addProductToWishlist(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found"));

        boolean alreadyInWishlist = wishlist.getProducts().stream()
                .anyMatch(existing -> existing.getId().equals(productId));
        if (alreadyInWishlist) {
            throw new BadRequestException("Product with id " + productId + " is already in the wishlist");
        }

        wishlist.getProducts().add(product);
        return toResponse(wishlistRepository.save(wishlist));
    }

    public WishlistResponse removeProductFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = requireWishlist(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found"));

        wishlist.getProducts().removeIf(existing -> existing.getId().equals(product.getId()));
        return toResponse(wishlistRepository.save(wishlist));
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        return toResponse(requireWishlist(userId));
    }

    public void clearWishlist(Long userId) {
        Wishlist wishlist = requireWishlist(userId);
        wishlist.getProducts().clear();
        wishlistRepository.save(wishlist);
    }

    @Transactional(readOnly = true)
    public boolean isProductInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductsId(userId, productId);
    }

    private Wishlist requireWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found for user id " + userId));
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        List<ProductResponse> products = wishlist.getProducts().stream().map(this::toProductResponse).toList();

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .productCount(products.size())
                .products(products)
                .createdAt(wishlist.getCreatedAt())
                .build();
    }

    private ProductResponse toProductResponse(Product product) {
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
