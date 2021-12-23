package com.simplemobiletools.contacts.pro.models

data class SimContact(
    val id: Int,
    val name: String,
    val phoneNumber: String,
) {
    val hasPhone: Boolean
        get() = phoneNumber.isNotEmpty()

    val hasName: Boolean
        get() = name.isNotEmpty()
}
