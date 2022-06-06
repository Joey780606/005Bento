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

    var uploadShopInfo = mutableMapOf<Int, Shop>()
    var uploadFoodInfo = mutableMapOf<Int, Food>()
    var dropdownMenuShop = ""

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

    fun findFood(): Map<Int, Food> {
        var foodInfo = mutableMapOf<Int, Food>()
        if(dropdownMenuShop == "")
            return foodInfo
        else {
            var shopID : Int
            for (info in uploadShopInfo) {
                if (info.value.name == dropdownMenuShop) {
                    shopID = info.key
                    for(food in uploadFoodInfo) {
                        if(food.value.shopId == shopID)
                            foodInfo[food.key] = food.value
                    }
                    break
                }
            }
            return foodInfo
        }
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

    fun parseUploadFile(fileContent: MutableList<FileStruct>) {
        uploadShopInfo.clear()
        uploadFoodInfo.clear()
        var shopCount = 0
        var foodCount = 0
        for(info in fileContent) {
            if(info.parse[1] == "0") {    //Shop
                val shopInfo = Shop(++shopCount, info.parse[2], info.parse[3], info.parse[4], info.parse[5])    //TODO("Change digit to meaningful text")
                uploadShopInfo[shopCount] = shopInfo
                Log.v("TEST", "Parse Shop: $shopCount, ${info.parse[2]}")
                //foodCount = 0
            } else {    //Food
                val foodInfo = Food(shopCount, foodCount, info.parse[1].toInt(), info.parse[2], info.parse[3], info.parse[4])
                uploadFoodInfo[foodCount++] = foodInfo
                Log.v("TEST", "Parse Food: $shopCount, $foodCount, ${info.parse[2]}")
            }
        }
    }

    fun findFoodInfo(label: String) {
        dropdownMenuShop = label
        _textFileContent.value?.let {
            Log.v("Test", "Info1")
            setTxtFileContent("$it ") } ?: Log.v("Test", "Info2")
    }

    companion object {
        val TEXT_EMAIL = 1
        val TEXT_PASSWORD = 2
        val TEXT_NICKNAME = 3
        val TEXT_PASSWORD_CONFIRM = 4
        val TEXT_NEW_ACCOUNT_EMAIL = 5
    }
}