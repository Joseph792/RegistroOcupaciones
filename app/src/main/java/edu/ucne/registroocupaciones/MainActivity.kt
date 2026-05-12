package edu.ucne.registroocupaciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import edu.ucne.registroocupaciones.ui.theme.RegistroOcupacionesTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var ocupacionDb: OcupacionDb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ocupacionDb = Room.databaseBuilder(
            applicationContext,
            OcupacionDb::class.java,
            "Ocupacion.db"
        ).fallbackToDestructiveMigration()
            .build()

        setContent {
            RegistroOcupacionesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        OcupacionScreen()
                    }
                }
            }
        }
    }

    @Composable
    fun OcupacionListScreen(ocupacionList: List<OcupacionEntity>) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Lista de Ocupaciones")
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(ocupacionList) {
                    OcupacionRow(it)
                }
            }
        }
    }

    @Composable
    private fun OcupacionRow(it: OcupacionEntity) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(modifier = Modifier.weight(1f), text = it.ocupacionId.toString())
            Text(
                modifier = Modifier.weight(2f),
                text = it.descripcion,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(modifier = Modifier.weight(1f), text = it.sueldo.toString())
        }
        HorizontalDivider()
    }

    @Entity(tableName = "Ocupaciones")
    data class OcupacionEntity(
        @PrimaryKey
        val ocupacionId: Int? = null,
        val descripcion: String = "",
        val sueldo: Int = 0
    )

    @Dao
    interface OcupacionDao {
        @Upsert()
        suspend fun save(ocupacion: OcupacionEntity)

        @Query(
            """
        SELECT * 
        FROM Ocupaciones 
        WHERE ocupacionId=:id  
        LIMIT 1
        """
        )
        suspend fun find(id: Int): OcupacionEntity?

        @Delete
        suspend fun delete(ocupacion: OcupacionEntity)

        @Query("SELECT * FROM Ocupaciones")
        fun getAll(): Flow<List<OcupacionEntity>>
    }

    @Database(
        entities = [
            OcupacionEntity::class
        ],
        version = 1,
        exportSchema = false
    )
    abstract class OcupacionDb : RoomDatabase() {
        abstract fun ocupacionDao(): OcupacionDao
    }

    @Composable
    fun OcupacionScreen(
    ) {
        var descripcion: String by remember { mutableStateOf("") }
        var sueldo: Int by remember { mutableStateOf(0) }
        var errorMessage: String? by remember { mutableStateOf(null) }

        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Registro de Ocupaciones")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {

                        OutlinedTextField(
                            label = { Text(text = "Descripcion") },
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            label = { Text(text = "Sueldo") },
                            value = sueldo.toString(),
                            onValueChange = { sueldo = it.toIntOrNull() ?: 0 },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        errorMessage?.let {
                            Text(text = it, color = Color.Red)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = {

                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "new button"
                                )
                                Text(text = "Nuevo")
                            }
                            val scope = rememberCoroutineScope()
                            OutlinedButton(
                                onClick = {
                                    if (descripcion.isBlank())
                                        errorMessage = "Descripcion vacia"

                                    scope.launch {
                                        saveOcupacion(
                                            OcupacionEntity(
                                                descripcion = descripcion,
                                                sueldo = sueldo
                                            )
                                        )
                                        descripcion = ""
                                        sueldo = 0
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "save button"
                                )
                                Text(text = "Guardar")
                            }
                        }
                    }
                }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val ocupacionList by ocupacionDb.ocupacionDao().getAll()
                    .collectAsStateWithLifecycle(
                        initialValue = emptyList(),
                        lifecycleOwner = lifecycleOwner,
                        minActiveState = Lifecycle.State.STARTED
                    )
                OcupacionListScreen(ocupacionList)
            }
        }
    }
    private suspend fun saveOcupacion(ocupacion: OcupacionEntity) {
        ocupacionDb.ocupacionDao().save(ocupacion)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RegistroOcupacionesTheme {
        Greeting("Android")
    }
}