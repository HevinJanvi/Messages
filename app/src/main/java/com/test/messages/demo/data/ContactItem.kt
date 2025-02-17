package com.test.messages.demo.data

import android.os.Parcel
import android.os.Parcelable

data class ContactItem(
    val cid: String?,
    val name: String?,
    val phoneNumber: String,
    val normalizeNumber: String,
    val profileImageUrl: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    ) {
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ContactItem)?.phoneNumber == this.phoneNumber
    }

    override fun hashCode(): Int {
        return phoneNumber.hashCode()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cid)
        parcel.writeString(name)
        parcel.writeString(phoneNumber)
        parcel.writeString(normalizeNumber)
        parcel.writeString(profileImageUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ContactItem> {
        override fun createFromParcel(parcel: Parcel): ContactItem {
            return ContactItem(parcel)
        }

        override fun newArray(size: Int): Array<ContactItem?> {
            return arrayOfNulls(size)
        }
    }
}