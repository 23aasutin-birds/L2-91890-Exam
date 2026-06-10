package com.example.herptofauna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.herptofauna.ui.theme.HerptofaunaTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

// Sets up table
@Entity (tableName = "observation")
data class Observation(
    @PrimaryKey (autoGenerate = true)
    val id: Int = 0,
    val location: String,
    val species: String,
    val count: Int
)

// Sets actions available in program
@Dao
interface ObservationDao {
    @Insert
    suspend fun insertObservation(observation: Observation)
}

// Sets up connection between database, room and the app
@Database(entities = [Observation::class], version = 1, exportSchema = false)
abstract class HerptofaunaDatabase : RoomDatabase() { // abstract is to make a blueprint class
    abstract fun observationDao(): ObservationDao // Connects DAO interface to connect to room and the database

    companion object { // Creates global class (instead of local class)
        @Volatile // Makes sure INSTANCE is consistent
        private var INSTANCE: HerptofaunaDatabase? = null // The only copy of the database

        fun getDatabase(context: Context): HerptofaunaDatabase { // Will use this function to access database
            return INSTANCE ?: synchronized(this) { // If ready to build return immediately, makes sure only 1 function accessing at a time
                val instance = Room.databaseBuilder( // Builds database
                    context.applicationContext,
                    HerptofaunaDatabase::class.java, // Tells which class room will use
                    "herptofauna_database"
                ).build()
                INSTANCE = instance // Saves database as global variable
                instance // Return instance
            }
        }
    }
}

// Creates routes to each page for navController to follow
sealed class HerptofaunaScreen(val route: String) {
    object HomePage : HerptofaunaScreen("home_page")
    object ChecklistPage : HerptofaunaScreen("checklist_page")
    object SpeciesPage : HerptofaunaScreen("species_page")
    object SubmitPage : HerptofaunaScreen("submit_page")
}

fun addObservation(observationDao: ObservationDao, location: String, species: String, count: Int) {
    CoroutineScope(Dispatchers.IO).launch {
        val newObservation =
            Observation(location = location, species = species, count = count)
        observationDao.insertObservation(newObservation)
        println("Done!")

    }
}

// Tells navController what do to at the end of each route (I guess???)
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

// All layouts (4 pages)

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
    var countMccannSkink by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val database = HerptofaunaDatabase.getDatabase(context)
    val observationDao = database.observationDao()

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {navController.navigate(HerptofaunaScreen.SpeciesPage.route)}) {
            Text("See Species")
        }
        Button(onClick = {
            addObservation(observationDao = observationDao, location = "WHS", species = "Mccann's Skink", count = countMccannSkink)
            navController.navigate(HerptofaunaScreen.SubmitPage.route)
        }) {
            Text("Stop Checklist")
        }
        Button(onClick = { countMccannSkink++ }) {
            Text("Add a Mccann's Skink. ($countMccannSkink)")
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
            Text("Confirm & Submit")
        }
    }
}