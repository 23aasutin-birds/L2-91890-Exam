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
import androidx.annotation.StringRes
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
import androidx.room.ForeignKey
import androidx.room.TypeConverter
import java.time.LocalDateTime
import androidx.room.TypeConverters
import androidx.room.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HerptofaunaTheme {
                HerptofaunaNavigation()
                }
            }
        val db = Room.databaseBuilder(
            applicationContext,
            HerptofaunaDatabase::class.java,
            "species_data"
        )
            .createFromAsset("species_data")
            .allowMainThreadQueries()
            .build()
        val species_dao = db.speciesDoa()
        val all_species: List<Species> = species_dao.get_all_species()
        for (species in all_species) {
            println("Found item: ${species.scientificName} is a ${species.englishName}!")
        }
    }
}

class DateTimeConverters {
    @TypeConverter
    fun dateToString(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    fun dateToString(value: LocalDateTime?): String? {
        return date?.toString()
    }
}

// Observation table
@Entity (tableName = "observation")
data class Observation(
    @PrimaryKey (autoGenerate = true)
    val eventId: Int = 0,
    val location: String,
    val dateTime: LocalDateTime,
    val duration: Int,
)

@Dao
interface ObservationDao {
    @Insert
    suspend fun insertObservation(observation: Observation)
}

// Checklist table
@Entity ("checklist",
    foreignKeys = [
        ForeignKey(
            entity = Observation::class,
            parentColumns = ["observationID"],
            childColumns = ["observationID"],
        ), // Declares species F-Key
        ForeignKey(
            entity = Species::class,
            parentColumns = ["speciesID"],
            childColumns = ["speciesID"],
        ) // Declares species F-Key
    ])
data class Checklist(
    @PrimaryKey (autoGenerate = true)
    val observationId: Int,
    val eventId: Int, // F-Key
    val speciesId: Int, // F-Key
    val count: Int,
    val userComment: String,
    val image: String,
)

@Dao
interface ChecklistDoa {
    @Insert
    suspend fun insertChecklist(checklist: Checklist)
}

// Species table
@Entity (tableName = "species_data")
data class Species(
    @PrimaryKey (autoGenerate = true)
    val speciesId: Int,
    val scientificName: String,
    val englishName: String,
    val speciesComment: String,
)

@Dao
interface SpeciesDoa {
    @Query("SELECT * FROM species_data")
    fun get_all_species(): List<Species>
    // Stuff in here...
}


// Sets up connection between database, room and the app

@TypeConverters(DateTimeConverters::class)
@Database(entities = [Observation::class], version = 1, exportSchema = false)
abstract class HerptofaunaDatabase : RoomDatabase() { // abstract is to make a blueprint class

    // Observation table
    abstract fun checklistDao(): ChecklistDoa
    abstract fun speciesDao(): SpeciesDoa
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