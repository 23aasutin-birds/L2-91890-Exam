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
import android.widget.TableRow
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
import androidx.room.TypeConverters
import androidx.room.Query
import androidx.room.Index
import androidx.compose.material3.TextField
import java.time.LocalDateTime
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.viewmodel.compose.viewModel

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

// LocalDateTime converters
class DateTimeConverters {

    @TypeConverter
    fun stringToLocalDateTime (value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun localDateTimeToString (date: LocalDateTime?): String? {
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
    suspend fun insertObservation(observation: Observation): Long
}

// Checklist table
@Entity ("checklist",
    foreignKeys = [
        ForeignKey(
            entity = Observation::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"]
        ), // Declares event F-Key
        ForeignKey(
            entity = Species::class,
            parentColumns = ["speciesId"],
            childColumns = ["speciesId"],
        ) // Declares species F-Key
    ],
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["speciesId"])]
    )
data class Checklist(
    @PrimaryKey (autoGenerate = true)
    val observationId: Int = 0,
    val eventId: Int, // F-Key
    val speciesId: Int, // F-Key
    val count: Int,
    val userComment: String,
    val image: String,
)

@Dao
interface ChecklistDao {
    @Insert
    suspend fun insertChecklistItems(checklist: Checklist)
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
interface SpeciesDao {
    @Query("SELECT * FROM species_data")
    suspend fun getAllSpecies(): List<Species>

    @Query("SELECT * FROM species_data WHERE speciesId = :id")
    suspend fun getSpeciesData(id: Int): Species?

    @Query("SELECT englishName FROM species_data WHERE speciesId = :id")
    suspend fun getSpeciesName(id: Int): String?

}


// Sets up connection between database, room and the app (abstract class)
@TypeConverters(DateTimeConverters::class)
@Database(entities = [Observation::class, Checklist::class, Species::class], version = 1, exportSchema = false)
abstract class HerptofaunaDatabase : RoomDatabase() { // abstract is to make a blueprint class

    // Observation table
    abstract fun checklistDao(): ChecklistDao
    abstract fun speciesDao(): SpeciesDao
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
    object ChecklistPage : HerptofaunaScreen("checklist_page/{startTime}")
    object SpeciesPage : HerptofaunaScreen("species_page")
    object SubmitPage : HerptofaunaScreen("submit_page/{startTime}")
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
        composable(route = HerptofaunaScreen.ChecklistPage.route) { backStackEntry ->
            val startTimeString = backStackEntry.arguments?.getString("startTime") ?: ""
            ChecklistPageLayout(
                navController = navController,
                startTimeString = startTimeString
            )
        }
        composable(route = HerptofaunaScreen.SpeciesPage.route) {
            SpeciesPageLayout(navController = navController)
        }
        // Single SubmitPage route using the updated sealed class property:
        composable(route = HerptofaunaScreen.SubmitPage.route) { backStackEntry ->
            val startTimeString = backStackEntry.arguments?.getString("startTime") ?: ""

            SubmitPageLayout(
                navController = navController,
                surveyStartTime = startTimeString
            )
        }
    }
}

// Commits observation data

suspend fun commitObservation(
    observationDao: ObservationDao,
    location: String,
    dateTime: LocalDateTime,
    duration: Int
) {
    val newObservation = Observation(
        location = location,
        dateTime = dateTime,
        duration = duration
    )

    observationDao.insertObservation(newObservation)
}

fun commitChecklist(checklistDao: ChecklistDao, count: Int, userComment: String, eventId: Int, speciesId: Int) {
    CoroutineScope(Dispatchers.IO).launch {
        val newChecklist = Checklist(eventId = eventId, speciesId = speciesId, count = count, userComment = userComment, image = "To be added later")// This has got to be a list will all the data in it
        checklistDao.insertChecklistItems(newChecklist)
    }
}

class SpeciesViewModel(private val speciesDao: SpeciesDao) : ViewModel() {

    // State for a single species name
    private val _speciesName = MutableStateFlow<String?>("Loading...")
    val speciesName: StateFlow<String?> = _speciesName

    // State for the whole species list
    private val _speciesList = MutableStateFlow<List<Species>>(emptyList())
    val speciesList: StateFlow<List<Species>> = _speciesList

    fun getSpeciesName(id: Int) {
        viewModelScope.launch {
            _speciesName.value = speciesDao.getSpeciesName(id)
        }
    }

    fun getAllSpeciesInfo() {
        viewModelScope.launch {
            _speciesList.value = speciesDao.getAllSpecies()
        }
    }
}

fun calcDuration(startTime: LocalDateTime, endTime: LocalDateTime): Int {
    val duration = Duration.between(startTime, endTime)
    return duration.toMinutes().toInt()
}

val speciesData = listOf(
    Species( 0, "Oligosoma grande", "Grand Skink", ""),
    Species( 1, "Oligosoma repens", "Eyres Skink", ""),
    Species( 2, "Woodwirthia \"Cromwell\"", "Kawarau Gecko", ""),
    Species( 3, "Woodworthia \"south-western\"", "Mountain Beech Gecko", ""),
    Species( 4, "Oligosoma grande", "Grand Skink", ""),
    Species( 5, "Oligosoma repens", "Eyres Skink", ""),
    Species( 6, "Woodwirthia \"Cromwell\"", "Kawarau Gecko", ""),
    Species( 7, "Woodworthia \"south-western\"", "Mountain Beech Gecko", ""),
    Species( 8, "Oligosoma grande", "Grand Skink", ""),
    Species( 9, "Oligosoma repens", "Eyres Skink", ""),
    Species( 10, "Woodwirthia \"Cromwell\"", "Kawarau Gecko", ""),
    Species( 11, "Woodworthia \"south-western\"", "Mountain Beech Gecko", ""),
    Species( 12, "Oligosoma grande", "Grand Skink", ""),
    Species( 13, "Oligosoma repens", "Eyres Skink", ""),
    Species( 14, "Woodwirthia \"Cromwell\"", "Kawarau Gecko", ""),

    )

// Uses species data to make table
@Composable
fun SpeciesTable(speciesData: List<Species>) {
    Column(modifier = Modifier.padding(16.dp)) {
        HorizontalDivider()
        LazyColumn() {
            items(speciesData) { species ->
                SpeciesRow(species = species)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SpeciesRow(species: Species) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = species.englishName, modifier = Modifier.weight(1f)
        )
        Text(
            text = species.scientificName, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SpeciesScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SpeciesTable(speciesData = speciesData)
    }
}

// All layouts (4 pages)

@Composable
fun HomePageLayout(navController : NavController, modifier: Modifier = Modifier) {

    val image = painterResource(R.drawable.unsplash_liazard_image)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Herptofauna Title
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Herptofauna",
                fontSize = 50.sp,
                textAlign = TextAlign.Center
            )
        }

        // Skink Image
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = image,
                contentDescription = "A lizard",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Start Survey Button
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center

        ) {
            Button(
                onClick = {
                    val startTime = LocalDateTime.now().toString()
                    navController.navigate("checklist_page/$startTime")
                },
                modifier = Modifier.size(width = 250.dp, height = 75.dp)

            ) {
                Text(
                    "Start Survey",
                    fontSize = 25.sp
                )
            }
        }

        // Credits and Privacy
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = -30.dp)
                    .padding(16.dp)
            ) {
                Text(
                    "Thank you to the Lizard Guy for the feedback for this app. Image credits: Igor Kazantsev from Unsplash.",
                    modifier = Modifier
                )
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    "Privacy Statement: you're personal data will not be recorded while using the app.",
                    modifier = Modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistPageLayout(
    navController : NavController,
    startTimeString: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Survery")},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {navController.navigate("submit_page/$startTimeString") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "End Survey") },
                    label = { Text("Stop Survey")
                    }
                )
            }
        }
    ) { innerPadding ->
        var countMccannSkink by remember { mutableStateOf(0) }

        val context = LocalContext.current
        val database = HerptofaunaDatabase.getDatabase(context)
        val observationDao = database.observationDao()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Stuff here")
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier) {
                    SpeciesScreen()
                }
            }
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
        } // Go back to checklist page
    }
}

@Composable
fun SubmitPageLayout(
    navController: NavController,
    modifier: Modifier = Modifier,
    surveyStartTime: String
) {
    // Collects LocalDateTime
    val context = LocalContext.current

    // Get a coroutine scope tied to this Composable's lifecycle
    val scope = rememberCoroutineScope()

    // Accesses the database
    val database = remember { HerptofaunaDatabase.getDatabase(context) }
    val observationDao = database.observationDao()

    var location by remember { mutableStateOf("") } // For collection of location

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = location,
            onValueChange = { newText -> location = newText },
            label = { Text("Entre location")}
        )

        Button(
            onClick = {
                val parsedStartTime = try {
                    LocalDateTime.parse(surveyStartTime)
                } catch (e: Exception) {
                    LocalDateTime.now() // Fallback to regular time if parsing fails, will change latter
                }

                val endTime = LocalDateTime.now()
                val duration = calcDuration(parsedStartTime, endTime)

                scope.launch(Dispatchers.IO) {
                    commitObservation(
                        observationDao = observationDao,
                        location = location,
                        dateTime = parsedStartTime,
                        duration = duration
                    )
                }

                navController.navigate(HerptofaunaScreen.HomePage.route) {
                    popUpTo(HerptofaunaScreen.HomePage.route) { inclusive = true }
                }
            }
        ) {
            Text("Confirm & Submit")
        }
    }
}