package br.com.clubedojava.webstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @Pattern(regexp = "^[0-9]{11,14}$", message = "CPF/CNPJ must contain only numbers with 11 to 14 digits")
    private String cpfCnpj;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone must contain only numbers with 10 to 11 digits")
    private String phone;

    @Valid
    private AddressDTO address;
}
