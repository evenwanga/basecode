package com.company.usercenter.auth;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.ClientRegistrationRequest;
import com.company.usercenter.api.dto.ClientRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "OAuth2 客户端管理", description = "OAuth2/OIDC 客户端应用（Client Application）管理接口。客户端是指需要接入用户中心进行身份认证的业务系统，如 SPA 单页应用、移动 App、后端服务等。")
public class ClientController {

    private final ClientApplicationService clientApplicationService;

    public ClientController(ClientApplicationService clientApplicationService) {
        this.clientApplicationService = clientApplicationService;
    }

    @Operation(
            summary = "注册 OAuth2 客户端",
            description = """
                    注册一个新的 OAuth2 客户端应用。注册后，客户端可使用返回的 clientId 和配置的 clientSecret 进行 OAuth2 授权流程。
                    
                    **支持的授权模式：**
                    - Authorization Code + PKCE：适用于 SPA、移动端
                    - Authorization Code：适用于传统 Web 应用
                    - Client Credentials：适用于服务间调用（M2M）
                    - Refresh Token：刷新访问令牌
                    
                    **重要提示：**
                    - clientId 必须唯一
                    - clientSecret 创建后请妥善保管，系统不会再次显示
                    - redirectUris 必须与实际回调地址完全匹配（包括协议、域名、端口、路径）
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "客户端注册成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "clientId 已存在")
    })
    @PostMapping
    public ApiResponse<ClientRegistrationResponse> register(
            @Parameter(description = "客户端注册请求体", required = true)
            @Valid @RequestBody ClientRegistrationRequest request) {
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
