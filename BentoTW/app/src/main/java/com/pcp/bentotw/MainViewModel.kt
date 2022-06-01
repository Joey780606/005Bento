package com.pcp.bentotw

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainViewModel: ViewModel() {
    val _loginUser = MutableLiveData<FirebaseUser>()
        val loginUser: LiveData<FirebaseUser> = _loginUser

    val _textFileContent = MutableLiveData<String>()
    val textFileContent: LiveData<String> = _textFileContent

    fun updateUserStatus(auth: FirebaseAuth) {
        _loginUser.value = auth.currentUser
        Log.v("Test", "updateUserStatus ${auth.currentUser?.email}" )
    }

    fun authStateListener(auth: FirebaseAuth) {
        auth.addAuthStateListener {
            updateUserStatus(auth)
        }
    }

    fun findShop(id: Int): Map<Int, Shop> {
        //val shopInfo: MutableMap<String, Shop>? = null
        //val foodInfo: MutableMap<String, Food>

        val shopInfo = Shop(1, "一鴻燒臘", "25569779", "台北市大同區南京西路232號", "")
        return mapOf(1 to shopInfo) //重要, mapOf的用法
    }

    fun findFood(shopId: Int): List<Map<Int, Food>> {
        val foodList = listOf(mapOf(1 to Food(1,1,1,"11飯", "100", "aaa")),
            mapOf(2 to Food(1,2,2,"21麵", "90", "bbb")),
            mapOf(3 to Food(1,3,3,"31豆干", "80", "ccc")),
            mapOf(4 to Food(1,4,4,"41貢丸湯", "70", "ddd")),
            mapOf(5 to Food(1,5,1,"12飯糰", "60", "eee")),
            mapOf(6 to Food(2,6,2,"22水餃", "50", "fff")),    //前面都同一家店,只有這家是另一家店
        )

        var foodInfo: MutableList<Map<Int, Food>> = mutableListOf() //重要,這裡不能用list,因為無法增加值,要用mutableListOf
        var count = 1
        for(info in foodList) {
            Log.v("TEST", "food info 0000: $count ${foodInfo.size} ${foodList.size}")
            info[count++]?.let { food ->
                Log.v("TEST", "food info 0001: $count ${food.shopId} $shopId")
                if(food.shopId == shopId) {
                    foodInfo.add(info)
                }
            }
        }
        return foodInfo
    }

    fun setTxtFileContent(content: String) {
        _textFileContent.value = content
    }

    companion object {
        val TEXT_EMAIL = 1
        val TEXT_PASSWORD = 2
        val TEXT_NICKNAME = 3
        val TEXT_PASSWORD_CONFIRM = 4
        val TEXT_NEW_ACCOUNT_EMAIL = 5
    }
}