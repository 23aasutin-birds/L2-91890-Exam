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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.OnConflictStrategy
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.ktx.model.cameraPosition
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


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
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(checklist: Checklist)
}

// Species table
@Entity (tableName = "species_data")
data class Species(
    @PrimaryKey (autoGenerate = false)
    val speciesId: Int,
    val scientificName: String,
    val englishName: String,
    val speciesComment: String,
)

@Dao
interface SpeciesDao {
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecies(species: Species)

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
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val speciesDao = getDatabase(context).speciesDao()
                                speciesData.forEach { species ->
                                    speciesDao.insertSpecies(species)
                                }
                            }
                        }
                    })

                .build()
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
    val surveyViewModel: SurveyViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = HerptofaunaScreen.HomePage.route
    ) {
        composable(route = HerptofaunaScreen.HomePage.route) {
            HomePageLayout(
                navController = navController,
                onStartSurvey = { startTime ->
                    surveyViewModel.startSurvey(startTime)
                    navController.navigate("checklist_page/$startTime")
                }
            )
        }
        composable(route = HerptofaunaScreen.ChecklistPage.route) { backStackEntry ->
            val startTimeString = backStackEntry.arguments?.getString("startTime") ?: ""
            ChecklistPageLayout(
                navController = navController,
                startTimeString = startTimeString,
                viewModel = surveyViewModel
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
                surveyStartTime = startTimeString,
                viewModel = surveyViewModel
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

suspend fun commitChecklist(
    checklistDao: ChecklistDao,
    count: Int,
    userComment: String,
    eventId: Int,
    speciesId: Int
) {
    val newChecklist = Checklist(
        eventId = eventId,
        speciesId = speciesId,
        count = count,
        userComment = "",
        image = "") // This has got to be a list will all the data in it
    checklistDao.insertChecklistItems(newChecklist)
    }

class SurveyViewModel : ViewModel() {
    var surveyStartTime: String = ""
        private set

    private val _speciesCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val speciesCounts: StateFlow<Map<Int, Int>> = _speciesCounts.asStateFlow()

    fun startSurvey(time: String) {
        surveyStartTime = time
        _speciesCounts.value = emptyMap()
    }

    fun incrementSpecies(speciesId: Int) {
        _speciesCounts.update { currentMap ->
            val count = currentMap[speciesId] ?: 0
            currentMap + (speciesId to count + 1)
        }
    }

    fun decrementSpecies(speciesId: Int) {
        _speciesCounts.update { currentMap ->
            val count = currentMap[speciesId] ?: 0
            if (count <= 1) {
                currentMap - speciesId
            } else {
                currentMap + (speciesId to count - 1)
            }
        }
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
    Species( 2, "Oligosoma inconspicuum", "Cyptic Skink", ""),
    Species( 3, "Oligosoma judgei", "Barrier Skink", ""),
    Species( 4, "Oligosoma maccanni", "McCann's Skink", ""),
    Species( 5, "Oligosoma otagense", "Otago Skink", ""),
    Species( 6, "Oligosoma polychroma", "New Zealand Grass Skink", ""),
    Species( 7, "Oligosoma toka", "Nevis Skink", ""),
    Species( 8, "Woodwirthia \"Cromwell\"", "Kawarau Gecko", ""),
    Species( 9, "Woodworthia \"Central Otago\"", "Schist Gecko", ""),
    Species( 10, "Woodworthia \"Otago/Southland large\"", "Korero Gecko", ""),
    Species( 11, "Woodworthia \"southern alps\"", "Southern Alps Gecko", ""),
    Species( 12, "Woodworthia \"southern mini\"", "Short-toed Gecko", ""),
    Species( 13, "Woodworthia \"Raggedy Range\"", "Raggedy Range Gecko", ""),
    Species( 14, "Woodworthia \"south-western\"", "Mountain Beech Gecko", ""),
    )

// Displays species in list
@Composable
fun SpeciesDisplayRow(
    species: Species,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .weight(3f)
        ) {
            Text(
                text = "${species.englishName} (${species.scientificName})",
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(modifier = Modifier
            .weight(1f)
        ) {
            Text(
                text = "${count}"
            )
        }

    }

}

// Uses species data to make table
@Composable
fun SpeciesTable(
    speciesData: List<Species>,
    viewModel: SurveyViewModel
) {
    val speciesCounts by viewModel.speciesCounts.collectAsState()

    Column(modifier = Modifier) {

        // Upper Half
        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
        ) {
            if (speciesCounts.isEmpty()) {
                // If empty
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "No species selected yet! Add individuals using counters bellow to create your list.",
                        modifier = Modifier.padding(16.dp)

                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(speciesCounts.toList()) { (speciesId, count) ->
                        val species = speciesData.find { it.speciesId == speciesId }
                        if (species != null) {
                            SpeciesDisplayRow(
                                species = species,
                                count = count
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Lower Half
        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
        ) {
            HorizontalDivider()
            LazyColumn {
                items(speciesData) { species ->
                    val count = speciesCounts[species.speciesId] ?: 0

                    SpeciesRow(
                        species = species,
                        count = count,
                        onIncrement = { viewModel.incrementSpecies(species.speciesId) },
                        onDecrement = { viewModel.decrementSpecies(species.speciesId) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// Function making each row
@Composable
fun SpeciesRow(
    species: Species,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .weight(1f)
        ) {
            Text(
                text = "${species.englishName} (${species.scientificName})",
                overflow = TextOverflow.Ellipsis,
            )
        }

        SpeciesCounter(
            count = count,
            onIncrement = onIncrement,
            onDecrement = onDecrement
        )
    }

}

// Count section (seperated for simplicity)
@Composable
fun SpeciesCounter(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = count > 0,
            modifier = Modifier.size(32.dp)
        ) {
            Text("-1")
        }
        Text(
            text = "$count",
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(32.dp)

        ) {
            Text("+1")
        }
    }
}

// Using lazy column to make the table (more useful with viewModel latter)
@Composable
fun SpeciesScreen(
    speciesData: List<Species>,
    viewModel: SurveyViewModel
) {
    Column(modifier = Modifier
            .fillMaxSize()
    ) {
        SpeciesTable(
            speciesData = speciesData,
            viewModel = viewModel
        )
    }
}

// All layouts (4 pages)

@Composable
fun HomePageLayout(
    navController : NavController,
    onStartSurvey: (String) -> Unit,
    modifier: Modifier = Modifier
) {

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
                    onStartSurvey(startTime)
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
                    "Privacy Statement: your location will be collected while using the app but will only be viewed by certified administrators and researchers.",
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
    viewModel: SurveyViewModel,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Your Survey")},
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
                    icon = { Icon(Icons.Filled.Done,
                            contentDescription = "End Survey",
                        ) },
                    label = { Text("Stop Survey")
                    }
                )
            }
        }
    ) { innerPadding ->

        val context = LocalContext.current
        val database = remember { HerptofaunaDatabase.getDatabase(context) }
        val observationDao = database.observationDao()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier) {
                SpeciesScreen(
                    speciesData = speciesData,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SpeciesPageLayout(
    navController: NavController,
    modifier: Modifier = Modifier) {

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitPageLayout(
    navController: NavController,
    surveyStartTime: String,
    viewModel: SurveyViewModel,
    modifier: Modifier = Modifier
) {
    // Collects species counts from view model
    val speciesCounts by viewModel.speciesCounts.collectAsState()

    // Collects LocalDateTime
    val context = LocalContext.current

    // Get a coroutine scope tied to this Composable's lifecycle
    val scope = rememberCoroutineScope()

    // Accesses the database
    val database = remember { HerptofaunaDatabase.getDatabase(context) }
    val observationDao = database.observationDao()

    // Getting parsed start time
    val parsedStartTime = try {
        LocalDateTime.parse(surveyStartTime)
    } catch (e: Exception) {
        LocalDateTime.now() // Fallback to regular time if parsing fails, will change latter
    }
    // Getting end time
    val endTime = LocalDateTime.now()
    // Calculating duration
    var duration by remember { mutableStateOf("") } // For collection of duration
    LaunchedEffect(surveyStartTime) {
        val parsedStartTime = try {
            LocalDateTime.parse(surveyStartTime)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
        duration = calcDuration(parsedStartTime, endTime).toString()
    }

    // Date Time picker
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedDateTime by remember { mutableStateOf(parsedStartTime) }
    val formattedDateTime = remember(selectedDateTime) {
        selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    // Google Maps
    var mapLocation by remember { mutableStateOf<LatLng?>(null) }
    var location by remember { mutableStateOf("") } // For collection of location
    val defaultLocation = LatLng(-45.0312, 168.6626)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    // Validation rules
    val isDurationValid = duration.toIntOrNull() != null && duration.toInt() >= 0
    val isLocationValid = location.isNotBlank()

    // Overall Completion
    val isFormValid = isDurationValid && isLocationValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add details") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    enabled = isFormValid,
                    onClick = {

                        val durationInt = duration.toIntOrNull() ?: 0

                        scope.launch(Dispatchers.IO) {
                            val currentEventId = observationDao.insertObservation(
                                Observation(
                                    location = location,
                                    dateTime = selectedDateTime,
                                    duration = durationInt
                                )
                            ).toInt()

                            if(currentEventId > 0) {
                                val checklistDao = database.checklistDao()
                                speciesCounts.forEach { (speciesId, count) ->
                                    commitChecklist(
                                        checklistDao = checklistDao,
                                        count = count,
                                        userComment = "",
                                        eventId = currentEventId,
                                        speciesId = speciesId,

                                        )
                                }
                            }

                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                navController.navigate(HerptofaunaScreen.HomePage.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Done,
                            contentDescription = "Confirm",
                        )
                    },
                    label = {
                        Text("Confirm")
                    }
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Google Maps
            Box(
                modifier = Modifier.weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng -> mapLocation = latLng }
                ) {
                    mapLocation?.let { pinLocation ->
                        Marker(
                            state = rememberUpdatedMarkerState(position = pinLocation),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Location Textbox
                OutlinedTextField(
                    value = location,
                    onValueChange = { newText -> location = newText },
                    label = { Text("Select location") }
                )

                // Date/Time
                Box(
                    modifier = Modifier
                ) {
                    OutlinedTextField(
                        value = formattedDateTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select date & time (yyyy-MM-dd HH:mm)") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date & time"
                            )
                        },
                    )

                    // Pop-up Calendar
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showDatePicker = true
                            }
                    )
                }

                // Duration
                OutlinedTextField(
                    value = duration,
                    onValueChange = { newValue -> duration = newValue.filter { it.isDigit() } },
                    label = { Text("Ente5 duration of count (minutes)") },
                )
            }
        }
    }

    // Date picker stuff
    if(showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedLocalDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        selectedDateTime = selectedDateTime
                            .withYear(selectedLocalDate.year)
                            .withMonth(selectedLocalDate.monthValue)
                            .withDayOfMonth(selectedLocalDate.dayOfMonth)
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("Ok")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker stuff
    if(showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTime.hour,
            initialMinute = selectedDateTime.minute,
            is24Hour = true

        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateTime = selectedDateTime
                        .withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute)
                    showTimePicker = false
                }) { Text("Ok") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

}