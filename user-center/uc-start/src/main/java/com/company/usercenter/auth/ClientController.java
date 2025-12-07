package com.company.usercenter.auth;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.ClientRegistrationRequest;
import com.company.usercenter.api.dto.ClientRegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientApplicationService clientApplicationService;

    public ClientController(ClientApplicationService clientApplicationService) {
        this.clientApplicationService = clientApplicationService;
    }

    @PostMapping
    public ApiResponse<ClientRegistrationResponse> register(@Valid @RequestBody ClientRegistrationRequest request) {
        var client = clientApplicationService.register(request);
        var response = new ClientRegistrationResponse(
                java.util.UUID.fromString(client.getId()),
                client.getClientId(),
                java.util.List.copyOf(client.getRedirectUris()),
                java.util.List.copyOf(client.getScopes())
        );
        return ApiResponse.created(response);
    }
}
