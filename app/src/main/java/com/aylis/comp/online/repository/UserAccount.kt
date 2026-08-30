package com.aylis.comp.online.repository

data class UserAccount(
    val id: String, // Google User ID or UUID
    val name: String, // Name of the profile
    val email: String?,
    val cookies: String,
    val sapisid: String?
)
