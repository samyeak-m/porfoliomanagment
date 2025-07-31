package com.stockmanagment.porfoliomanagment.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ControllerAdvice
public class GlobalControllerAdvice {
    
    @ModelAttribute("currentURI")
    public String getCurrentURI() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getRequestURI();
        } catch (Exception e) {
            return "/"; // fallback
        }
    }
}