package com.popcorn.common.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

data class PassportPrincipal(
    val userId: UUID,
    val email: String,
    private val userAuthorities: Collection<GrantedAuthority> = emptyList()
) : UserDetails {

    override fun getUsername(): String = email
    override fun getPassword(): String = ""
    override fun getAuthorities(): Collection<GrantedAuthority> = userAuthorities
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}