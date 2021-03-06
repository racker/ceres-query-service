/*
 * Copyright 2019 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspacecloud.metrics.queryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * This is a general purpose Spring Security filter which translates the given tenant/user and roles
 * headers into the equivalent values in a {@link PreAuthenticatedToken}
 */
@Slf4j
public class PreAuthenticatedFilter extends GenericFilterBean {

  private final String tenantHeader;
  private final List<String> rolesHeaders;

  public PreAuthenticatedFilter(String tenantHeader, List<String> rolesHeaders) {
    this.tenantHeader = tenantHeader;
    this.rolesHeaders = rolesHeaders;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain chain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      final HttpServletRequest req = (HttpServletRequest) servletRequest;
      Optional<PreAuthenticatedToken> token = getToken(req);
      if (token.isPresent()) {
        final PreAuthenticatedToken auth = token.get();
        log.debug("Processed Repose-driven authentication={}", auth);
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  Optional<PreAuthenticatedToken> getToken(HttpServletRequest req) {
    final Set<String> rolesSet = new HashSet<>();
    for (String header : rolesHeaders) {
      String roleString = req.getHeader(header);
      if (roleString != null) {
        List<String> roleValues = Arrays.asList(roleString.split(","));
        rolesSet.addAll(roleValues);
      }
    }
    final String tenant = req.getHeader(tenantHeader);

    if (!rolesSet.isEmpty() && (StringUtils.hasText(tenant))) {
      final List<SimpleGrantedAuthority> roles = rolesSet.stream()
          .map(role ->
              role
                  .replace(':', '_')
                  .replace('-', '_')
                  .toUpperCase()
          )
          .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
          .collect(toList());

      return Optional.of(new PreAuthenticatedToken(tenant, roles));
    } else {
      log.debug("Skipping PreAuthenticatedToken creation for tenant={}, roles={}", tenant, rolesSet);
      return Optional.empty();
    }
  }
}
