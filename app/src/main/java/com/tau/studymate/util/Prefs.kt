package com.tau.studymate.util

import android.content.Context
import android.content.SharedPreferences

/** App settings (profile, onboarding, schedule name). Included in Android backup. */
fun Context.appPrefs(): SharedPreferences = getSharedPreferences("tau_studymate_prefs", Context.MODE_PRIVATE)

/** Secrets such as the Moodle iCal link (contains a personal token). Excluded from backup in res/xml. */
fun Context.secretPrefs(): SharedPreferences = getSharedPreferences("tau_studymate_secret", Context.MODE_PRIVATE)
