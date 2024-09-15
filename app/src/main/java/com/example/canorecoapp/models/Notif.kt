package com.example.canorecoapp.models

data class Notif (
    val title: String = "",
    val text: String = "",
    val timestamp : String =  "",
    val status : Boolean =  false,


    ) {
    constructor() : this("", "","",false) {
        // Default constructor required for Firebase
    }
}