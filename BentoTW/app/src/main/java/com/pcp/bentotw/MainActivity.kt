package com.pcp.bentotw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pcp.bentotw.ui.theme.BentoTWTheme
import com.pcp.bentotw.ui.theme.Green0091A7
import com.pcp.bentotw.ui.theme.Green00E6FE
import kotlinx.coroutines.delay

/*
    Arthur: Joey yang
    Use skill:
    1. Navigation.
    2. LaunchedEffect (Coroutine).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BentoTWTheme {
                val navController = rememberNavController() //Navigation Step2

                NavHost(
                    navController = navController,
                    startDestination = "initial_screen"
                ) {
                    composable("initial_screen") {
                        //second screen
                        InitialScreen01(navController = navController)
                    }
                    composable("login_screen") {
                        //first screen
                        LoginScreen02(navController = navController)
                    }

                }
            }
        }
    }
}

@Composable
fun InitialScreen01(navController: NavController) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green00E6FE),
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
fun LoginScreen02(navController: NavController) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Green00E6FE),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_title),
            color = Green0091A7)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BentoTWTheme {

    }
}