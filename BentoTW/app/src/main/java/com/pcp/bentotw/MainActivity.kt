package com.pcp.bentotw

import android.content.Context
import android.os.Bundle
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
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
class MainActivity : ComponentActivity() {
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                }
            }
        }
    }

    companion object {
        val DIALOG_CLOSE = 0
        val DIALOG_OPEN = 1
        val DIALOG_CLOSE_BACK_PRIOR = 2

        val DIALOG_TYPE_OK = 0
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
        navController.navigate("login_screen")
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
            onClick = { if(loginUserMail == "") {
                if(emailText.isEmpty()) {
                    Toast.makeText(context, "Please enter account", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if(passwordText.isEmpty()) {
                    Toast.makeText(context, "Please enter password", Toast.LENGTH_LONG).show()
                    return@Button
                }
                auth.signInWithEmailAndPassword(emailText,passwordText)
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Login error: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                    }.addOnSuccessListener { result ->
                        result.user?.let { userInfo ->
                            if (!userInfo.isEmailVerified) {
                                userInfo.sendEmailVerification().addOnSuccessListener {
                                    openDialog.value = DIALOG_OPEN
                                    Log.v("TEST", "verify 02: Send verification")
                                }.addOnFailureListener { error ->
                                    Toast.makeText(context, "Account create success but fail verification! error: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                                auth.signOut()
                            } else {
                                Toast.makeText(context, "Already sign and verify, go to next", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            } else {
                auth.signOut()
            } })
        {
            Text(text = if(loginUserMail == "") "Login" else "Logout")
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
fun TextFieldShow(from: Int, value: String, fieldratio: Float, valueAlter: (info: String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.padding(2.dp).
        fillMaxWidth(fieldratio),
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