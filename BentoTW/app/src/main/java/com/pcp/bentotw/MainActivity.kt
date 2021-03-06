package com.pcp.bentotw

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.DatePicker
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.pcp.bentotw.MainActivity.Companion.DB_INITIAL
import com.pcp.bentotw.MainActivity.Companion.DIALOG_CLOSE
import com.pcp.bentotw.MainActivity.Companion.DIALOG_CLOSE_BACK_PRIOR
import com.pcp.bentotw.MainActivity.Companion.DIALOG_OPEN
import com.pcp.bentotw.MainActivity.Companion.DIALOG_TYPE_OK
import com.pcp.bentotw.ui.theme.*
import kotlinx.coroutines.delay
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
const val SELECT_FILE_ID: Int = 10
const val SELECT_TEXT_FILE_ID: Int = 11
const val REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 102

class MainActivity : ComponentActivity() {
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

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
            ActivityCompat.requestPermissions(  // ??????
                this,
                requestedPermissions.toTypedArray(), REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SELECT_FILE_ID -> if (data != null) {
                //Log.v("TEST", "Choice file: ${data.data?.path}")
                Log.v("TEST", "Choice file: ${File(data.data?.path).name}")

                data.data?.let { uriInfo ->
                    mainViewModel.setReceivePictureUri(uriInfo)
                }
//  Note: Already mark, but still need to study.
//                when(data.data?.scheme) {
//                    ContentResolver.SCHEME_CONTENT -> Log.v("TEST", "Choice file2: ${getContentFileName(data.data!!)}")
//                    else -> Log.v("TEST", "Choice file3: ${data.data?.path?.let(::File)?.name}")
//                }
            }

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
                            this@MainActivity.mainViewModel.parseUploadFile(fileContent)
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

            firestore = Firebase.firestore
            storage = Firebase.storage("gs://myfirebase-d8e25.appspot.com")
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
                        FunctionList05(navController = navController, auth, applicationContext, mainViewModel, firestore)
                    }
                    composable("shop_txt_process") {
                        ShopTxtProcess08(navController = navController, applicationContext, this@MainActivity, mainViewModel, firestore)
                    }
                    composable("schedule_bento") {
                        Schedule09(navController = navController, applicationContext, this@MainActivity, mainViewModel, auth, firestore)
                    }
                    composable("order_bento") {
                        Order10(navController = navController, applicationContext, this@MainActivity, mainViewModel, auth, firestore)
                    }
                    composable("storage_test") {
                        StorageTest11(navController = navController, applicationContext, this@MainActivity, mainViewModel, auth, firestore, storage)
                    }

                }
            }
        }
    }

    private fun readTextFile(uri: Uri): MutableList<FileStruct> {
        var sentenceList: MutableList<String> = ArrayList() //??????,????????????
        var parseInfo: MutableList<FileStruct> = ArrayList()

        var reader: BufferedReader? = null
        val builder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(contentResolver.openInputStream(uri)))
            var line: String? = ""
            while (reader.readLine().also { line = it } != null) {
                builder.append(line)
                line?.let { sentenceList.add(it) }
                builder.append(System.getProperty("line.separator"));   //??????,?????????????????????\r\n?????????
            }
            reader.close()
            var count = 0
            var parseFail = PARSE_SUCCESS
            for(info in sentenceList) {
                var dataSplit : MutableList<String> = info.split(",".toRegex()).toMutableList()
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

                    if(dataSplit[1] == "0" && dataSplit.size == 5)
                        dataSplit.add("")
                    else(dataSplit[1] != "0" && dataSplit.size == 4)
                        dataSplit.add("")

                    val value = FileStruct(parseFail, count + 1, info, dataSplit)
                    if(parseFail != PARSE_SUCCESS)
                        parseInfo.clear()   //??????????????????????????????,????????????????????????
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
        const val DIALOG_CLOSE = 0  //??????:AS???????????? const val,??????????????????
        const val DIALOG_OPEN = 1
        const val DIALOG_CLOSE_BACK_PRIOR = 2

        const val DIALOG_TYPE_OK = 0

        const val TEXT_FILE_SAMPLE = "0,0,????????????,25569779,?????????????????????????????????70???,a.????????????(?????????) b.???????????????????????????0 c.????????????????????????????????????\n" +
                "0,1,???????????????,140,??????(??????????????????????????????0-????????????)\n" +
                "0,1,???????????????,120,??????(????????????????????????1)\n" +
                "0,2,??????????????????,70,???????????????????????????2\n" +
                "0,3,????????????,30,???????????????????????????3\n" +
                "0,4,?????????,60,????????????????????????4\n" +
                "0,1,?????????(???),25\n" +
                "0,1,?????????(???),40\n" +
                "0,0,????????????,88888888,?????????????????????????????????70???,??????????????????(?????????????????????????????????)\n" +
                "0,1,???????????????,500,??????????????????\n" +
                "0,2,?????????,60\n"

        const val PARSE_SUCCESS = 0
        const val PARSE_ERROR_FIELD_NEED_DIGIT = 1
        const val PARSE_ERROR_FIRST_NOT_RESTAURANT = 2
        const val PARSE_ERROR_FIELD_AMOUNT = 3
        const val PARSE_ERROR_FIELD_OTHER = 4
        var DB_INITIAL = 0
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
    val openDialog = remember { mutableStateOf(DIALOG_CLOSE) } // = ??? by ????????????, by???????????????,??????????????? .value ??????.
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
            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .alpha(if (emailText.isEmpty()) 0f else 100f),   //????????????????????????
                //enabled = false,
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                            Toast.makeText(context, context.getText(R.string.enter_email), Toast.LENGTH_LONG).show()    //??????,?????????????????????
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

            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier.fillMaxWidth(0.5f),
                //enabled = false,
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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

        Button( //Button??????????????????,??????????????????,?????????????????????Text
            //modifier = Modifier.fillMaxHeight(0.5f),
            //enabled = false,
            enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
        //Text(text = "order", modifier = Modifier.clickable { navController.navigate("choice_food_screen") })
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
        Button( //Button??????????????????,??????????????????,?????????????????????Text
            //modifier = Modifier.fillMaxHeight(0.5f),
            //enabled = false,
            enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                            auth.signOut()  // ????????????????????????,????????????,???????????????????????????,????????????????????????,???????????????
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
fun FunctionList05(navController: NavController, auth: FirebaseAuth, context: Context, mainViewModel: MainViewModel, firestore: FirebaseFirestore) {
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.Blue31B6FB

    val foodList = listOf("Order", "Modify order", "Shop manager", "Schedule shop", "Storage test", "Logout")

    Log.v("Test", "FunctionList05 in")

    //if(DB_INITIAL++ == 0)
        mainViewModel.getDBInfo(firestore)

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally) {

        for(item in foodList) {
            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier.fillMaxWidth(0.8f),
                //enabled = false,
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                        "Order" ->
                            navController.navigate("order_bento")
                        "Shop manager" -> {
                            navController.navigate("shop_txt_process")
                        }
                        "Logout" -> {
                            auth?.let { authority ->
                                authority.signOut()
                                navController.navigate("login_screen")
                            }
                        }
                        "Schedule shop" -> {
                            navController.navigate("schedule_bento")
                        }
                        "Storage test" -> {
                            navController.navigate("storage_test")
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
                    "Storage test" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(id = R.drawable.ic_baseline_schedule_24), contentDescription = "")
                            Text(text = stringResource(R.string.function_storage_test), color = Green4DCEE3, fontSize = 30.sp)
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
fun ShopTxtProcess08(navController: NavController, context: Context, activity: MainActivity, mainViewModel: MainViewModel, firestore: FirebaseFirestore) {
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.Blue31B6FB

    val fileContent by mainViewModel._textFileContent.observeAsState(stringResource(R.string.process_status))

    val scrollState = rememberLazyListState()
    //val foodInfo = mainViewModel.findFood(1)
    val foodInfo = mainViewModel.findFood()

    if(mainViewModel.uploadShopInfo.isNotEmpty()) {
        for(data in mainViewModel.uploadShopInfo)
            Log.v("TEST", "Shop info: ${data.key} = ${data.value} ")
    }
    if(mainViewModel.uploadFoodInfo.isNotEmpty()) {
        for(data in mainViewModel.uploadFoodInfo)
            Log.v("TEST", "Food info: ${data.key} = ${data.value} ")
    }
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
            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),  //??????,??????Row?????????????????????, ??????????????? padding(start, ??????????????? .padding(end
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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

            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 20.dp),
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                    //    arrayOf("image/*", "video/*")   //????????????Image, ????????????????????????,?????????????????????,??????????????????????????????
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
            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                    //    arrayOf("image/*", "video/*")   //????????????Image, ????????????????????????,?????????????????????,??????????????????????????????
                    //fileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    startActivityForResult(activity, fileIntent, SELECT_TEXT_FILE_ID, null)
                })
            {
                Text(text = stringResource(R.string.upload_shop_file), color = Green4DCEE3, fontSize = 15.sp)
            }

            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 20.dp),
                enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                    mainViewModel.setTxtFileContent("Writing data into firestore")
                    mainViewModel.deleteShopFood(firestore)
                    //mainViewModel.saveShopFoodFromFile(firestore)
                })
            {
                Text(text = stringResource(R.string.save_to_database), color = Green4DCEE3, fontSize = 15.sp)
            }
        }
        Button( //Button??????????????????,??????????????????,?????????????????????Text
            modifier = Modifier.fillMaxWidth(),
            enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                navController.popBackStack()
            })
        {
            Text(text = "Back to prior page", color = Green4DCEE3, fontSize = 15.sp)
        }
        Text(
            text = fileContent,
            modifier = Modifier
                .fillMaxWidth(1f)
                .clickable(onClick = {
                    navController.navigate("initial_screen")
                })
        )
        DropdownMenuShow(mainViewModel)
        var count = 1
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(color = Purple500),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
            state = scrollState
        ) {

            Log.v("TEST", "food info 001: ${foodInfo.size}")
            for (food in foodInfo) {
                item {  //??????,??????????????????????????????
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .background(if (count % 2 == 0) Purple200 else Blue31B6FB)) {
                        Text(food.value.name)
                        Text(food.value.price)
                        Text(food.value.memo)
                        Text("-----")
                    }
                }
                count++
            }
        }
    }
}

@Composable
fun Schedule09(navController: NavController, context: Context, activity: MainActivity, mainViewModel: MainViewModel, auth: FirebaseAuth, firestore: FirebaseFirestore) {
    // Reference website: https://www.geeksforgeeks.org/date-picker-in-android-using-jetpack-compose/
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.Blue31B6FB

    val shopName by mainViewModel._scheduleShop.observeAsState()
    val scheduleRefresh by mainViewModel._scheduleRefresh.observeAsState()  //??????????????????,???????????????refresh,?????????Log.v???????????????

    val scrollState = rememberLazyListState()

    // Fetching the Local Context
    val mContext = LocalContext.current

    // Declaring integer values
    // for year, month and day
    val mYear: Int
    val mMonth: Int
    val mDay: Int

    Log.v("TEST", "Schedule09 in $scheduleRefresh ${mainViewModel.scheduleRefreshUpdateOK}")
    // Initializing a Calendar
    val mCalendar = Calendar.getInstance()

    // Fetching current year, month and day
    mYear = mCalendar.get(Calendar.YEAR)
    mMonth = mCalendar.get(Calendar.MONTH)
    mDay = mCalendar.get(Calendar.DAY_OF_MONTH)

    mCalendar.time = Date()

    // Declaring a string value to
    // store date in string format
    val mDate = remember { mutableStateOf("") }

    // Declaring DatePickerDialog and setting
    // initial values as current values (present year, month and day)
    val mDatePickerDialog = DatePickerDialog(
        mContext,
        { _: DatePicker, mYear: Int, mMonth: Int, mDayOfMonth: Int ->
            //mDate.value = "$mDayOfMonth/${mMonth+1}/$mYear"
            mDate.value = "%04d/%02d/%02d".format(mYear, mMonth+1, mDayOfMonth)
        }, mYear, mMonth, mDay
    )

    Column(modifier = Modifier.fillMaxSize().background(color = Green4DCEE3), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
        // Creating a button that on
        // click displays/shows the DatePickerDialog
        Spacer(modifier = Modifier.size(50.dp))  //Important
        Row() {
            Button(modifier = Modifier.weight(0.5f).padding(5.dp),
                onClick = { mDatePickerDialog.show() },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0XFF0F9D58))) {
                Text(text = "Open Date Picker", color = Color.White)
            }

            Button( //Button??????????????????,??????????????????,?????????????????????Text
                modifier = Modifier.weight(0.5f).padding(5.dp),
                shape = RoundedCornerShape(8.dp),
                //border = BorderStroke(5.dp, color = borderColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0XFF0F9D58),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(4.dp, 3.dp, 2.dp, 1.dp),
                onClick = {
                    if (mDate.value == "" || shopName == "") {
                        Toast.makeText(context, "Don't set Day or Shop", Toast.LENGTH_LONG).show()    //??????,?????????????????????
                        return@Button
                    } else
                        shopName?.let { mainViewModel.setShop(mDate.value, it, firestore, auth) }
                })
            {
                Text(text = "Set shop")
            }
        }
        Button(modifier = Modifier.fillMaxWidth().padding(5.dp),
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0XFF0F9D58))) {
            Text(text = "Back to prior page", color = Color.White)
        }

        DropdownMenuDBShop(mainViewModel)

        // Adding a space of 100dp height
        //Spacer(modifier = Modifier.size(100.dp))  //Important

        // Displaying the mDate value in the Text
        Text(text = "Selected Date: ${mDate.value}", fontSize = 30.sp, textAlign = TextAlign.Center)
        Text(text = "Selected Shop: $shopName", fontSize = 30.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.size(50.dp))  //Important
        Text(text = "Schedule info:")
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            state = scrollState
        ) {
            var count = 1
            Log.v("TEST", "food info 001: ${mainViewModel.dbScheduleInfo.size}")
            val sdFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:dd")
            for(info in mainViewModel.dbScheduleInfo) {
                val recordDateTime = Date(info.value.setTime)
                item {  //??????,??????????????????????????????
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .background(if (count % 2 == 0) Purple200 else Blue31B6FB)) {
                        Text("??????: ${info.value.date}")
                        Text("????????????: ${info.value.name}")
                        Text("????????????: ${sdFormatter.format(recordDateTime)}")
                        Text("?????????: ${info.value.founder}")
                        Text("-----")
                    }
                }
            }
//            items(foodInfo.size) {
//                for(info in foodInfo) {
//                    info[count]?.let { food ->
//                        Text(food.name)
//                        Text(food.price)
//                        Text(food.memo)
//                        Text("-----")
//                        count++
//                    }
//                }
//            }
        }
    }

    if(mainViewModel.scheduleRefreshUpdateOK != 0) {
        when(mainViewModel.scheduleRefreshUpdateOK) {
            1 -> Toast.makeText(context, "Set shop success", Toast.LENGTH_LONG).show()
            2 -> Toast.makeText(context, "Set shop fail", Toast.LENGTH_LONG).show()
            3 -> Toast.makeText(context, "Set shop success but read fail!", Toast.LENGTH_LONG).show()
        }
        mainViewModel.scheduleRefreshUpdateOK = 0
    }
}

@Composable
fun Order10(navController: NavController, context: Context, activity: MainActivity, mainViewModel: MainViewModel, auth: FirebaseAuth, firestore: FirebaseFirestore) {
    val foodInfo = mainViewModel.getTodayFood(firestore)
    val shopName = mainViewModel.getTodayShop()
    Column(modifier = Modifier.fillMaxSize().background(color = Green4DCEE3), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
        // Creating a button that on
        // click displays/shows the DatePickerDialog
        Spacer(modifier = Modifier.size(50.dp))  //Important
        Button(modifier = Modifier.fillMaxWidth(0.8f),
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0XFF0F9D58))) {
            Text(text = "Back to prior page", color = Color.White)
        }
        if(shopName == "")
            Text(text = "????????????: ?????????")
        else
            Text(text = "????????????: $shopName")
        Text(text = "????????????:")
        if(foodInfo.isEmpty())
            Text("No data")
        else {
            for(info in foodInfo) {
                //Card() {
                    Row() {
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = info.value.name
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = info.value.price
                        )
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = info.value.memo
                    )
                //}
            }
        }
    }
}

@Composable
fun StorageTest11(navController: NavController, context: Context, activity: MainActivity, mainViewModel: MainViewModel, auth: FirebaseAuth, firestore: FirebaseFirestore, storage: FirebaseStorage) {

    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE //Import com.pcp.composecomponent.ui.theme.YellowFFEB3B

    val pictureFileUrl by mainViewModel._pictureFieUri.observeAsState(null)

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green4DCEE3),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Upload shop image from mobile",
            modifier = Modifier.clickable(onClick = {
                navController.navigate("initial_screen")
            })
        )
        Button( //Button??????????????????,??????????????????,?????????????????????Text
            modifier = Modifier.fillMaxWidth(),
            //enabled = false,
            enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                var fileIntent = Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
                val mimeTypes = arrayOf("image/*", "video/*")   //????????????Image, ????????????????????????,?????????????????????,??????????????????????????????
                fileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                startActivityForResult(activity, fileIntent, SELECT_FILE_ID, null)
            })
        {
            Text(text = "Upload image from mobile phone")
        }
        Button( //Button??????????????????,??????????????????,?????????????????????Text
            modifier = Modifier.fillMaxWidth(),
            //enabled = false,
            enabled = true, //?????? enabled ??????false, border, interactionSource??????????????????
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
                navController.popBackStack()
            })
        {
            Text(text = "Back to prior page")
        }
    }

    pictureFileUrl?.let { uriInfo ->
        var fileName = mainViewModel.getFileName(uriInfo, activity)
        var storageRef = storage.reference.child("images/" + fileName)

        var uploadTask = storageRef.putFile(uriInfo)
        uploadTask.addOnSuccessListener { listener ->
            Toast.makeText(activity, "Upload success", Toast.LENGTH_LONG).show()
            mainViewModel.setReceivePictureUri(null)
        }.addOnFailureListener { listener ->
            Toast.makeText(activity, "Upload fail", Toast.LENGTH_LONG).show()
            mainViewModel.setReceivePictureUri(null)
        }
    }
}

@Composable
fun DropdownMenuShow(mainViewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    //val suggestions = listOf("Item1", "Item2", "Item3",)
    //val suggestions = mainViewModel.uploadShopInfo
    var suggestions : MutableList<String> = ArrayList()
    var selectedText by remember { mutableStateOf("") } //??????: ???????????? import library,??????AS????????? import androidx.compose.runtime.*
    var textfieldSize by remember { mutableStateOf(Size.Zero) }
    val keyboardOption = KeyboardOptions(autoCorrect = true)   //??????,????????????,???????????????
    val keyboardAction = KeyboardActions(onDone = {})

    val icon2 = if (expanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val icon = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown

    // ??????,??????????????? interactionSource ?????????
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE  //Import com.pcp.composecomponent.ui.theme.YellowFFEB3B

    var title = "No data"
    if(mainViewModel.uploadShopInfo.isNotEmpty()) {
        for(info in mainViewModel.uploadShopInfo) {
            suggestions.add(info.value.name)
        }
        title = suggestions[0]
    }

    OutlinedTextField(
        value = selectedText,
        onValueChange = { selectedText = it },
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->  //??????
                textfieldSize = coordinates.size.toSize()
            },
        enabled = true,
        readOnly = true,
        textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
        label = { if(suggestions.isEmpty()) Text("No data") else Text(suggestions[0])},
        placeholder = { Text("Enter info") },
        leadingIcon = {    //??????
            Icon(icon2, "contentDescription",
                Modifier.clickable { expanded = !expanded })
        },
        trailingIcon = {    //??????
            Icon(icon, "contentDescription",
                Modifier.clickable { expanded = !expanded })
        },
        isError = false,    //????????????text fields????????????????????????,???true, label, bottom indicator??? trailingIcon ??????????????????????????????
        //visualTransformation = PasswordVisualTransformation(), //???????????????
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
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false, securePolicy = SecureFlagPolicy.SecureOn), // ??????
    ) {
        suggestions.forEach { label ->
            DropdownMenuItem(onClick = {
                selectedText = label
                expanded = false
                mainViewModel.findFoodInfo(label) },
                modifier = Modifier.background(Teal200),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                interactionSource = interactionSourceTest) {    //?????????????????????????????????
                Text(text = label)
            }
        }
    }
}

@Composable
fun DropdownMenuDBShop(mainViewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    //val suggestions = listOf("Item1", "Item2", "Item3",)
    //val suggestions = mainViewModel.uploadShopInfo
    var suggestions : MutableList<String> = ArrayList()
    var selectedText by remember { mutableStateOf("") } //??????: ???????????? import library,??????AS????????? import androidx.compose.runtime.*
    var textfieldSize by remember { mutableStateOf(Size.Zero) }
    val keyboardOption = KeyboardOptions(autoCorrect = true)   //??????,????????????,???????????????
    val keyboardAction = KeyboardActions(onDone = {})

    val icon2 = if (expanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val icon = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown

    // ??????,??????????????? interactionSource ?????????
    val interactionSourceTest = remember { MutableInteractionSource() }
    val pressState = interactionSourceTest.collectIsPressedAsState()
    val borderColor = if (pressState.value) Blue31B6FB else Blue00E6FE  //Import com.pcp.composecomponent.ui.theme.YellowFFEB3B

    if(mainViewModel.dbShopInfo.isNotEmpty()) {
        for(info in mainViewModel.dbShopInfo) {
            suggestions.add(info.value.name)
        }
    } else {
        suggestions.add("No data")
    }

    OutlinedTextField(
        value = selectedText,
        onValueChange = { selectedText = it },
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->  //??????
                textfieldSize = coordinates.size.toSize()
            },
        enabled = true,
        readOnly = true,
        textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
        label = { if(suggestions.isEmpty()) Text("No data") else Text(suggestions[0])},
        placeholder = { Text("Enter info") },
        leadingIcon = {    //??????
            Icon(icon2, "contentDescription",
                Modifier.clickable { expanded = !expanded })
        },
        trailingIcon = {    //??????
            Icon(icon, "contentDescription",
                Modifier.clickable { expanded = !expanded })
        },
        isError = false,    //????????????text fields????????????????????????,???true, label, bottom indicator??? trailingIcon ??????????????????????????????
        //visualTransformation = PasswordVisualTransformation(), //???????????????
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
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false, securePolicy = SecureFlagPolicy.SecureOn), // ??????
    ) {
        suggestions.forEach { label ->
            DropdownMenuItem(onClick = {
                selectedText = label
                expanded = false
                mainViewModel.setScheduleContent(label) },
                modifier = Modifier.background(Teal200),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                interactionSource = interactionSourceTest) {    //?????????????????????????????????
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
                MainViewModel.TEXT_EMAIL -> Text(text = stringResource(R.string.email), color = WhiteFFFFFF)
                MainViewModel.TEXT_PASSWORD -> Text(text = stringResource(R.string.password), color = WhiteFFFFFF)
                MainViewModel.TEXT_NICKNAME -> Text(text = stringResource(R.string.nick_name), color = WhiteFFFFFF)
                MainViewModel.TEXT_PASSWORD_CONFIRM -> Text(text = stringResource(R.string.confirm_password), color = WhiteFFFFFF)
                MainViewModel.TEXT_NEW_ACCOUNT_EMAIL -> Text(text = stringResource(R.string.new_account_email), color = WhiteFFFFFF)
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