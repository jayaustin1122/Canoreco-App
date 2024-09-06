package com.example.canorecoapp.models

data class News (
    val title: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val image: String = "",
    val timestamp : String =  "",
    val date : String =  "",
    val gawain : String =  "",
    val lugar : String =  "",
    val oras : String =  "",
    val petsa : String =  "",
    val category : String =  "",

) {
    constructor() : this("",  "","","","","",  "","","","","") {
        // Default constructor required for Firebase
    }
}