package com.careerpilot.security;

import com.careerpilot.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;

    @JsonIgnore
    private final String password;

    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(
            Long id,
            String email,
            String password,
            boolean active,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.active = active;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                authorities
        );
    }

    // ----------------------------------------------------------------
    // UserDetails contract
    // ----------------------------------------------------------------

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // ----------------------------------------------------------------
    // Equality by ID
    // ----------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
