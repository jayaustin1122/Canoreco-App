package com.example.canorecoapp.models

data class Notif (
    val title: String = "",
    val text: String = "",
    val timestamp : String =  "",
    val status : Boolean =  false,
    val isRead : Boolean =  false,
    val isFromDevice : Boolean =  false,


    ) {
    constructor() : this("", "","",false,false,false) {
        // Default constructor required for Firebase
    }
}