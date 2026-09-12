package com.lims.module.system.controller;

import com.lims.common.core.Result;
import com.lims.common.security.SecurityUtils;
import com.lims.module.system.dto.LoginDTO;
import com.lims.module.system.dto.LoginVO;
import com.lims.module.system.dto.UserInfoVO;
import com.lims.module.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtils.currentUserId());
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.currentUserInfo(SecurityUtils.currentUserId()));
    }
}
