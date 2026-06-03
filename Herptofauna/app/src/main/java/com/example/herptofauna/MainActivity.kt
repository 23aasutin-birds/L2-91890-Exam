package com.example.herptofauna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.herptofauna.ui.theme.HerptofaunaTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HerptofaunaTheme {
                HerptofaunaNavigation()
                }
            }
        }
}

sealed class HerptofaunaScreen(val route: String) {
    object HomePage : HerptofaunaScreen("home_page")
    object ChecklistPage : HerptofaunaScreen("checklist_page")
    object SpeciesPage : HerptofaunaScreen("species_page")
    object SubmitPage : HerptofaunaScreen("submit_page")
}

@Composable
fun HerptofaunaNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HerptofaunaScreen.HomePage.route
    ) {
        composable(route = HerptofaunaScreen.HomePage.route) {
            HomePageLayout(navController = navController)
        }
        composable(route = HerptofaunaScreen.ChecklistPage.route) {
            ChecklistPageLayout(navController = navController)
        }
        composable(route = HerptofaunaScreen.SpeciesPage.route) {
            SpeciesPageLayout(navController = navController)
        }
        composable(route = HerptofaunaScreen.SubmitPage.route) {
            SubmitPageLayout(navController = navController)
        }
    }
}

@Composable
fun HomePageLayout(navController : NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {navController.navigate(HerptofaunaScreen.ChecklistPage.route)}) {
            Text("Start Survey")
        }
    }
}

@Composable
fun ChecklistPageLayout(navController : NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {navController.navigate(HerptofaunaScreen.SpeciesPage.route)}) {
            Text("See Species")
        }
        Button(onClick = {navController.navigate(HerptofaunaScreen.SubmitPage.route)}) {
            Text("Stop Checklist")
        }
    }
}

@Composable
fun SpeciesPageLayout(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {navController.popBackStack()}) {
            Text("Back")
        }
    }
}

@Composable
fun SubmitPageLayout(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {navController.navigate(HerptofaunaScreen.HomePage.route)}) {
            Text("Back to Home Page")
        }
    }
}