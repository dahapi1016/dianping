package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.hmdp.utils.SystemConstants.*;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {

        HttpSession session = request.getSession();
        Object user = session.getAttribute(CURRENT_USER);

        if(user == null) {
            response.setStatus(401);
            return false;
        }

        UserHolder.saveUser((UserDTO) user);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
