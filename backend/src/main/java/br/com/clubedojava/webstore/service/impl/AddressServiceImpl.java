package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.AddressDTO;
import br.com.clubedojava.webstore.exception.ResourceNotFoundException;
import br.com.clubedojava.webstore.exception.UnauthorizedException;
import br.com.clubedojava.webstore.model.Address;
import br.com.clubedojava.webstore.model.User;
import br.com.clubedojava.webstore.repository.AddressRepository;
import br.com.clubedojava.webstore.repository.UserRepository;
import br.com.clubedojava.webstore.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Suspendable
    public CompletableFuture<Set<AddressDTO>> findByUserId(Long userId, String currentUserEmail) {
        return CompletableFuture.supplyAsync(() -> {
            // Verifica se o usuário atual tem permissão para acessar esses endereços
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found with email: " + currentUserEmail));

            // Apenas o próprio usuário ou um admin pode ver os endereços
            if (!currentUser.getId().equals(userId) && !currentUser.hasRole("ROLE_ADMIN")) {
                throw new UnauthorizedException("You don't have permission to view these addresses");
            }

            return user.getAddresses().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toSet());
        });
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<AddressDTO> addAddress(Long userId, AddressDTO addressDTO, String currentUserEmail) {
        return CompletableFuture.supplyAsync(() -> {
            // Verifica se o usuário atual tem permissão para adicionar endereços
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found with email: " + currentUserEmail));

            // Apenas o próprio usuário ou um admin pode adicionar endereços
            if (!currentUser.getId().equals(userId) && !currentUser.hasRole("ROLE_ADMIN")) {
                throw new UnauthorizedException("You don't have permission to add addresses to this user");
            }

            // Se for o primeiro endereço ou for marcado como padrão, remove a marcação de padrão dos outros endereços
            if (user.getAddresses().isEmpty() || Boolean.TRUE.equals(addressDTO.getDefaultAddress())) {
                user.getAddresses().forEach(address -> address.setDefaultAddress(false));
            }

            // Se não for especificado, o primeiro endereço é sempre o padrão
            if (addressDTO.getDefaultAddress() == null) {
                addressDTO.setDefaultAddress(user.getAddresses().isEmpty());
            }

            // Se não for especificado, assume que é para ambos os tipos de endereço
            if (addressDTO.getAddressType() == null) {
                addressDTO.setAddressType("BOTH");
            }

            Address address = Address.builder()
                    .street(addressDTO.getStreet())
                    .number(addressDTO.getNumber())
                    .complement(addressDTO.getComplement())
                    .neighborhood(addressDTO.getNeighborhood())
                    .city(addressDTO.getCity())
                    .state(addressDTO.getState())
                    .zipCode(addressDTO.getZipCode())
                    .country(addressDTO.getCountry())
                    .defaultAddress(addressDTO.getDefaultAddress())
                    .addressType(addressDTO.getAddressType())
                    .build();

            user.addAddress(address);
            userRepository.save(user);

            return convertToDTO(address);
        });
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<AddressDTO> updateAddress(Long userId, Long addressId, AddressDTO addressDTO, String currentUserEmail) {
        return CompletableFuture.supplyAsync(() -> {
            // Verifica se o usuário atual tem permissão para atualizar endereços
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found with email: " + currentUserEmail));

            // Apenas o próprio usuário ou um admin pode atualizar endereços
            if (!currentUser.getId().equals(userId) && !currentUser.hasRole("ROLE_ADMIN")) {
                throw new UnauthorizedException("You don't have permission to update addresses for this user");
            }

            // Busca o endereço
            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

            // Verifica se o endereço pertence ao usuário
            if (!address.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("This address does not belong to the specified user");
            }

            // Se for marcado como padrão, remove a marcação de padrão dos outros endereços
            if (Boolean.TRUE.equals(addressDTO.getDefaultAddress()) && !address.getDefaultAddress()) {
                user.getAddresses().forEach(a -> {
                    if (!a.getId().equals(addressId)) {
                        a.setDefaultAddress(false);
                    }
                });
            }

            // Atualiza os campos
            address.setStreet(addressDTO.getStreet());
            address.setNumber(addressDTO.getNumber());
            address.setComplement(addressDTO.getComplement());
            address.setNeighborhood(addressDTO.getNeighborhood());
            address.setCity(addressDTO.getCity());
            address.setState(addressDTO.getState());
            address.setZipCode(addressDTO.getZipCode());
            address.setCountry(addressDTO.getCountry());

            if (addressDTO.getDefaultAddress() != null) {
                address.setDefaultAddress(addressDTO.getDefaultAddress());
            }

            if (addressDTO.getAddressType() != null) {
                address.setAddressType(addressDTO.getAddressType());
            }

            Address updatedAddress = addressRepository.save(address);
            return convertToDTO(updatedAddress);
        });
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<Void> deleteAddress(Long userId, Long addressId, String currentUserEmail) {
        return CompletableFuture.runAsync(() -> {
            // Verifica se o usuário atual tem permissão para excluir endereços
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found with email: " + currentUserEmail));

            // Apenas o próprio usuário ou um admin pode excluir endereços
            if (!currentUser.getId().equals(userId) && !currentUser.hasRole("ROLE_ADMIN")) {
                throw new UnauthorizedException("You don't have permission to delete addresses for this user");
            }

            // Busca o endereço
            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

            // Verifica se o endereço pertence ao usuário
            if (!address.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("This address does not belong to the specified user");
            }

            // Verifica se é o único endereço do usuário
            if (user.getAddresses().size() <= 1) {
                throw new IllegalStateException("Cannot delete the only address of the user");
            }

            // Se for o endereço padrão, define outro como padrão
            if (address.getDefaultAddress()) {
                user.getAddresses().stream()
                        .filter(a -> !a.getId().equals(addressId))
                        .findFirst()
                        .ifPresent(a -> {
                            a.setDefaultAddress(true);
                            addressRepository.save(a);
                        });
            }

            // Remove o endereço
            user.removeAddress(address);
            addressRepository.delete(address);
            userRepository.save(user);
        });
    }

    private AddressDTO convertToDTO(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .defaultAddress(address.getDefaultAddress())
                .addressType(address.getAddressType())
                .build();
    }
}