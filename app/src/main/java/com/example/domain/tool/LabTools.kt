package com.example.domain.tool

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.FunctionDeclaration
import com.example.data.model.Schema
import com.example.data.model.Tool
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

interface LabTool {
    val name: String
    val displayName: String
    val description: String
    val icon: ImageVector
    val schema: Schema

    fun execute(args: Map<String, Any?>): Map<String, Any?>
}

object WeatherTool : LabTool {
    override val name = "getWeather"
    override val displayName = "Weather Forecaster"
    override val description = "Get the current weather and activity recommendation for a given location (city, state, or country)."
    override val icon = Icons.Default.Cloud

    override val schema = Schema(
        type = "OBJECT",
        properties = mapOf(
            "location" to Schema(
                type = "STRING",
                description = "The city and state or country, e.g. 'San Francisco, CA' or 'Tokyo, Japan'"
            )
        ),
        required = listOf("location")
    )

    override fun execute(args: Map<String, Any?>): Map<String, Any?> {
        val rawLocation = args["location"]?.toString() ?: "Unknown"
        val location = rawLocation.trim().lowercase()

        // Deterministic but dynamic weather based on location hash
        val hash = location.hashCode()
        val tempC = 15 + (Math.abs(hash) % 16) // range 15 to 30
        val humidity = 40 + (Math.abs(hash) % 46) // range 40 to 85
        
        val condition = when {
            Math.abs(hash) % 4 == 0 -> "Sunny & Clear"
            Math.abs(hash) % 4 == 1 -> "Partly Cloudy with gentle breeze"
            Math.abs(hash) % 4 == 2 -> "Showery & Rainy"
            else -> "Mist & Cool"
        }

        val recommendation = when (condition) {
            "Sunny & Clear" -> "An amazing day to go for an outdoor picnic or a run. Don't forget your sunglasses!"
            "Partly Cloudy with gentle breeze" -> "Perfect weather for sightseeing or a casual walk. Grab a light jacket!"
            "Showery & Rainy" -> "It is rainy. Ideal for cozying up in a local café with a hot drink or visiting a museum."
            else -> "Visibility is low. A wonderful time to read a book in a library or watch a movie."
        }

        return mapOf(
            "location" to rawLocation,
            "temperature" to "${tempC}°C / ${(tempC * 9/5) + 32}°F",
            "humidity" to "${humidity}%",
            "condition" to condition,
            "recommendation" to recommendation
        )
    }
}

object CurrencyTool : LabTool {
    override val name = "convertCurrency"
    override val displayName = "Currency Exchange"
    override val description = "Convert a monetary amount from one currency to another using the latest exchange rates."
    override val icon = Icons.Default.CurrencyExchange

    override val schema = Schema(
        type = "OBJECT",
        properties = mapOf(
            "amount" to Schema(
                type = "NUMBER",
                description = "The numeric amount of money to convert"
            ),
            "from" to Schema(
                type = "STRING",
                description = "The source currency 3-letter code, e.g. USD, EUR, GBP, JPY, INR"
            ),
            "to" to Schema(
                type = "STRING",
                description = "The target currency 3-letter code, e.g. USD, EUR, GBP, JPY, INR"
            )
        ),
        required = listOf("amount", "from", "to")
    )

    private val baseRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.78,
        "JPY" to 155.0,
        "INR" to 83.5
    )

    override fun execute(args: Map<String, Any?>): Map<String, Any?> {
        val amount = when (val rawAmount = args["amount"]) {
            is Number -> rawAmount.toDouble()
            is String -> rawAmount.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val from = args["from"]?.toString()?.uppercase(Locale.ROOT) ?: "USD"
        val to = args["to"]?.toString()?.uppercase(Locale.ROOT) ?: "EUR"

        val rateFrom = baseRates[from] ?: 1.0
        val rateTo = baseRates[to] ?: 1.0

        // Convert to USD base first, then to target
        val amountInUsd = amount / rateFrom
        val converted = amountInUsd * rateTo
        val finalAmount = (converted * 100.0).roundToInt() / 100.0
        val directRate = (rateTo / rateFrom * 10000.0).roundToInt() / 10000.0

        return mapOf(
            "amount" to amount,
            "from" to from,
            "to" to to,
            "exchangeRate" to directRate,
            "convertedAmount" to finalAmount,
            "formattedResult" to "$amount $from = $finalAmount $to (Rate: $directRate)"
        )
    }
}

object LoanTool : LabTool {
    override val name = "calculateLoanOrInterest"
    override val displayName = "Finance & Interest Solver"
    override val description = "Calculate monthly payments and total interest for a fixed-rate loan or mortgage."
    override val icon = Icons.Default.Calculate

    override val schema = Schema(
        type = "OBJECT",
        properties = mapOf(
            "principal" to Schema(
                type = "NUMBER",
                description = "The total principal loan amount (e.g., 250000.0)"
            ),
            "rate" to Schema(
                type = "NUMBER",
                description = "The annual interest rate in percentage, e.g., 5.5 for 5.5%"
            ),
            "years" to Schema(
                type = "NUMBER",
                description = "The duration of the loan in years, e.g. 15 or 30"
            )
        ),
        required = listOf("principal", "rate", "years")
    )

    override fun execute(args: Map<String, Any?>): Map<String, Any?> {
        val principal = when (val raw = args["principal"]) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val rate = when (val raw = args["rate"]) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val years = when (val raw = args["years"]) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        if (principal <= 0 || years <= 0) {
            return mapOf("error" to "Principal and years must be greater than zero.")
        }

        val monthlyRate = (rate / 100.0) / 12.0
        val totalMonths = years * 12.0

        val monthlyPayment = if (monthlyRate == 0.0) {
            principal / totalMonths
        } else {
            (principal * monthlyRate * (1 + monthlyRate).pow(totalMonths)) /
                    ((1 + monthlyRate).pow(totalMonths) - 1)
        }

        val totalPayment = monthlyPayment * totalMonths
        val totalInterest = totalPayment - principal

        val formattedMonthly = String.format(Locale.US, "$%.2f", monthlyPayment)
        val formattedTotal = String.format(Locale.US, "$%.2f", totalPayment)
        val formattedInterest = String.format(Locale.US, "$%.2f", totalInterest)

        return mapOf(
            "principal" to String.format(Locale.US, "$%.2f", principal),
            "interestRate" to "${rate}%",
            "termYears" to "${years.toInt()} years",
            "monthlyPayment" to formattedMonthly,
            "totalPayment" to formattedTotal,
            "totalInterest" to formattedInterest
        )
    }
}

object BmiTool : LabTool {
    override val name = "calculateBmi"
    override val displayName = "BMI Health Advisor"
    override val description = "Calculate Body Mass Index (BMI) and retrieve customized health and weight tips."
    override val icon = Icons.Default.MonitorWeight

    override val schema = Schema(
        type = "OBJECT",
        properties = mapOf(
            "weightKg" to Schema(
                type = "NUMBER",
                description = "Weight in kilograms (e.g. 70.0)"
            ),
            "heightCm" to Schema(
                type = "NUMBER",
                description = "Height in centimeters (e.g. 175.0)"
            )
        ),
        required = listOf("weightKg", "heightCm")
    )

    override fun execute(args: Map<String, Any?>): Map<String, Any?> {
        val weight = when (val raw = args["weightKg"]) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val height = when (val raw = args["heightCm"]) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        if (weight <= 0 || height <= 0) {
            return mapOf("error" to "Weight and height must be greater than zero.")
        }

        val heightM = height / 100.0
        val bmi = weight / heightM.pow(2)
        val roundedBmi = (bmi * 10.0).roundToInt() / 10.0

        val (category, advice) = when {
            bmi < 18.5 -> Pair(
                "Underweight",
                "Consider consulting with a dietitian. Incorporate nutrient-dense foods and strength training."
            )
            bmi < 25.0 -> Pair(
                "Normal weight",
                "Fantastic! Keep maintaining a balanced diet and doing at least 150 minutes of moderate exercise weekly."
            )
            bmi < 30.0 -> Pair(
                "Overweight",
                "Aim for a modest calorie deficit. Increase daily movement (like brisk walking) and manage sleep."
            )
            else -> Pair(
                "Obese",
                "Consider discussing weight management goals with a healthcare professional to establish a safe plan."
            )
        }

        return mapOf(
            "weight" to "${weight} kg",
            "height" to "${height} cm",
            "bmi" to roundedBmi,
            "category" to category,
            "advice" to advice
        )
    }
}

object ColorPaletteTool : LabTool {
    override val name = "generateRandomColorPalette"
    override val displayName = "Color Palette Designer"
    override val description = "Generate a stylish, themed 5-color hex palette from a description or style keyword."
    override val icon = Icons.Default.ColorLens

    override val schema = Schema(
        type = "OBJECT",
        properties = mapOf(
            "theme" to Schema(
                type = "STRING",
                description = "The style description of the palette, e.g. 'retro synthwave', 'nordic forest', 'ocean breeze'"
            )
        ),
        required = listOf("theme")
    )

    override fun execute(args: Map<String, Any?>): Map<String, Any?> {
        val theme = args["theme"]?.toString()?.lowercase(Locale.ROOT) ?: "cool"

        val palette = when {
            theme.contains("synthwave") || theme.contains("cyberpunk") || theme.contains("neon") || theme.contains("retro") -> listOf(
                "#FF007F" to "Neon Pink",
                "#00F0FF" to "Cyber Cyan",
                "#7B2CBF" to "Electric Indigo",
                "#240046" to "Void Black",
                "#E0AA3E" to "Hologram Gold"
            )
            theme.contains("forest") || theme.contains("nature") || theme.contains("green") || theme.contains("tree") -> listOf(
                "#2D6A4F" to "Deep Pine",
                "#52B788" to "Sage Green",
                "#D8F3DC" to "Mint Cream",
                "#1B4332" to "Moss Shadows",
                "#D08C60" to "Acorn Wood"
            )
            theme.contains("ocean") || theme.contains("breeze") || theme.contains("sea") || theme.contains("blue") || theme.contains("water") -> listOf(
                "#0077B6" to "Deep Ocean",
                "#90E0EF" to "Sky Mist",
                "#03045E" to "Abyssal Blue",
                "#CAF0F8" to "Seafoam White",
                "#F1A7A1" to "Coral Sunset"
            )
            theme.contains("sunset") || theme.contains("warm") || theme.contains("fire") || theme.contains("red") || theme.contains("orange") -> listOf(
                "#FF6B6B" to "Sunset Salmon",
                "#FF8E53" to "Warm Tangerine",
                "#FFD2FC" to "Twilight Rose",
                "#4E1A3D" to "Plum Dusk",
                "#FFE66D" to "Warm Sunlight"
            )
            else -> {
                val hash = theme.hashCode()
                val hue = Math.abs(hash % 360)
                listOf(
                    "#1E293B" to "Slate Dark",
                    "#38BDF8" to "Sky Breeze",
                    "#0EA5E9" to "Ocean Deep",
                    "#F1F5F9" to "Soft Cloud",
                    "#64748B" to "Slate Grey"
                )
            }
        }

        val colorsString = palette.joinToString(", ") { "${it.first} (${it.second})" }
        val colorHexes = palette.joinToString(",") { it.first }

        return mapOf(
            "theme" to theme,
            "paletteColors" to colorsString,
            "paletteHexList" to colorHexes,
            "count" to palette.size
        )
    }
}

object LabToolRegistry {
    val tools = listOf(
        WeatherTool,
        CurrencyTool,
        LoanTool,
        BmiTool,
        ColorPaletteTool
    )

    fun getByName(name: String): LabTool? {
        return tools.find { it.name == name }
    }

    fun getGeminiToolDeclarations(): List<Tool> {
        val declarations = tools.map { tool ->
            FunctionDeclaration(
                name = tool.name,
                description = tool.description,
                parameters = tool.schema
            )
        }
        return listOf(Tool(functionDeclarations = declarations))
    }
}
