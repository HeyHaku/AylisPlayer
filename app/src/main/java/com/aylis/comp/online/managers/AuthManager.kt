package com.aylis.comp.online.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aylis.PlayerCore
import com.aylis.comp.online.repository.UserAccount
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object AuthManager {
    private const val PREFS_NAME = "ytm_auth_prefs_encrypted"
    private const val KEY_ACCOUNTS = "ytm_accounts"
    private const val KEY_ACTIVE_ACCOUNT_ID = "ytm_active_account_id"

    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val accountListType = Types.newParameterizedType(List::class.java, UserAccount::class.java)
    private val jsonAdapter = moshi.adapter<List<UserAccount>>(accountListType)

    private val _activeAccountFlow = MutableStateFlow<UserAccount?>(null)
    val activeAccountFlow: StateFlow<UserAccount?> = _activeAccountFlow.asStateFlow()

    init {
        // Initialize the active account on startup
        _activeAccountFlow.value = getActiveAccount()
    }

    private fun getPrefs(): SharedPreferences? {
        val context = PlayerCore.s().appContext ?: return null
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to regular SharedPreferences if encryption fails (e.g. keystore issues)
            context.getSharedPreferences("ytm_auth_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getAllAccounts(): List<UserAccount> {
        val prefs = getPrefs() ?: return emptyList()
        val json = prefs.getString(KEY_ACCOUNTS, null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            jsonAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getActiveAccount(): UserAccount? {
        val prefs = getPrefs() ?: return null
        val activeId = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null) ?: return null
        val accounts = getAllAccounts()
        return accounts.find { it.id == activeId }
    }

    fun setActiveAccount(id: String) {
        val prefs = getPrefs() ?: return
        val accounts = getAllAccounts()
        val account = accounts.find { it.id == id }
        if (account != null) {
            prefs.edit().putString(KEY_ACTIVE_ACCOUNT_ID, id).apply()
            _activeAccountFlow.value = account
        }
    }

    fun addOrUpdateAccount(account: UserAccount) {
        val prefs = getPrefs() ?: return
        val accounts = getAllAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.id == account.id }
        if (existingIndex >= 0) {
            accounts[existingIndex] = account
        } else {
            accounts.add(account)
        }
        val json = jsonAdapter.toJson(accounts)
        prefs.edit()
            .putString(KEY_ACCOUNTS, json)
            .putString(KEY_ACTIVE_ACCOUNT_ID, account.id)
            .apply()
        _activeAccountFlow.value = account
    }

    fun removeAccount(id: String) {
        val prefs = getPrefs() ?: return
        val accounts = getAllAccounts().toMutableList()
        val wasRemoved = accounts.removeIf { it.id == id }
        if (wasRemoved) {
            val json = jsonAdapter.toJson(accounts)
            val editor = prefs.edit().putString(KEY_ACCOUNTS, json)
            
            val activeAccount = getActiveAccount()
            if (activeAccount?.id == id) {
                // The removed account was active. Switch to another or logout.
                if (accounts.isNotEmpty()) {
                    val nextAccount = accounts.first()
                    editor.putString(KEY_ACTIVE_ACCOUNT_ID, nextAccount.id)
                    _activeAccountFlow.value = nextAccount
                } else {
                    editor.remove(KEY_ACTIVE_ACCOUNT_ID)
                    _activeAccountFlow.value = null
                }
            }
            editor.apply()
        }
    }

    fun getCookies(): String {
        return getActiveAccount()?.cookies ?: ""
    }

    fun getSapisid(): String? {
        return getActiveAccount()?.sapisid
    }

    fun isLoggedIn(): Boolean {
        val cookies = getCookies()
        return cookies.contains("SAPISID") || cookies.contains("__Secure-3PSID")
    }

    fun logout() {
        // Sets Guest Mode without removing accounts
        getPrefs()?.edit()?.remove(KEY_ACTIVE_ACCOUNT_ID)?.apply()
        _activeAccountFlow.value = null
    }
}
