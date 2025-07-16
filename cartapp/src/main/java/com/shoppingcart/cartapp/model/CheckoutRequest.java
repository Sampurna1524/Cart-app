package com.shoppingcart.cartapp.model;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Address address;
    private String paymentMethod;

    @Data
    public static class Address {
        private String line;
        private String city;
        private String state;
        private String zip;
    }
}
