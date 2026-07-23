package com.amol.microservices.cbs.domain;

/** A bank customer. KYC is assumed verified upstream (API management / KYC layer). */
public record Customer(String id, String name, String email, String kycStatus) {
}
