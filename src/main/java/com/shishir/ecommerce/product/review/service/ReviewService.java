package com.shishir.ecommerce.product.review.service;

import com.shishir.ecommerce.exception.DuplicateResourceException;
import com.shishir.ecommerce.exception.ResourceNotFoundException;
import com.shishir.ecommerce.exception.UnauthorizedException;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.repository.ProductRepository;
import com.shishir.ecommerce.product.review.dto.CreateReviewRequest;
import com.shishir.ecommerce.product.review.dto.ProductRatingResponse;
import com.shishir.ecommerce.product.review.dto.ReviewResponse;
import com.shishir.ecommerce.product.review.dto.UpdateReviewRequest;
import com.shishir.ecommerce.product.review.entity.ProductReview;
import com.shishir.ecommerce.product.review.repository.ProductReviewRepository;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ReviewService(ProductReviewRepository productReviewRepository, UserRepository userRepository,
                          ProductRepository productRepository) {
        this.productReviewRepository = productReviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id " + request.getProductId() + " not found"));

        productReviewRepository.findByProductIdAndUserId(request.getProductId(), userId)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "User with id " + userId + " already reviewed product id " + request.getProductId());
                });

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return toResponse(productReviewRepository.save(review));
    }

    public ReviewResponse updateReview(Long reviewId, Long userId, UpdateReviewRequest request) {
        ProductReview review = getReviewById(reviewId);
        verifyOwnership(review, userId);

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());

        return toResponse(productReviewRepository.save(review));
    }

    public void deleteReview(Long reviewId, Long userId) {
        ProductReview review = getReviewById(reviewId);
        verifyOwnership(review, userId);
        productReviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        requireProductExists(productId);
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductRatingResponse getProductRating(Long productId) {
        requireProductExists(productId);
        List<ProductReview> reviews = productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId);

        double averageRating = reviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            int starValue = star;
            long count = reviews.stream().filter(review -> review.getRating() == starValue).count();
            ratingDistribution.put(star, count);
        }

        return ProductRatingResponse.builder()
                .productId(productId)
                .averageRating(Math.round(averageRating * 10) / 10.0)
                .totalReviews(reviews.size())
                .ratingDistribution(ratingDistribution)
                .reviews(reviews.stream().map(this::toResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews(Long userId) {
        return productReviewRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductReview getReviewById(Long reviewId) {
        return productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with id " + reviewId + " not found"));
    }

    private void verifyOwnership(ProductReview review, Long userId) {
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Review with id " + review.getId() + " does not belong to user id " + userId);
        }
    }

    private void requireProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product with id " + productId + " not found");
        }
    }

    private ReviewResponse toResponse(ProductReview review) {
        User user = review.getUser();
        String userName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (userName.isEmpty()) {
            userName = user.getEmail();
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(user.getId())
                .userName(userName)
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
