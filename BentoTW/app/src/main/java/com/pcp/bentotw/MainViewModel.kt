package com.pcp.bentotw

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.io.File
import java.text.SimpleDateFormat

class MainViewModel: ViewModel() {
    val _loginUser = MutableLiveData<FirebaseUser>()
        val loginUser: LiveData<FirebaseUser> = _loginUser

    val _textFileContent = MutableLiveData<String>()
        val textFileContent: LiveData<String> = _textFileContent

    val _scheduleShop = MutableLiveData<String>("")
        val scheduleShop: LiveData<String> = _scheduleShop

    var _scheduleRefresh = MutableLiveData<Long>(0)
        val scheduleRefresh: LiveData<Long> = _scheduleRefresh
    var scheduleRefreshUpdateOK = 0

    val _pictureFieUri = MutableLiveData<Uri?>()
    val pictureFieUri: LiveData<Uri?> = _pictureFieUri

    var uploadShopInfo = mutableMapOf<Int, Shop>()
    var uploadFoodInfo = mutableMapOf<Int, Food>()
    var dropdownMenuShop = ""

    var dbShopInfo = mutableMapOf<Int, Shop>()
    var dbFoodInfo = mutableMapOf<Int, Food>()
    var dbScheduleInfo = mutableMapOf<String, Schedule>()

    var shopGetAmount = -1
    var foodGetAmount = -1

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

    fun setScheduleContent(content: String) {
        _scheduleShop.value = content
    }

    fun setScheduleRefresh(content: Long) {
        _scheduleRefresh.value = content
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

    fun saveShopFoodFromFile(firestore: FirebaseFirestore) {
        var bentoSite = firestore.collection("btShop")
        var foodAmount = 0
        var foodWriteSuccess = 0
        for(shop in uploadShopInfo) {
            val shopInfo = hashMapOf(
                "name" to shop.value.name,
                "tel" to shop.value.telephone,
                "addr" to shop.value.address,
                "memo" to shop.value.memo
            )
            bentoSite.document(shop.key.toString())
                .set(shopInfo)
                .addOnSuccessListener { docRefer ->
                    Log.v("TEST", "DB write shop OK: ${shop.value.name} ${docRefer.toString()}")
                }.addOnFailureListener { err ->
                    Log.v("TEST", "DB write shop Fail: $err")
                }
        }

        bentoSite = firestore.collection("btFood")
        foodAmount = uploadFoodInfo.size
        foodWriteSuccess = 0
        for(food in uploadFoodInfo) {
            val foodInfo = hashMapOf(
                "shopId" to food.value.shopId,
                "type" to food.value.type,
                "name" to food.value.name,
                "price" to food.value.price,
                "memo" to food.value.memo
            )
            bentoSite.document(food.key.toString())
                .set(foodInfo)
                .addOnSuccessListener { foodRefer ->
                    Log.v("TEST", "DB write food OK: ${food.value.name} ${foodRefer.toString()}")
                    if(++foodWriteSuccess == foodAmount) {
                        setTxtFileContent("Write data into firestore success!")
                        getDBInfo(firestore)
                    }
                }.addOnFailureListener { err ->
                    Log.v("TEST", "DB write food Fail: $err")
                }
        }

// 可以運作,但因不能刪除,所以先不要這樣用
//        val bentoSite = firestore.collection("bento").document("shop")
//        var foodSite = firestore.collection("bento").document("shop")
//        for(shop in uploadShopInfo) {
//            val shopInfo = hashMapOf(
//                "name" to shop.value.name,
//                "tel" to shop.value.telephone,
//                "addr" to shop.value.address,
//                "memo" to shop.value.memo
//            )
//            bentoSite.collection(shop.key.toString()).document(shop.value.name)
//                .set(shopInfo)
//                .addOnSuccessListener { docRefer ->
//                    foodSite = firestore.collection("bento").document("shop").collection(shop.key.toString()).document(shop.value.name)
//                    Log.v("TEST", "DB write shop OK: ${shop.value.name} ${docRefer.toString()}")
//                    for(food in uploadFoodInfo) {
//                        if(food.value.shopId == shop.value.id) {
//                            val foodInfo = hashMapOf(
//                                "shopId" to food.value.shopId,
//                                "type" to food.value.type,
//                                "name" to food.value.name,
//                                "price" to food.value.price,
//                                "memo" to food.value.memo
//                            )
//                            foodSite.collection(food.key.toString()).document(food.value.name)
//                                .set(foodInfo)
//                                .addOnSuccessListener { foodRefer ->
//                                    Log.v("TEST", "DB write food OK: ${food.value.name} ${foodRefer.toString()}")
//                                }.addOnFailureListener { err ->
//                                    Log.v("TEST", "DB write food Fail: $err")
//                                }
//                        }
//                        //foodSite.document()
//                    }
//                }.addOnFailureListener { err ->
//                    Log.v("TEST", "DB write shop Fail: $err")
//                }
//        }

    }

    fun deleteShopFood(firestore: FirebaseFirestore) {
        lateinit var value: QuerySnapshot
        var infoSite = firestore.collection("btShop")
        shopGetAmount = -1
        foodGetAmount = -1
        infoSite
            .get()
            .addOnSuccessListener { result ->
                Log.d("TEST", "get Shop 00: ${result.size()} ")
                shopGetAmount = result.size()
                if(shopGetAmount == 0 && foodGetAmount == 0)
                    saveShopFoodFromFile(firestore)
                for (document in result) {
                    Log.d("TEST", "get Shop: ${document.id} => ${document.data}")
                    infoSite.document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            if(--shopGetAmount == 0 && foodGetAmount == 0)
                                saveShopFoodFromFile(firestore)
                            Log.d("TEST", "del Shop ok! ${document.id} ($shopGetAmount,$foodGetAmount")
                        }
                        .addOnFailureListener { e -> Log.w("TEST", "del Shop fail", e) }
                }
                value = result
            }.addOnFailureListener { exception ->
                Log.w("TEST", "Error getting documents.", exception)
            }

        infoSite = firestore.collection("btFood")
        infoSite
            .get()
            .addOnSuccessListener { result ->
                Log.d("TEST", "get food 00: ${result.size()} ")
                foodGetAmount = result.size()
                if(shopGetAmount == 0 && foodGetAmount == 0)
                    saveShopFoodFromFile(firestore)
                for (document in result) {
                    Log.d("TEST", "get food: ${document.id} => ${document.data}")
                    infoSite.document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            if(shopGetAmount == 0 && --foodGetAmount == 0)
                                saveShopFoodFromFile(firestore)
                            Log.d("TEST", "del food ok! ${document.id} ($shopGetAmount,$foodGetAmount")
                        }
                        .addOnFailureListener { e -> Log.w("TEST", "del food fail", e) }
                }
                value = result
            }.addOnFailureListener { exception ->
                Log.w("TEST", "Error getting documents.", exception)
            }

    }

    fun getDBInfo(firestore: FirebaseFirestore) {
        getDBShopInfo(firestore)
        getDBFoodInfo(firestore)
        getDBScheduleInfo(firestore)

        var infoSite = firestore.collection("btShop")
        infoSite
            .get()
            .addOnSuccessListener { result ->
                Log.d("TEST", "get DB Shop 00: ${result.size()} ")
                for (document in result) {
                    //Log.d("TEST", "get DB Shop: ${document.id} => ${document.data} == ${document.data["name"]}")
                    dbShopInfo[document.id.toInt()] = Shop(id = document.id.toInt(), name = document.data["name"].toString(), telephone = document.data["tel"].toString(),
                      address = document.data["address"].toString(), memo = document.data["memo"].toString())
                }
            }.addOnFailureListener { exception ->
                Log.w("TEST", "Error getting documents.", exception)
            }
    }

    private fun getDBScheduleInfo(firestore: FirebaseFirestore) {
        dbScheduleInfo.clear()

        val infoSite = firestore.collection("btSchedule")
        infoSite
            .get()
            .addOnSuccessListener { result ->
                Log.d("TEST", "get Schedule 00: ${result.size()} ")
                dbScheduleInfo.clear()
                for (document in result) {
                    Log.d("TEST", "get Shop: ${document.id} => ${document.data}")
                    dbScheduleInfo[document.id] = Schedule(document.id, document.data["name"].toString(), document.data["setTime"] as Long, document.data["founder"] as String)
                }
            }.addOnFailureListener { exception ->
                Log.w("TEST", "Error getting documents.", exception)
            }
    }

    private fun getDBFoodInfo(firestore: FirebaseFirestore) {
        dbShopInfo.clear()

        val infoSite = firestore.collection("btFood")
        infoSite
            .get()
            .addOnSuccessListener { result ->
                Log.d("TEST", "get DB food 00: ${result.size()} ")
                foodGetAmount = result.size()
                for (document in result) {
                    Log.d("TEST", "get DB food: ${document.id} => ${document.data}")
                    dbFoodInfo[document.id.toInt()] = Food(id = document.id.toInt(), shopId = document.data["shopId"].toString().toInt(), type = document.data["type"].toString().toInt(),
                        name = document.data["name"].toString(), price = document.data["price"].toString(), memo = document.data["memo"].toString())
                }
            }.addOnFailureListener { exception ->
                Log.w("TEST", "Error getting documents.", exception)
            }

    }

    private fun getDBShopInfo(firestore: FirebaseFirestore) {
        dbShopInfo.clear()

        val infoSite = firestore.collection("btShop")
        infoSite
            .get()
            .addOnSuccessListener { result ->
                Log.d("TEST", "get DB Shop 00: ${result.size()} ")
                for (document in result) {
                    //Log.d("TEST", "get DB Shop: ${document.id} => ${document.data} == ${document.data["name"]}")
                    dbShopInfo[document.id.toInt()] = Shop(id = document.id.toInt(), name = document.data["name"].toString(), telephone = document.data["tel"].toString(),
                        address = document.data["address"].toString(), memo = document.data["memo"].toString())
                }
            }.addOnFailureListener { exception ->
                Log.w("TEST", "Error getting documents.", exception)
            }
    }

    fun setShop(dateValue: String, shopName: String, firestore: FirebaseFirestore, auth: FirebaseAuth) {
        val bentoSite = firestore.collection("btSchedule")
            val shopInfo = hashMapOf(
                "name" to shopName,
                "setTime" to System.currentTimeMillis(),
                "founder" to (auth.currentUser?.email ?: "")
            )
            bentoSite.document(dateValue.replace("/".toRegex() ,""))
                .set(shopInfo)
                .addOnSuccessListener { foodRefer ->
                    Log.v("TEST", "DB write shop OK: $shopName ${foodRefer.toString()}")

                    bentoSite
                        .get()
                        .addOnSuccessListener { result ->
                            Log.d("TEST", "get Schedule 00: ${result.size()} ")
                            dbScheduleInfo.clear()
                            for (document in result) {
                                Log.d("TEST", "get Shop: ${document.id} => ${document.data}")
                                dbScheduleInfo[document.id] = Schedule(document.id, document.data["name"].toString(), document.data["setTime"] as Long, document.data["founder"] as String)
                                scheduleRefreshUpdateOK = 1
                                setScheduleRefresh(System.currentTimeMillis())
                            }
                        }.addOnFailureListener { exception ->
                            Log.w("TEST", "Error getting documents.", exception)
                            scheduleRefreshUpdateOK = 3
                            setScheduleRefresh(System.currentTimeMillis())
                        }
                }.addOnFailureListener { err ->
                    Log.v("TEST", "DB write shop Fail: $err")
                    scheduleRefreshUpdateOK = 2
                }
    }

    fun getTodayFood(firestore: FirebaseFirestore): MutableMap<Int, Food> {
        var dbFoodResultInfo = mutableMapOf<Int, Food>()
        var shopId = -1
        var shop = getTodayShop()

        if(shop == "")
            return dbFoodResultInfo
        else {
            shopId = getShopIdFromName(shop)
            if(shopId == -1)
                return dbFoodResultInfo
            else {
                for(info in dbFoodInfo) {
                    if(info.value.shopId == shopId) {
                        dbFoodResultInfo[info.key] = info.value
                    }
                }
                return dbFoodResultInfo
            }
        }
    }

    private fun getShopIdFromName(shop: String): Int {
        for(info in dbShopInfo) {
            if(info.value.name == shop) {
                return info.key
            }
        }
        return -1
    }

    public fun getTodayShop(): String {
        val dateInfo = getDateTimeString(System.currentTimeMillis(), "yyyyMMdd")
        var shopName = ""
        for(info in dbScheduleInfo) {
            if(info.key == dateInfo) {
                shopName = info.value.name
                break;
            }
        }
        return shopName
    }

    fun getFileName(uriInfo: Uri, activity: MainActivity): String {
        when(uriInfo.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                Log.v("TEST", "Choice file2: ${getContentFileName(uriInfo, activity)}")
                getContentFileName(uriInfo, activity)?.let { stringInfo ->
                    return stringInfo
                }
                return ""
            }
            else -> {
                Log.v("TEST", "Choice file3: ${uriInfo.path?.let(::File)?.name}")
                uriInfo.path?.let(::File)?.let{ fileInfo ->
                    return fileInfo.name
                }
                return ""
            }
        }
    }

    //Important: Need to learning.
    private fun getContentFileName(uri: Uri, activity: MainActivity): String? = runCatching {
        activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()

    private fun getDateTimeString(currentTimeMillis: Long, dateInfo: String): String {
        val sdFormatter = SimpleDateFormat(dateInfo)
        return sdFormatter.format(currentTimeMillis)
    }

    fun setReceivePictureUri(uriInfo: Uri?) {
        _pictureFieUri.value = uriInfo
    }

    companion object {
        val TEXT_EMAIL = 1
        val TEXT_PASSWORD = 2
        val TEXT_NICKNAME = 3
        val TEXT_PASSWORD_CONFIRM = 4
        val TEXT_NEW_ACCOUNT_EMAIL = 5
    }
}