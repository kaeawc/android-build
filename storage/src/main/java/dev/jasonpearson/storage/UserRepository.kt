package dev.jasonpearson.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserRepository(context: Context) {
  private val sharedPreferences: SharedPreferences =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val authRepository = AuthRepository(context)

  var onGuestModeProfileModificationAttempt: (() -> Unit)? = null

  fun clearUserData() {
    sharedPreferences.edit { clear() }
  }

  companion object {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PROFILE_IMAGE_URL = "profile_image_url"
    private const val KEY_CREATED_AT = "created_at"
  }
}
