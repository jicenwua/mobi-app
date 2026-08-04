package com.xcz.member.user.service;

import com.xcz.member.user.domain.dto.LoginDTO;

public interface SysLoginService{

    String login(LoginDTO loginDTO) throws IllegalAccessException;

    boolean logout() throws IllegalAccessException;

}
