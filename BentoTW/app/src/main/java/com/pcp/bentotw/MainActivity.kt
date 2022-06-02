package com.pcp.bentotw

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.text.isDigitsOnly
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pcp.bentotw.MainActivity.Companion.DIALOG_CLOSE
import com.pcp.bentotw.MainActivity.Companion.DIALOG_CLOSE_BACK_PRIOR
import com.pcp.bentotw.MainActivity.Companion.DIALOG_OPEN
import com.pcp.bentotw.MainActivity.Companion.DIALOG_TYPE_OK
import com.pcp.bentotw.ui.theme.*
import kotlinx.coroutines.delay
import java.io.*

/*
    Arthur: Joey yang
    Use skill:
    1. Navigation.
    2. LaunchedEffect (Coroutine).
    3. Firebase analytics (Tools -> Firebase)
    4. Firebase auth
    5. ViewModel
    6. MutableLiveData, LiveData
    7. observeAsState
 */
const val SELECT_TEXT_FILE_ID: Int = 11
const val REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 102

class MainActivity : ComponentActivity() {
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private val mainViewModel by viewModels<MainViewModel>()

    private val requiredPermissions = object : ArrayList<String>() {
        init {
            add("android.permission.WRITE_EXTERNAL_STORAGE")
        }
    }
    
    private fun checkPermissionsThenInitSdk() {
        val requestedPermissions: MutableList<String> = ArrayList()
        for (requiredPermission in this.requiredPermissions) {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestedPermissions.add(requiredPermission)
            }
        }
        if (requestedPermissions.size == 0) {
        } else {
            ActivityCompat.requestPermissions(  // 重要
                this,
                requestedPermissions.toTypedArray(), REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SELECT_TEXT_FILE_ID -> if (data != null) {
                Log.v("TEST", "Choice file: ${data.data?.toString()}")
                if(resultCode == RESULT_OK) {
                    data.data?.let { uri ->
                        var fileContent = readTextFile(uri)
                        var displayInfo = ""

                        if(fileContent.size == 1 && fileContent[0].correct != PARSE_SUCCESS) {
                            when(fileContent[0].correct) {
                                PARSE_ERROR_FIELD_NEED_DIGIT -> displayInfo = "Error: Some fields need digit.  "
                                PARSE_ERROR_FIRST_NOT_RESTAURANT -> displayInfo = "Error: First line should be restaurant data.  "
                                PARSE_ERROR_FIELD_AMOUNT -> displayInfo = "Error: Wrong fields amount.  "
                                PARSE_ERROR_FIELD_OTHER -> displayInfo = "Error: Other.  "
                            }
                            displayInfo += "Line no: ${fileContent[0].lineNumber}\nContent: ${fileContent[0].content}"
                        } else {
                            displayInfo += "File data is correct."
                        }
                        this@MainActivity.mainViewModel.setTxtFileContent(displayInfo)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsThenInitSdk()
        setContent {
            analytics = Firebase.analytics
            auth = Firebase.auth
            BentoTWTheme {
                val navController = rememberNavController() //Navigation Step2

                NavHost(
                    navController = navController,
                    startDestination = "initial_screen"
                ) {
                    composable("initial_screen") {
                        //second screen
                        InitialScreen01(navController = navController, auth)
                    }
                    composable("login_screen") {
                        //first screen
                        LoginScreen02(navController = navController, auth, applicationContext, mainViewModel)
                    }
                    composable("new_user_screen") {
                        //first screen
                        NewUserScreen03(navController = navController, auth, applicationContext, mainViewModel)
                    }
                    composable("choice_food_screen") {
                        //first screen
                        ChoiceFood04(navController = navController, auth, applicationContext, mainViewModel)
                    }
                    composable("function_list") {
                        //first screen
                        FunctionList05(navController = navController, auth, applicationContext, mainViewModel)
                    }
                    composable("shop_txt_process") {
                        ShopTxtProcess08(navController = navController, applicationContext, this@MainActivity, mainViewModel)
                    }
                }
            }
        }
    }

    private fun readTextFile(uri: Uri): MutableList<FileStruct> {
        var sentenceList: MutableList<String> = ArrayList() //重要,宣告方式
        var parseInfo: MutableList<FileStruct> = ArrayList()

        var reader: BufferedReader? = null
        val builder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(contentResolver.openInputStream(uri)))
            var line: String? = ""
            while (reader.readLine().also { line = it } != null) {
                builder.append(line)
                line?.let { sentenceList.add(it) }
                builder.append(System.getProperty("line.separator"));   //重要,這樣才能把換行\r\n加進去
            }
            reader.close()
            var count = 0
            var parseFail = PARSE_SUCCESS
            for(info in sentenceList) {
                val dataSplit = info.split(",".toRegex())
                if(info.isNotEmpty()) {
                    if(dataSplit.size < 4)
                        parseFail = PARSE_ERROR_FIELD_AMOUNT
                    else if(!dataSplit[0].isDigitsOnly() || dataSplit[0].isEmpty())
                        parseFail = PARSE_ERROR_FIELD_NEED_DIGIT
                    else if(!dataSplit[1].isDigitsOnly() || dataSplit[1].isEmpty())
                        parseFail = PARSE_ERROR_FIELD_NEED_DIGIT
                    else if(!dataSplit[3].isDigitsOnly() || dataSplit[3].isEmpty())
                        parseFail = PARSE_ERROR_FIELD_NEED_DIGIT
                    else if(count == 0 && dataSplit[1] != "0")    //Line 1 but not shop type
                        parseFail = PARSE_ERROR_FIRST_NOT_RESTAURANT
                    else if(dataSplit[1] == "0" && dataSplit.size != 5 && dataSplit.size != 6)
                        parseFail = PARSE_ERROR_FIELD_AMOUNT
                    else if(dataSplit[1] != "0" && dataSplit.size != 4 && dataSplit.size != 5)
                        parseFail = PARSE_ERROR_FIELD_AMOUNT
                    val value = FileStruct(parseFail, count + 1, info, dataSplit)
                    if(parseFail != PARSE_SUCCESS)
                        parseInfo.clear()   //先清掉之前成功的資料,獨留有問題的資料
                    parseInfo.add(value)
                    Log.v("TEST", "parse Sentence: $info -> len= ${dataSplit.count()} ->$parseFail")
                    if(parseFail != PARSE_SUCCESS)
                        break;
                }
                count++
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return parseInfo
    }

    companion object {
        const val DIALOG_CLOSE = 0  //重要:AS建議要用 const val,但要查為什麼
        const val DIALOG_OPEN = 1
        const val DIALOG_CLOSE_BACK_PRIOR = 2

        const val DIALOG_TYPE_OK = 0

        const val TEXT_FILE_SAMPLE = "0,0,栢能餐廳,25569779,台北市大同區承德路一段70號,a.此欄備註(可不填) b.餐廳第二項數字皆填0 c.先放餐廳資料再放食物資料\n" +
                "0,1,脆皮鴨腿飯,140,備註(所有行第一項數字皆填0,系統用)\n" +
                "0,1,玫塊雞腿飯,120,備註(飯類第二項數字填1)\n" +
                "0,2,土魠魚焿米粉,70,麵食類第二項數字填2\n" +
                "0,3,滷獅子頭,30,小菜類第二項數字填3\n" +
                "0,4,香菇湯,60,湯類第二項數字填4\n" +
                "0,1,魯肉飯(小),25\n" +
                "0,1,魯肉飯(大),40\n" +
                "0,0,小麥小館,88888888,台北市大同區承德路一段70號,可放多筆資料(需保持店名在前菜色在後)\n" +
                "0,1,黯然消魂飯,500,每天限量一份\n" +
                "0,2,陽春麵,60\n"

        const val PARSE_SUCCESS = 0
        const val PARSE_ERROR_FIELD_NEED_DIGIT = 1
        const val PARSE_ERROR_FIRST_NOT_RESTAURANT = 2
        const val PARSE_ERROR_FIELD_AMOUNT = 3
        const val PARSE_ERROR_FIELD_OTHER = 4
    }
}

@Composable
fun InitialScreen01(navController: NavController, auth: FirebaseAuth) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(id = R.drawable.logo),
            contentDescription = "",
        )
        Text(text = stringResource(R.string.app_title),
            color = Green0091A7)
    }
    LaunchedEffect(true) {
        delay(2000)
        auth.currentUser?.let {
            navController.navigate("function_list")
        } ?: navController.navigate("login_screen")

    }
}

@Composable
fun LoginScreen02(navController: NavController, auth: FirebaseAuth, context: Context, mainViewModel: MainViewModel) {
    val openDialog = remember { mutableStateOf(DIALOG_CLOSE) } // = 與 by 的差異是, by是委托概念,就不需要加 .value 值了.
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme

    val loginUser by mainViewModel._loginUser.observeAsState(null)  // observeAsState need to implement: androidx.compose.runtime:runtime-livedata:

    var loginUserMail = ""
    var nickName = ""

    mainViewModel.authStateListener(auth)
    loginUser?.let { user ->
        user.email?.let { mail ->
            loginUserMail = mail
        }
        user.displayName?.let { name ->
            nickName = name
        }
    }
    if(loginUser == null)
        Log.v("Test", "LoginUser 00 = null")
    else
        Log.v("Test", "LoginUser 01 = ${loginUser!!.toString()},  ${loginUser!!.email}")
    //TODO("Need to modify UI more beautiful")
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.login_title),
            color = Green0091A7,
            modifier = Modifier.clickable(onClick = {
                navController.navigate("login_screen")
            })
        )
        TextFieldShow(MainViewModel.TEXT_EMAIL, emailText, 0.8f) { info -> emailText = info }
        TextFieldShow(MainViewModel.TEXT_PASSWORD, passwordText, 0.8f) { info -> passwordText = info }
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center) {
            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .alpha(if (emailText.isEmpty()) 0f else 100f),   //讓元件消失的方法
                //enabled = false,
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                        if (emailText.isEmpty()) {
                            Toast.makeText(context, context.getText(R.string.enter_email), Toast.LENGTH_LONG).show()    //注意,文字要這樣使用
                            return@Button
                        }
                        auth.sendPasswordResetEmail(emailText).addOnCompleteListener {  task ->
                            task.addOnSuccessListener {
                                Toast.makeText(context, context.getText(R.string.send_reset_pwd_email_ok), Toast.LENGTH_LONG).show()
                            }
                            task.addOnFailureListener { error ->
                                Toast.makeText(context, context.getText(R.string.send_reset_pwd_email_fail).toString() + error.localizedMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                })
            {
                Text(text = stringResource(R.string.forget_password))
            }

            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier.fillMaxWidth(0.5f),
                //enabled = false,
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                    if (loginUserMail == "") {
                        if (emailText.isEmpty()) {
                            Toast.makeText(context, "Please enter account", Toast.LENGTH_LONG)
                                .show()
                            return@Button
                        }
                        if (passwordText.isEmpty()) {
                            Toast.makeText(context, "Please enter password", Toast.LENGTH_LONG)
                                .show()
                            return@Button
                        }
                        auth.signInWithEmailAndPassword(emailText, passwordText)
                            .addOnFailureListener { exception ->
                                Toast.makeText(
                                    context,
                                    "Login error: ${exception.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.addOnSuccessListener { result ->
                                result.user?.let { userInfo ->
                                    if (!userInfo.isEmailVerified) {
                                        userInfo.sendEmailVerification().addOnSuccessListener {
                                            openDialog.value = DIALOG_OPEN
                                            Log.v("TEST", "verify 02: Send verification")
                                        }.addOnFailureListener { error ->
                                            Toast.makeText(
                                                context,
                                                "Account create success but fail verification! error: ${error.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        auth.signOut()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Already sign and verify, go to next",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.navigate("function_list")
                                    }
                                }
                            }
                    } else {
                        auth.signOut()
                    }
                })
            {
                Text(text = if (loginUserMail == "") "Login" else "Logout")
            }
        }

        Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
            //modifier = Modifier.fillMaxHeight(0.5f),
            //enabled = false,
            enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
            interactionSource = interactionSourceTest,
            elevation = ButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
                disabledElevation = 2.dp
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(5.dp, color = borderColor),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color.Red),
            contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
            onClick = { navController.navigate("new_user_screen") }) {
            Text(text = "Create new account") }

        Text(text = if(loginUserMail == "") "Status: not login" else "Status: $loginUserMail login, nick name = $nickName")
        Text(text = "order", modifier = Modifier.clickable { navController.navigate("choice_food_screen") })
    }

    when (openDialog.value) {
        DIALOG_OPEN -> {
            DialogShow(stringResource(R.string.success), stringResource(R.string.send_confirm_mail), DIALOG_TYPE_OK) { status -> openDialog.value = status }
        }
    }
}

@Composable
fun NewUserScreen03(navController: NavController, auth: FirebaseAuth, context: Context, mainViewModel: MainViewModel) {
    /*
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_title),
            color = Green0091A7)
    } */
    var emailText by remember { mutableStateOf("") }
    var nickNameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordConfirmText by remember { mutableStateOf("") }

    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.Blue31B6FB

    Log.v("Test", "NewUserScreen03 in")
    //TODO("Need to modify UI more beautiful")
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.create_new_account),
            modifier = Modifier.clickable(onClick = {
                navController.navigate("login_screen")
            })
        )
        TextFieldShow(MainViewModel.TEXT_NEW_ACCOUNT_EMAIL , emailText, 0.8f) { info -> emailText = info }
        TextFieldShow(MainViewModel.TEXT_NICKNAME, nickNameText, 0.8f) { info -> nickNameText = info }
        TextFieldShow(MainViewModel.TEXT_PASSWORD, passwordText, 0.8f) { info -> passwordText = info }
        TextFieldShow(MainViewModel.TEXT_PASSWORD_CONFIRM, passwordConfirmText, 0.8f) { info -> passwordConfirmText = info }
        Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
            //modifier = Modifier.fillMaxHeight(0.5f),
            //enabled = false,
            enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
            interactionSource = interactionSourceTest,
            elevation = ButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
                disabledElevation = 2.dp
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(5.dp, color = borderColor),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color.Red),
            contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
            onClick = {
                if(emailText.isEmpty()) {
                    Toast.makeText(context, "Please enter account", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if(nickNameText.isEmpty()) {
                    Toast.makeText(context, "Please enter nickName", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if(passwordText != passwordConfirmText) {
                    Toast.makeText(context, "Password not match", Toast.LENGTH_LONG).show()
                    return@Button
                }
                auth.createUserWithEmailAndPassword(emailText, passwordText).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Account Created!", Toast.LENGTH_LONG).show()
                        var user = auth.currentUser
                        var request = UserProfileChangeRequest.Builder().setDisplayName(nickNameText).build()

                        user?.let { userInfo ->
                            userInfo.updateProfile(request)
                            auth.signOut()  // 預設一旦建立帳號,即使用者,但因要考量認證流程,所以先不要直接用,讓他先登出
                        }
                        navController.navigate("login_screen")
                    }
                }.addOnFailureListener() { task ->
                    Toast.makeText(context, "Fail! ${task.message}", Toast.LENGTH_LONG).show()
                }
            })
        {
            Text(text = "Create account", color = Green0091A7)
        }
    }
}

@Composable
fun DialogShow(title: String, content: String, dialogType: Int, valueAlter: (info: Int) -> Unit) {
    AlertDialog(
        onDismissRequest = {
            valueAlter(DIALOG_CLOSE)
        },
        title = {
            Text(modifier = Modifier.fillMaxWidth(),
                text = title)
        },
        text = {
            Text(modifier = Modifier.fillMaxWidth(),
                text = content)
        },
        buttons = {
            if(dialogType == DIALOG_TYPE_OK) {
                Button(
                    modifier = Modifier
                        .padding(2.dp),
                    onClick = {
                        valueAlter(DIALOG_CLOSE)
                    })
                {
                    Text(stringResource(R.string.ok))
                }
            } else {
                Row() {
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(2.dp),
                        onClick = {
                            valueAlter(DIALOG_CLOSE)
                        })
                    {
                        Text(stringResource(R.string.app_title))
                    }

                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(2.dp),
                        onClick = {
                            valueAlter(DIALOG_CLOSE)
                        })
                    {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

@Composable
fun ChoiceFood04(navController: NavController, auth: FirebaseAuth, context: Context, mainViewModel: MainViewModel) {
    // data class
    // Map, List, MutableList
    val shopInfo = mainViewModel.findShop(1)
    val foodInfo = mainViewModel.findFood(1)
    val scrollState = rememberLazyListState()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.shop), color = Green0091A7)
        shopInfo[1]?.let { shopData ->
            Text(text = shopData.name, color = Green0091A7)
            //if(foodInfo.size)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
                state = scrollState
            ) {
                var count = 1
                Log.v("TEST", "food info 001: ${foodInfo.size}")
                items(foodInfo.size) {
                    for(info in foodInfo) {
                        info[count]?.let { food ->
                            Text(food.name)
                            Text(food.price)
                            Text(food.memo)
                            Text("-----")
                            count++
                        }
                    }
                }
            }

        }


//        Text(text = "Today's show:",
//            modifier = Modifier.clickable(onClick = {
//                navController.navigate("login_screen")
//            })
//        )
    }
}

@Composable
fun FunctionList05(navController: NavController, auth: FirebaseAuth, context: Context, mainViewModel: MainViewModel) {
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.Blue31B6FB

    val foodList = listOf("Order", "Modify order", "Shop manager", "Schedule shop", "Logout")

    Log.v("Test", "FunctionList05 in")

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally) {

        for(item in foodList) {
            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier.fillMaxWidth(0.8f),
                //enabled = false,
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Green0091A7,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                    when (item) {
                        "Order" -> Log.v("TEST", "123")
                        "Shop manager" -> {
                            navController.navigate("shop_txt_process")
                        }
                        "Logout" -> {
                            auth?.let { authority ->
                                authority.signOut()
                                navController.navigate("login_screen")
                            }
                        }
                    }
                })
            {
                when (item) {
                    "Order" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.ic_baseline_fastfood_24), contentDescription = "")
                            Text(text = stringResource(R.string.function_order), color = Green4DCEE3, fontSize = 30.sp)
                        }
                    }
                    "Modify order" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.ic_baseline_border_color_24), contentDescription = "")
                            Text(text = stringResource(R.string.function_modify_order), color = Green4DCEE3, fontSize = 30.sp)
                        }
                    }
                    "Shop manager" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.ic_baseline_shopping_basket_24), contentDescription = "")
                            Text(text = stringResource(R.string.function_shop_process), color = Green4DCEE3, fontSize = 30.sp)
                        }
                    }
                    "Schedule shop" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.ic_baseline_schedule_24), contentDescription = "")
                            Text(text = stringResource(R.string.function_schedule_shop), color = Green4DCEE3, fontSize = 30.sp)
                        }
                    }
                    "Logout" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.ic_baseline_arrow_outward_24), contentDescription = "")
                            Text(text = stringResource(R.string.function_logout), color = Green4DCEE3, fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ShopTxtProcess08(navController: NavController, context: Context, activity: MainActivity, mainViewModel: MainViewModel) {
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.Blue31B6FB

    val fileContent by mainViewModel._textFileContent.observeAsState(stringResource(R.string.process_status))

    val scrollState = rememberLazyListState()
    val foodInfo = mainViewModel.findFood(1)

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier.weight(1f).padding(start = 20.dp),  //重要,要讓Row的資料集中顯示, 左邊的要用 padding(start, 右邊的要用 .padding(end
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Green0091A7,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),

                onClick = {
                    var inputStream: InputStream = MainActivity.TEXT_FILE_SAMPLE.byteInputStream()
                    //val storeDirectory = mainActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) // DCIM folder
                    //val storeDirectory = mainActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM)
                    val storeDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                    val outputFile = File(storeDirectory, "BentoShopSample.txt")
                    inputStream.use { input ->
                        val outputStream = FileOutputStream(outputFile)
                        outputStream.use { output ->
                            val buffer = ByteArray(4 * 1024) // buffer size
                            while (true) {
                                val byteCount = input.read(buffer)
                                if (byteCount < 0) break
                                output.write(buffer, 0, byteCount)
                            }
                            output.flush()
                            output.close()
                            Log.v("TEST", "download file success")
                        }
                    }

                }
            ) {
                Text(text = stringResource(R.string.download_shop_sample_file), color = Green4DCEE3, fontSize = 15.sp)
            }

            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier.weight(1f).padding(end = 20.dp),
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Green0091A7,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                    var fileIntent = Intent(Intent.ACTION_GET_CONTENT).setType("text/plain")
                    //val mimeTypes =
                    //    arrayOf("image/*", "video/*")   //我們只要Image, 但為測試多個可能,就把二個都加入,但實測上發現沒有效果
                    //fileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    startActivityForResult(activity, fileIntent, SELECT_TEXT_FILE_ID, null)
                })
            {
                Text(text = stringResource(R.string.download_shop_file), color = Green4DCEE3, fontSize = 15.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier.weight(1f).padding(start = 20.dp),
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Green0091A7,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                    var fileIntent = Intent(Intent.ACTION_GET_CONTENT).setType("text/plain")
                    //val mimeTypes =
                    //    arrayOf("image/*", "video/*")   //我們只要Image, 但為測試多個可能,就把二個都加入,但實測上發現沒有效果
                    //fileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    startActivityForResult(activity, fileIntent, SELECT_TEXT_FILE_ID, null)
                })
            {
                Text(text = stringResource(R.string.upload_shop_file), color = Green4DCEE3, fontSize = 15.sp)
            }

            Button( //Button只是一個容器,裡面要放文字,就是要再加一個Text
                modifier = Modifier.weight(1f).padding(end = 20.dp),
                enabled = true, //如果 enabled 設為false, border, interactionSource就不會有變化
                interactionSource = interactionSourceTest,
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 2.dp
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Green0091A7,
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                    var inputStream: InputStream = MainActivity.TEXT_FILE_SAMPLE.byteInputStream()
                    //val storeDirectory = mainActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) // DCIM folder
                    //val storeDirectory = mainActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM)
                    val storeDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                    val outputFile = File(storeDirectory, "BentoShopSample.txt")
                    inputStream.use { input ->
                        val outputStream = FileOutputStream(outputFile)
                        outputStream.use { output ->
                            val buffer = ByteArray(4 * 1024) // buffer size
                            while (true) {
                                val byteCount = input.read(buffer)
                                if (byteCount < 0) break
                                output.write(buffer, 0, byteCount)
                            }
                            output.flush()
                            output.close()
                            Log.v("TEST", "download file success")
                        }
                    }
                })
            {
                Text(text = stringResource(R.string.save_to_database), color = Green4DCEE3, fontSize = 15.sp)
            }

        }
        Text(
            text = fileContent,
            modifier = Modifier
                .fillMaxWidth(1f)
                .clickable(onClick = {
                navController.navigate("initial_screen")
            })
        )
        DropdownMenuShow()
        LazyColumn(
            modifier = Modifier.fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .background(color = Purple500),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
            state = scrollState
        ) {
            var count = 1
            Log.v("TEST", "food info 001: ${foodInfo.size}")
            items(foodInfo.size) {
                for(info in foodInfo) {
                    info[count]?.let { food ->
                        Text(food.name)
                        Text(food.price)
                        Text(food.memo)
                        Text("-----")
                        count++
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuShow() {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("Item1", "Item2", "Item3",)
    var selectedText by remember { mutableStateOf("") } //重要: 這會要你 import library,最後AS會直接 import androidx.compose.runtime.*
    var textfieldSize by remember { mutableStateOf(Size.Zero) }
    val keyboardOption = KeyboardOptions(autoCorrect = true)   //重要,共有四項,都可以再加
    val keyboardAction = KeyboardActions(onDone = {})

    val icon2 = if (expanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val icon = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown

    // 重要,下面三行是 interactionSource 的使用
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE  //Import com.pcp.composecomponent.ui.theme.YellowFFEB3B

    OutlinedTextField(
        value = selectedText,
        onValueChange = { selectedText = it },
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->  //重要
                textfieldSize = coordinates.size.toSize()
            },
        enabled = true,
        readOnly = true,
        textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
        label = { Text("OutlinedTextField & DropdownMenu")},
        placeholder = { Text("Enter info") },
        leadingIcon = {    //重要
            Icon(icon2, "contentDescription",
                Modifier.clickable { expanded = !expanded })
        },
        trailingIcon = {    //重要
            Icon(icon, "contentDescription",
                Modifier.clickable { expanded = !expanded })
        },
        isError = false,    //指示是否text fields的目前值是有錯的,若true, label, bottom indicator和 trailingIcon 預設都顯示錯誤的顏色
        visualTransformation = PasswordVisualTransformation(), //可看原始碼
        keyboardOptions = keyboardOption,
        keyboardActions = keyboardAction,
        singleLine = false,
        maxLines = 2,
        interactionSource = interactionSourceTest,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = Purple200),
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .width(with(LocalDensity.current) {
                textfieldSize.width.toDp() }),
        offset = DpOffset(10.dp, 10.dp),
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false, securePolicy = SecureFlagPolicy.SecureOn), // 重要
    ) {
        suggestions.forEach { label ->
            DropdownMenuItem(onClick = {
                selectedText = label
                expanded = false },
                modifier = Modifier.background(Teal200),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                interactionSource = interactionSourceTest) {    //可以增加按下之類的處理
                Text(text = label)
            }
        }
    }
}

@Composable
fun TextFieldShow(from: Int, value: String, fieldratio: Float, valueAlter: (info: String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth(fieldratio),
        value = value,
        onValueChange = { valueAlter(it) },
        placeholder = {
            when (from) {
                MainViewModel.TEXT_EMAIL -> Text(text = stringResource(R.string.email), color = Green0091A7)
                MainViewModel.TEXT_PASSWORD -> Text(text = stringResource(R.string.password), color = Green0091A7)
                MainViewModel.TEXT_NICKNAME -> Text(text = stringResource(R.string.nick_name), color = Green0091A7)
                MainViewModel.TEXT_PASSWORD_CONFIRM -> Text(text = stringResource(R.string.confirm_password), color = Green0091A7)
                MainViewModel.TEXT_NEW_ACCOUNT_EMAIL -> Text(text = stringResource(R.string.new_account_email), color = Green0091A7)
            }
        },
        shape = RoundedCornerShape(8.dp),
        //colors = TextFieldDefaults.outlinedTextFieldColors(
        //    backgroundColor = Purple200),
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BentoTWTheme {

    }
}