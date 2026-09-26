package com.opspilot.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理员用户治理 HTTP 入口；路径整体由服务端 RBAC 限定为 ADMIN。 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdministrationController {

    private final UserAdministrationService userAdministrationService;

    public UserAdministrationController(UserAdministrationService userAdministrationService) {
        this.userAdministrationService = userAdministrationService;
    }

    /** 创建用户并返回不含凭据的 201 响应。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponse create(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return userAdministrationService.create(request, authentication, servletRequest);
    }

    /** 返回最近创建的用户清单。 */
    @GetMapping
    public List<AppUserResponse> list() {
        return userAdministrationService.list();
    }
}
