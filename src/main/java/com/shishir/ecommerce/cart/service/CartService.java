package com.shishir.ecommerce.cart.service;

import com.shishir.ecommerce.cart.entity.Cart;
import com.shishir.ecommerce.cart.entity.CartItem;
import com.shishir.ecommerce.cart.repository.CartItemRepository;
import com.shishir.ecommerce.cart.repository.CartRepository;
import com.shishir.ecommerce.exception.BadRequestException;
import com.shishir.ecommerce.exception.ResourceNotFoundException;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.repository.ProductRepository;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        UserRepository userRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Cart getCartByUserId(Long userId) {
        requireUser(userId);
        return requireCart(userId);
    }

    // Creates the cart on first item, and merges into the existing line if the product is already present.
    public Cart addItemToCart(Long userId, Long productId, Integer quantity) {
        User user = requireUser(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found"));

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product id " + productId);
        }

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        return cart;
    }

    public Cart removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = requireCart(userId);
        CartItem cartItem = requireCartItem(cart, cartItemId);
        cartItemRepository.delete(cartItem);
        return cart;
    }

    public Cart updateCartItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        Cart cart = requireCart(userId);
        CartItem cartItem = requireCartItem(cart, cartItemId);

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        if (cartItem.getProduct().getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product id " + cartItem.getProduct().getId());
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return cart;
    }

    public void clearCart(Long userId) {
        Cart cart = requireCart(userId);
        cartItemRepository.deleteAll(cartItemRepository.findByCartId(cart.getId()));
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Long userId) {
        Cart cart = requireCart(userId);
        return cartItemRepository.findByCartId(cart.getId());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
    }

    private Cart requireCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id " + userId));
    }

    private CartItem requireCartItem(Cart cart, Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item with id " + cartItemId + " not found in user's cart"));
    }
}
