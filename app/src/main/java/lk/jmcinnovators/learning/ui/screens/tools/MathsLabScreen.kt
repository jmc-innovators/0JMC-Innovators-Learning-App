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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathsLabScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Geometry", "Number Theory", "Calculator", "Converter")
    val tabIcons = listOf(
        Icons.Default.Category,
        Icons.Default.Functions,
        Icons.Default.Calculate,
        Icons.Default.SwapHoriz
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maths Lab", fontWeight = FontWeight.Bold) },
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
                0 -> GeometryTab()
                1 -> NumberTheoryTab()
                2 -> CalculatorTab()
                3 -> UnitConverterTab()
            }
        }
    }
}

/* ============================================================
   1. GEOMETRY TAB
   ============================================================ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeometryTab() {
    val shapes = listOf("Circle", "Triangle", "Rectangle", "Square", "Cylinder", "Sphere")
    var selectedShape by remember { mutableStateOf("Circle") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Select Shape", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shapes.forEach { shape ->
                    FilterChip(
                        selected = selectedShape == shape,
                        onClick = { selectedShape = shape },
                        label = { Text(shape) }
                    )
                }
            }
        }

        item {
            when (selectedShape) {
                "Circle" -> CircleCalculator()
                "Triangle" -> TriangleCalculator()
                "Rectangle" -> RectangleCalculator()
                "Square" -> SquareCalculator()
                "Cylinder" -> CylinderCalculator()
                "Sphere" -> SphereCalculator()
            }
        }
    }
}

@Composable
private fun CircleCalculator() {
    var radiusStr by remember { mutableStateOf("5") }
    val radius = radiusStr.toDoubleOrNull() ?: 0.0
    val area = PI * radius * radius
    val circumference = 2 * PI * radius

    ShapeCard(title = "Circle (Radius r = $radius)") {
        OutlinedTextField(
            value = radiusStr,
            onValueChange = { radiusStr = it },
            label = { Text("Radius (r)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        ResultRow(label = "Formula (Area)", formula = "A = π × r²")
        ResultRow(label = "Area", result = String.format("%.4f", area))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ResultRow(label = "Formula (Circumference)", formula = "C = 2 × π × r")
        ResultRow(label = "Circumference", result = String.format("%.4f", circumference))
    }
}

@Composable
private fun TriangleCalculator() {
    var baseStr by remember { mutableStateOf("6") }
    var heightStr by remember { mutableStateOf("4") }
    val base = baseStr.toDoubleOrNull() ?: 0.0
    val height = heightStr.toDoubleOrNull() ?: 0.0
    val area = 0.5 * base * height

    ShapeCard(title = "Triangle (Base × Height)") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = baseStr,
                onValueChange = { baseStr = it },
                label = { Text("Base (b)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightStr,
                onValueChange = { heightStr = it },
                label = { Text("Height (h)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        ResultRow(label = "Formula (Area)", formula = "A = ½ × b × h")
        ResultRow(label = "Area", result = String.format("%.4f", area))
    }
}

@Composable
private fun RectangleCalculator() {
    var lengthStr by remember { mutableStateOf("8") }
    var widthStr by remember { mutableStateOf("5") }
    val length = lengthStr.toDoubleOrNull() ?: 0.0
    val width = widthStr.toDoubleOrNull() ?: 0.0
    val area = length * width
    val perimeter = 2 * (length + width)

    ShapeCard(title = "Rectangle (Length × Width)") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = lengthStr,
                onValueChange = { lengthStr = it },
                label = { Text("Length (l)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = widthStr,
                onValueChange = { widthStr = it },
                label = { Text("Width (w)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        ResultRow(label = "Area Formula", formula = "A = l × w")
        ResultRow(label = "Area", result = String.format("%.4f", area))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ResultRow(label = "Perimeter Formula", formula = "P = 2 × (l + w)")
        ResultRow(label = "Perimeter", result = String.format("%.4f", perimeter))
    }
}

@Composable
private fun SquareCalculator() {
    var sideStr by remember { mutableStateOf("4") }
    val side = sideStr.toDoubleOrNull() ?: 0.0
    val area = side * side
    val perimeter = 4 * side

    ShapeCard(title = "Square (Side s = $side)") {
        OutlinedTextField(
            value = sideStr,
            onValueChange = { sideStr = it },
            label = { Text("Side length (s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        ResultRow(label = "Area Formula", formula = "A = s²")
        ResultRow(label = "Area", result = String.format("%.4f", area))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ResultRow(label = "Perimeter Formula", formula = "P = 4s")
        ResultRow(label = "Perimeter", result = String.format("%.4f", perimeter))
    }
}

@Composable
private fun CylinderCalculator() {
    var radiusStr by remember { mutableStateOf("3") }
    var heightStr by remember { mutableStateOf("7") }
    val radius = radiusStr.toDoubleOrNull() ?: 0.0
    val height = heightStr.toDoubleOrNull() ?: 0.0
    val volume = PI * radius * radius * height
    val surfaceArea = 2 * PI * radius * (radius + height)

    ShapeCard(title = "Cylinder (Radius r, Height h)") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = radiusStr,
                onValueChange = { radiusStr = it },
                label = { Text("Radius (r)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightStr,
                onValueChange = { heightStr = it },
                label = { Text("Height (h)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        ResultRow(label = "Volume Formula", formula = "V = π × r² × h")
        ResultRow(label = "Volume", result = String.format("%.4f", volume))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ResultRow(label = "Total Surface Area", formula = "A = 2πr(r + h)")
        ResultRow(label = "Surface Area", result = String.format("%.4f", surfaceArea))
    }
}

@Composable
private fun SphereCalculator() {
    var radiusStr by remember { mutableStateOf("4") }
    val radius = radiusStr.toDoubleOrNull() ?: 0.0
    val volume = (4.0 / 3.0) * PI * radius.pow(3.0)
    val surfaceArea = 4 * PI * radius * radius

    ShapeCard(title = "Sphere (Radius r = $radius)") {
        OutlinedTextField(
            value = radiusStr,
            onValueChange = { radiusStr = it },
            label = { Text("Radius (r)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        ResultRow(label = "Volume Formula", formula = "V = ⁴⁄₃ × π × r³")
        ResultRow(label = "Volume", result = String.format("%.4f", volume))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ResultRow(label = "Surface Area", formula = "A = 4 × π × r²")
        ResultRow(label = "Surface Area", result = String.format("%.4f", surfaceArea))
    }
}

@Composable
private fun ShapeCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ResultRow(label: String, result: String? = null, formula: String? = null) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (formula != null) {
            Text(
                formula,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (result != null) {
            Text(
                result,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* ============================================================
   2. NUMBER THEORY TAB
   ============================================================ */
@Composable
private fun NumberTheoryTab() {
    var num1Str by remember { mutableStateOf("24") }
    var num2Str by remember { mutableStateOf("36") }
    var primeNumStr by remember { mutableStateOf("29") }

    val n1 = num1Str.toLongOrNull() ?: 0L
    val n2 = num2Str.toLongOrNull() ?: 0L
    val gcd = if (n1 > 0 && n2 > 0) calculateGCD(n1, n2) else 0L
    val lcm = if (n1 > 0 && n2 > 0) (n1 / gcd) * n2 else 0L

    val testPrime = primeNumStr.toLongOrNull() ?: 0L
    val isPrime = if (testPrime > 1) checkIsPrime(testPrime) else false
    val primeFactors = if (testPrime > 1) getPrimeFactors(testPrime) else emptyList()

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
                    Text("HCF (GCD) & LCM Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = num1Str,
                            onValueChange = { num1Str = it },
                            label = { Text("First Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = num2Str,
                            onValueChange = { num2Str = it },
                            label = { Text("Second Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    ResultRow("Highest Common Factor (HCF / GCD)", result = gcd.toString())
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    ResultRow("Lowest Common Multiple (LCM)", result = lcm.toString())
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Prime Checker & Factorization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = primeNumStr,
                        onValueChange = { primeNumStr = it },
                        label = { Text("Enter a positive integer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prime Status", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (testPrime <= 1) "Neither Prime nor Composite"
                            else if (isPrime) "✓ PRIME NUMBER" else "✕ COMPOSITE NUMBER",
                            fontWeight = FontWeight.Bold,
                            color = if (isPrime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prime Factorization", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (primeFactors.isEmpty()) "N/A" else primeFactors.joinToString(" × "),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun calculateGCD(a: Long, b: Long): Long {
    var x = a
    var y = b
    while (y != 0L) {
        val temp = y
        y = x % y
        x = temp
    }
    return x
}

private fun checkIsPrime(n: Long): Boolean {
    if (n < 2) return false
    if (n == 2L || n == 3L) return true
    if (n % 2L == 0L || n % 3L == 0L) return false
    var i = 5L
    while (i * i <= n) {
        if (n % i == 0L || n % (i + 2L) == 0L) return false
        i += 6
    }
    return true
}

private fun getPrimeFactors(number: Long): List<Long> {
    var n = number
    val factors = mutableListOf<Long>()
    while (n % 2L == 0L) {
        factors.add(2L)
        n /= 2L
    }
    var i = 3L
    while (i * i <= n) {
        while (n % i == 0L) {
            factors.add(i)
            n /= i
        }
        i += 2L
    }
    if (n > 2L) factors.add(n)
    return factors
}

/* ============================================================
   3. CALCULATOR TAB
   ============================================================ */
@Composable
private fun CalculatorTab() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }

    val buttons = listOf(
        listOf("C", "⌫", "(", ")"),
        listOf("√", "^", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "±", "=")
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = result,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Keypad
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttons.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { btn ->
                        val isOp = btn in listOf("÷", "×", "-", "+", "=", "^", "√", "%")
                        val isAction = btn in listOf("C", "⌫")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        btn == "=" -> MaterialTheme.colorScheme.primary
                                        isOp -> MaterialTheme.colorScheme.primaryContainer
                                        isAction -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable {
                                    when (btn) {
                                        "C" -> {
                                            expression = ""
                                            result = "0"
                                        }
                                        "⌫" -> {
                                            if (expression.isNotEmpty()) expression = expression.dropLast(1)
                                        }
                                        "=" -> {
                                            result = evaluateSimpleExpression(expression)
                                        }
                                        "±" -> {
                                            if (expression.startsWith("-")) expression = expression.drop(1)
                                            else if (expression.isNotEmpty()) expression = "-$expression"
                                        }
                                        "√" -> {
                                            val currentVal = expression.toDoubleOrNull()
                                            if (currentVal != null && currentVal >= 0) {
                                                val sqrtRes = sqrt(currentVal)
                                                result = String.format("%.4f", sqrtRes).trimEnd('0').trimEnd('.')
                                                expression = result
                                            } else {
                                                expression += "sqrt("
                                            }
                                        }
                                        else -> expression += btn
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    btn == "=" -> MaterialTheme.colorScheme.onPrimary
                                    isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isAction -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun evaluateSimpleExpression(expr: String): String {
    return try {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        // Tokenize and evaluate basic arithmetic
        val tokens = mutableListOf<String>()
        var currentNum = StringBuilder()
        for (ch in sanitized) {
            if (ch.isDigit() || ch == '.') {
                currentNum.append(ch)
            } else if (ch in listOf('+', '-', '*', '/', '%', '^')) {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum.toString())
                    currentNum = StringBuilder()
                }
                tokens.add(ch.toString())
            }
        }
        if (currentNum.isNotEmpty()) tokens.add(currentNum.toString())

        if (tokens.isEmpty()) return "0"

        // First pass: exponent
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] == "^") {
                val prev = tokens[i - 1].toDouble()
                val next = tokens[i + 1].toDouble()
                val res = prev.pow(next)
                tokens[i - 1] = res.toString()
                tokens.removeAt(i)
                tokens.removeAt(i)
                i--
            }
            i++
        }

        // Second pass: multiplication, division, modulo
        i = 0
        while (i < tokens.size) {
            if (tokens[i] in listOf("*", "/", "%")) {
                val op = tokens[i]
                val prev = tokens[i - 1].toDouble()
                val next = tokens[i + 1].toDouble()
                val res = when (op) {
                    "*" -> prev * next
                    "/" -> if (next == 0.0) return "Error (div 0)" else prev / next
                    "%" -> prev % next
                    else -> 0.0
                }
                tokens[i - 1] = res.toString()
                tokens.removeAt(i)
                tokens.removeAt(i)
                i--
            }
            i++
        }

        // Third pass: addition, subtraction
        var finalResult = tokens[0].toDouble()
        i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val next = tokens[i + 1].toDouble()
            finalResult = when (op) {
                "+" -> finalResult + next
                "-" -> finalResult - next
                else -> finalResult
            }
            i += 2
        }

        val rounded = String.format("%.6f", finalResult).trimEnd('0').trimEnd('.')
        rounded
    } catch (e: Exception) {
        "Syntax Error"
    }
}

/* ============================================================
   4. UNIT CONVERTER TAB
   ============================================================ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitConverterTab() {
    val categories = listOf("Length", "Mass", "Temperature", "Speed")
    var selectedCategory by remember { mutableStateOf("Length") }

    var inputValStr by remember { mutableStateOf("10") }
    val inputVal = inputValStr.toDoubleOrNull() ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = inputValStr,
                onValueChange = { inputValStr = it },
                label = { Text("Value to Convert") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$selectedCategory Conversions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    when (selectedCategory) {
                        "Length" -> {
                            val meters = inputVal
                            ResultRow("Kilometers (km)", String.format("%.4f", meters / 1000.0))
                            ResultRow("Meters (m)", String.format("%.2f", meters))
                            ResultRow("Centimeters (cm)", String.format("%.2f", meters * 100.0))
                            ResultRow("Millimeters (mm)", String.format("%.2f", meters * 1000.0))
                            ResultRow("Inches (in)", String.format("%.2f", meters * 39.3701))
                            ResultRow("Feet (ft)", String.format("%.2f", meters * 3.28084))
                            ResultRow("Miles (mi)", String.format("%.4f", meters * 0.000621371))
                        }
                        "Mass" -> {
                            val kg = inputVal
                            ResultRow("Kilograms (kg)", String.format("%.3f", kg))
                            ResultRow("Grams (g)", String.format("%.2f", kg * 1000.0))
                            ResultRow("Milligrams (mg)", String.format("%.1f", kg * 1000000.0))
                            ResultRow("Pounds (lbs)", String.format("%.3f", kg * 2.20462))
                            ResultRow("Ounces (oz)", String.format("%.3f", kg * 35.274))
                        }
                        "Temperature" -> {
                            val c = inputVal
                            val f = (c * 9.0 / 5.0) + 32.0
                            val k = c + 273.15
                            ResultRow("Celsius (°C)", String.format("%.2f °C", c))
                            ResultRow("Fahrenheit (°F)", String.format("%.2f °F", f))
                            ResultRow("Kelvin (K)", String.format("%.2f K", k))
                        }
                        "Speed" -> {
                            val kmh = inputVal
                            val ms = kmh / 3.6
                            val mph = kmh * 0.621371
                            ResultRow("km/h", String.format("%.2f km/h", kmh))
                            ResultRow("m/s", String.format("%.2f m/s", ms))
                            ResultRow("mph", String.format("%.2f mph", mph))
                        }
                    }
                }
            }
        }
    }
}
