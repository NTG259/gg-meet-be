package com.meet.BE.utils.convert;

import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.UserResponseDTO;

public class UserConvert {
    public static UserResponseDTO convertToUserResponseDTO(User user) {
        UserResponseDTO rs = new UserResponseDTO();
        rs.setId(user.getId());
        rs.setFullName(user.getFullName());
        rs.setEmail(user.getEmail());
        rs.setAvatar(user.getAvatar());
        return rs;
    }
}
