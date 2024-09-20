package com.example.canorecoapp.models

import android.os.Parcel
import android.os.Parcelable

class Centers (
    val additionalMobile: String = "",
    val barangay: String = "",
    val latitude: String = "",
    val locationName: String = "",
    val longitude : String =  "",
    val mobile : String =  "",
    val municipality : String =  "",
    val street : String =  "",
    val unit : String =  "",


    ): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString()
    ) {
    }

    constructor() : this("","","","",  "","","","","") {
        // Default constructor required for Firebase
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(additionalMobile)
        parcel.writeString(barangay)
        parcel.writeString(latitude)
        parcel.writeString(locationName)
        parcel.writeString(longitude)
        parcel.writeString(mobile)
        parcel.writeString(municipality)
        parcel.writeString(street)
        parcel.writeString(unit)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Centers> {
        override fun createFromParcel(parcel: Parcel): Centers {
            return Centers(parcel)
        }

        override fun newArray(size: Int): Array<Centers?> {
            return arrayOfNulls(size)
        }
    }
}