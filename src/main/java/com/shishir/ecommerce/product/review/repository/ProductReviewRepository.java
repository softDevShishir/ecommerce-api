package com.shishir.ecommerce.product.review.repository;

import com.shishir.ecommerce.product.review.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByUserId(Long userId);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    void deleteByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) AS averageRating, COUNT(r) AS totalReviews "
            + "FROM ProductReview r WHERE r.product.id = :productId")
    RatingStats getRatingStats(@Param("productId") Long productId);

    // Aggregate projection so product list/detail responses can attach rating
    // data with a single query per product instead of loading every review.
    interface RatingStats {
        Double getAverageRating();
        Long getTotalReviews();
    }
}
