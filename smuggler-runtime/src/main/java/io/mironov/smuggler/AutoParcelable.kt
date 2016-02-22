package io.mironov.smuggler

import android.os.Parcel
import android.os.Parcelable
import java.util.IdentityHashMap

interface AutoParcelable : Parcelable {
  companion object {
    private val CREATORS = IdentityHashMap<Class<*>, Parcelable.Creator<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <S : AutoParcelable> creator(clazz: Class<S>): Parcelable.Creator<S> {
      return synchronized(CREATORS) {
        CREATORS.getOrPut(clazz) {
          Parcelable.Creator::class.java.cast(clazz.getDeclaredField("CREATOR").get(null))
        }
      } as Parcelable.Creator<S>
    }

    inline fun <reified S : AutoParcelable> creator(): Parcelable.Creator<S> {
      return creator(S::class.java)
    }
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    throw UnsupportedOperationException("This method will never be used in runtime. The real implementation will be generated by smuggler plugin.")
  }

  override fun describeContents(): Int {
    throw UnsupportedOperationException("This method will never be used in runtime. The real implementation will be generated by smuggler plugin.")
  }
}
