package com.pcp.bentotw

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainViewModel: ViewModel() {
    val _loginUser = MutableLiveData<FirebaseUser>()
        val loginUser: LiveData<FirebaseUser> = _loginUser

    fun updateUserStatus(auth: FirebaseAuth) {
        _loginUser.value = auth.currentUser
    }

    fun authStateListener(auth: FirebaseAuth) {
        auth.addAuthStateListener {
            updateUserStatus(auth)
        }
    }

    companion object {
        val TEXT_EMAIL = 1
        val TEXT_PASSWORD = 2
        val TEXT_NICKNAME = 3
        val TEXT_PASSWORD_CONFIRM = 4
        val TEXT_NEW_ACCOUNT_EMAIL = 5
    }
}