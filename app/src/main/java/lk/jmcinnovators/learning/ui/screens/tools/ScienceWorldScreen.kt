package lk.jmcinnovators.learning.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

data class ElementData(
    val number: Int,
    val symbol: String,
    val name: String,
    val mass: Double,
    val category: String,
    val phase: String,
    val electronConfig: String,
    val summary: String
)

private val SAMPLE_ELEMENTS = listOf(
    ElementData(1, "H", "Hydrogen", 1.008, "Nonmetal", "Gas", "1s¹", "Lightest and most abundant chemical substance in the Universe."),
    ElementData(2, "He", "Helium", 4.0026, "Noble Gas", "Gas", "1s²", "Colorless, odorless, inert monatomic gas that heads the noble gas series."),
    ElementData(3, "Li", "Lithium", 6.94, "Alkali Metal", "Solid", "[He] 2s¹", "Soft, silvery-white alkali metal with the lowest density of all metals."),
    ElementData(4, "Be", "Beryllium", 9.0122, "Alkaline Earth", "Solid", "[He] 2s²", "Relatively rare metal in the universe, often forming beryl gemstones."),
    ElementData(5, "B", "Boron", 10.81, "Metalloid", "Solid", "[He] 2s² 2p¹", "Low-abundance metalloid used in fiberglass and semiconductors."),
    ElementData(6, "C", "Carbon", 12.011, "Nonmetal", "Solid", "[He] 2s² 2p²", "Basis of all organic chemistry and life on Earth, forming graphite and diamond."),
    ElementData(7, "N", "Nitrogen", 14.007, "Nonmetal", "Gas", "[He] 2s² 2p³", "Forms about 78% of Earth's atmosphere, essential component of amino acids."),
    ElementData(8, "O", "Oxygen", 15.999, "Nonmetal", "Gas", "[He] 2s² 2p⁴", "Highly reactive nonmetal forming 21% of atmosphere and essential for aerobic respiration."),
    ElementData(9, "F", "Fluorine", 18.998, "Halogen", "Gas", "[He] 2s² 2p⁵", "Extremely reactive, poisonous halogen and the most electronegative element."),
    ElementData(10, "Ne", "Neon", 20.180, "Noble Gas", "Gas", "[He] 2s² 2p⁶", "Gives an orange-red glow in high-voltage electrical discharge signs."),
    ElementData(11, "Na", "Sodium", 22.990, "Alkali Metal", "Solid", "[Ne] 3s¹", "Highly reactive soft metal; major cation in extracellular fluid (table salt)."),
    ElementData(12, "Mg", "Magnesium", 24.305, "Alkaline Earth", "Solid", "[Ne] 3s²", "Shiny gray solid, essential mineral for enzymes and chlorophyll center."),
    ElementData(13, "Al", "Aluminium", 26.982, "Post-transition", "Solid", "[Ne] 3s² 3p¹", "Low-density metal widely used in aerospace, construction, and packaging."),
    ElementData(14, "Si", "Silicon", 28.085, "Metalloid", "Solid", "[Ne] 3s² 3p²", "Hard brittle crystalline metalloid that powers the modern semiconductor industry."),
    ElementData(15, "P", "Phosphorus", 30.974, "Nonmetal", "Solid", "[Ne] 3s² 3p³", "Essential for life in DNA, RNA, ATP, and cell membranes."),
    ElementData(16, "S", "Sulfur", 32.06, "Nonmetal", "Solid", "[Ne] 3s² 3p⁴", "Bright yellow crystalline solid used in gunpowder, matches, and sulfuric acid."),
    ElementData(17, "Cl", "Chlorine", 35.45, "Halogen", "Gas", "[Ne] 3s² 3p⁵", "Yellow-green gas used as a powerful disinfectant and water purification agent."),
    ElementData(18, "Ar", "Argon", 39.948, "Noble Gas", "Gas", "[Ne] 3s² 3p⁶", "Third-most abundant gas in the atmosphere, used as inert shielding in welding."),
    ElementData(19, "K", "Potassium", 39.098, "Alkali Metal", "Solid", "[Ar] 4s¹", "Crucial electrolyte for biological nerve transmission and muscle contraction."),
    ElementData(20, "Ca", "Calcium", 40.078, "Alkaline Earth", "Solid", "[Ar] 4s²", "Most abundant metal in the human body, vital for bones, teeth, and cellular signals."),
    ElementData(26, "Fe", "Iron", 55.845, "Transition Metal", "Solid", "[Ar] 3d⁶ 4s²", "Most common element on Earth by mass; core component of hemoglobin and steel."),
    ElementData(29, "Cu", "Copper", 63.546, "Transition Metal", "Solid", "[Ar] 3d¹⁰ 4s¹", "Soft, ductile metal with very high thermal and electrical conductivity."),
    ElementData(30, "Zn", "Zinc", 65.38, "Transition Metal", "Solid", "[Ar] 3d¹⁰ 4s²", "Essential trace element used for galvanizing steel to prevent corrosion."),
    ElementData(47, "Ag", "Silver", 107.87, "Transition Metal", "Solid", "[Kr] 4d¹⁰ 5s¹", "Precious metal exhibiting highest electrical and thermal conductivity of any metal."),
    ElementData(79, "Au", "Gold", 196.97, "Transition Metal", "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s¹", "Dense, shiny, corrosion-resistant precious metal prized throughout history."),
    ElementData(80, "Hg", "Mercury", 200.59, "Transition Metal", "Liquid", "[Xe] 4f¹⁴ 5d¹⁰ 6s²", "Only metallic element that is liquid at standard temperature and pressure.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceWorldScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Periodic Table", "Physics Solvers", "Chemistry Solvers")
    val tabIcons = listOf(
        Icons.Default.Science,
        Icons.Default.Speed,
        Icons.Default.Science
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Science World", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, maxLines = 1) },
                        icon = { Icon(tabIcons[index], contentDescription = title, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PeriodicTableTab()
                1 -> PhysicsSolversTab()
                2 -> ChemistrySolversTab()
            }
        }
    }
}

/* ============================================================
   1. PERIODIC TABLE TAB
   ============================================================ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodicTableTab() {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedElement by remember { mutableStateOf<ElementData?>(null) }

    val categories = listOf("All", "Nonmetal", "Noble Gas", "Alkali Metal", "Alkaline Earth", "Transition Metal", "Metalloid", "Post-transition", "Halogen")

    val filtered = SAMPLE_ELEMENTS.filter { el ->
        val matchesQuery = query.isBlank() ||
                el.name.contains(query, ignoreCase = true) ||
                el.symbol.contains(query, ignoreCase = true) ||
                el.number.toString() == query.trim()
        val matchesCat = selectedCategory == "All" || el.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCat
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by name, symbol, or atomic number…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 90.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { el ->
                ElementCard(element = el, onClick = { selectedElement = el })
            }
        }
    }

    selectedElement?.let { el ->
        ElementDetailDialog(element = el, onDismiss = { selectedElement = null })
    }
}

@Composable
private fun ElementCard(element: ElementData, onClick: () -> Unit) {
    val categoryColor = when (element.category) {
        "Noble Gas" -> Color(0xFF673AB7)
        "Alkali Metal" -> Color(0xFFE91E63)
        "Alkaline Earth" -> Color(0xFFFF9800)
        "Transition Metal" -> Color(0xFF2196F3)
        "Metalloid" -> Color(0xFF4CAF50)
        "Halogen" -> Color(0xFF009688)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${element.number}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = categoryColor)
                Text(element.phase, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(element.symbol, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(element.name, fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ElementDetailDialog(element: ElementData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(element.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(element.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Atomic Number: ${element.number} | ${element.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Standard Atomic Weight", "${element.mass} u")
                DetailRow("State at STP", element.phase)
                DetailRow("Electron Configuration", element.electronConfig)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("About:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(element.summary, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

/* ============================================================
   2. PHYSICS SOLVERS TAB
   ============================================================ */
@Composable
private fun PhysicsSolversTab() {
    var uStr by remember { mutableStateOf("0") }
    var aStr by remember { mutableStateOf("9.8") }
    var tStr by remember { mutableStateOf("5") }

    val u = uStr.toDoubleOrNull() ?: 0.0
    val a = aStr.toDoubleOrNull() ?: 0.0
    val t = tStr.toDoubleOrNull() ?: 0.0

    // v = u + at
    val v = u + a * t
    // s = ut + 0.5 * a * t^2
    val s = u * t + 0.5 * a * t * t

    var massStr by remember { mutableStateOf("10") }
    var accStr by remember { mutableStateOf("2.5") }
    val mass = massStr.toDoubleOrNull() ?: 0.0
    val acc = accStr.toDoubleOrNull() ?: 0.0
    val force = mass * acc

    var voltStr by remember { mutableStateOf("12") }
    var resStr by remember { mutableStateOf("4") }
    val volt = voltStr.toDoubleOrNull() ?: 0.0
    val res = resStr.toDoubleOrNull() ?: 1.0
    val current = if (res != 0.0) volt / res else 0.0
    val power = volt * current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Equations of Motion (Kinematics)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uStr,
                            onValueChange = { uStr = it },
                            label = { Text("Initial Vel (u, m/s)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = aStr,
                            onValueChange = { aStr = it },
                            label = { Text("Acc (a, m/s²)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tStr,
                            onValueChange = { tStr = it },
                            label = { Text("Time (t, s)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Final Velocity: v = u + at", String.format("%.2f m/s", v))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    DetailRow("Displacement: s = ut + ½at²", String.format("%.2f m", s))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Newton's Second Law: F = m × a", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = massStr,
                            onValueChange = { massStr = it },
                            label = { Text("Mass (m, kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = accStr,
                            onValueChange = { accStr = it },
                            label = { Text("Acceleration (m/s²)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Resultant Force (F)", String.format("%.2f N (Newtons)", force))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Electricity: Ohm's Law & Power", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = voltStr,
                            onValueChange = { voltStr = it },
                            label = { Text("Voltage (V, Volts)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = resStr,
                            onValueChange = { resStr = it },
                            label = { Text("Resistance (R, Ω)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Current (I = V / R)", String.format("%.3f A (Amperes)", current))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    DetailRow("Power (P = V × I)", String.format("%.2f W (Watts)", power))
                }
            }
        }
    }
}

/* ============================================================
   3. CHEMISTRY SOLVERS TAB
   ============================================================ */
@Composable
private fun ChemistrySolversTab() {
    var mStr by remember { mutableStateOf("250") }
    var vStr by remember { mutableStateOf("50") }

    val mass = mStr.toDoubleOrNull() ?: 0.0
    val volume = vStr.toDoubleOrNull() ?: 1.0
    val density = if (volume != 0.0) mass / volume else 0.0

    var soluteStr by remember { mutableStateOf("15") }
    var solventStr by remember { mutableStateOf("85") }
    val solute = soluteStr.toDoubleOrNull() ?: 0.0
    val solvent = solventStr.toDoubleOrNull() ?: 0.0
    val totalSolution = solute + solvent
    val massPercent = if (totalSolution > 0) (solute / totalSolution) * 100.0 else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Density Calculator: ρ = m / V", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mStr,
                            onValueChange = { mStr = it },
                            label = { Text("Mass (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vStr,
                            onValueChange = { vStr = it },
                            label = { Text("Volume (cm³)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Density (ρ)", String.format("%.4f g/cm³ (or kg/L)", density))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Solution Concentration (Mass %)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = soluteStr,
                            onValueChange = { soluteStr = it },
                            label = { Text("Solute mass (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = solventStr,
                            onValueChange = { solventStr = it },
                            label = { Text("Solvent mass (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Concentration (% m/m)", String.format("%.2f %%", massPercent))
                }
            }
        }
    }
}
