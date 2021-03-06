package com.rackspacecloud.metrics.queryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * An interceptor that can be registered by a {@link WebMvcConfigurer} config bean via
 * {@link WebMvcConfigurer#addInterceptors(InterceptorRegistry)}. It will log a summary of
 * a request and its response to allow for easily locating within logs along with any trace IDs.
 */
@Slf4j
@Profile("development")
public class RequestLogging extends HandlerInterceptorAdapter {

  /* Additional unsafe logging */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    log.info("request url:{}", request.getRequestURI());
    log.info("remoteHost: {}", request.getRemoteHost());

    Enumeration<String> headerNames = request.getHeaderNames();
    String name;
    while (headerNames.hasMoreElements()) {
      name = headerNames.nextElement();
      log.info("header: {}:{}", name, request.getHeader(name));
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) throws Exception {
    log.info("Request completed: method={} path={}{} parameters={} status={}",
        request.getMethod(), request.getContextPath(), request.getServletPath(), request.getParameterMap(),
        response.getStatus());
  }
}
