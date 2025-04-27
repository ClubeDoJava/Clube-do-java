package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.AddressDTO;
import br.com.clubedojava.webstore.service.impl.Suspendable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface AddressService {

    @Suspendable
    CompletableFuture<Set<AddressDTO>> findByUserId(Long userId, String currentUserEmail);

    @Suspendable
    CompletableFuture<AddressDTO> addAddress(Long userId, AddressDTO addressDTO, String currentUserEmail);

    @Suspendable
    CompletableFuture<AddressDTO> updateAddress(Long userId, Long addressId, AddressDTO addressDTO, String currentUserEmail);

    @Suspendable
    CompletableFuture<Void> deleteAddress(Long userId, Long addressId, String currentUserEmail);
}