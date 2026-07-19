package com.example.ui.lab

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.theme.PurpleGrey80
import com.example.domain.tool.LabTool
import com.example.domain.tool.LabToolRegistry
import com.example.domain.tool.WeatherTool
import com.example.domain.tool.CurrencyTool
import com.example.domain.tool.LoanTool
import com.example.domain.tool.BmiTool
import com.example.domain.tool.ColorPaletteTool

// Color Definitions for Custom Light 'Sleek Interface' Theme
val SlateDark = Color(0xFFFEF7FF) // Ultra light lavender/cream body background
val CardSlate = Color(0xFFFFFFFF) // Crisp white card background
val TealAccent = Color(0xFF6750A4) // Dynamic primary brand purple
val BlueAccent = Color(0xFFD0BCFF) // User message secondary lavender
val GoldAccent = Color(0xFFF3EDF7) // Soft header / tool lavender container
val CoralAccent = Color(0xFFB3261E) // Material 3 standard error red
val LightText = Color(0xFF1D1B20) // Primary high-contrast charcoal text
val GreyText = Color(0xFF49454F) // Supporting/secondary slate grey text
val BorderColor = Color(0xFFCAC4D0) // M3 Outline / divider color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabScreen(viewModel: LabViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var inputPrompt by remember { mutableStateOf("") }

    // Preset prompts
    val suggestionChips = listOf(
        "Weather in Tokyo" to "What's the weather like in Tokyo right now?",
        "Convert 150 USD to EUR" to "Can you convert 150 USD to EUR?",
        "30y Loan at 5.5%" to "Calculate payments for a 350,000 principal loan at 5.5% rate for 30 years.",
        "BMI of 80kg / 180cm" to "What is my BMI if I weigh 80 kg and am 180 cm tall?",
        "Cyberpunk Neon Palette" to "Generate a retro cyberpunk neon color palette."
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Science Icon",
                            tint = TealAccent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "Gemini Function Lab",
                                color = LightText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Dynamic Tool Selection Sandbox",
                                color = GreyText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDark
                ),
                actions = {
                    Badge(
                        containerColor = Color(0xFFE8DEF8),
                        contentColor = Color(0xFF21005D),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LLM: GEMINI-3.5-FLASH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                        }
                    }
                }
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateDark,
                contentColor = TealAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealAccent
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sandbox Console", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Console") },
                    selectedContentColor = TealAccent,
                    unselectedContentColor = GreyText
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Tools Playground", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Playground") },
                    selectedContentColor = TealAccent,
                    unselectedContentColor = GreyText
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("API Logs", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Logs") },
                    selectedContentColor = TealAccent,
                    unselectedContentColor = GreyText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Sandbox Console
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Quick suggestion chips
                        Text(
                            "Interactive Presets",
                            color = GreyText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(suggestionChips) { (label, fullText) ->
                                AssistChip(
                                    onClick = {
                                        inputPrompt = fullText
                                        viewModel.sendRequest(fullText)
                                    },
                                    label = { Text(label, color = LightText, fontSize = 12.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CardSlate,
                                        labelColor = LightText
                                    ),
                                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.4f)),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Run",
                                            tint = TealAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }

                        // Input Box
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sandbox_prompt_input"),
                            placeholder = { Text("Ask Gemini to convert, check weather, calculate mortgage or palette...", color = GreyText) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputPrompt.isNotBlank()) {
                                    viewModel.sendRequest(inputPrompt)
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText,
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = CardSlate,
                                focusedContainerColor = CardSlate,
                                unfocusedContainerColor = CardSlate
                            ),
                            trailingIcon = {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        color = TealAccent,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (inputPrompt.isNotBlank()) {
                                                viewModel.sendRequest(inputPrompt)
                                            }
                                        },
                                        enabled = inputPrompt.isNotBlank()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Send",
                                            tint = if (inputPrompt.isNotBlank()) TealAccent else GreyText
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timeline steps
                        if (state.timelineSteps.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Brain",
                                        tint = GreyText.copy(alpha = 0.3f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Sandbox Idle",
                                        color = LightText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Type a dynamic request or click one of the interactive presets above to see the LLM select and call registered tools in real-time.",
                                        color = GreyText,
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("sandbox_timeline_list"),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.timelineSteps) { step ->
                                    TimelineStepCard(step)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Tools Playground
                    ToolsPlaygroundView(state, viewModel)
                }
                2 -> {
                    // Raw API logs
                    ApiLogsView(state)
                }
            }
        }
    }
}

@Composable
fun TimelineStepCard(step: TimelineStep) {
    val colorAccent = when (step.type) {
        StepType.REQUEST -> Color(0xFF0EA5E9)      // Vibrant blue for request
        StepType.ANALYZING -> Color(0xFF6750A4)     // Purple for thinking
        StepType.TOOL_SELECTED -> Color(0xFFD97706) // Deep orange for tool call
        StepType.DIRECT_ANSWER -> Color(0xFF059669) // Forest green for direct answer
        StepType.TOOL_EXECUTED -> Color(0xFF7C3AED) // Rich violet for execution success
        StepType.MODEL_SYNTHESIS -> Color(0xFF6750A4)// Purple for synthesizing
        StepType.FINAL_RESPONSE -> Color(0xFF16A34A) // Bright emerald green for final answer
        StepType.ERROR -> Color(0xFFB3261E)          // Alert red for error
    }

    val stepIcon = when (step.type) {
        StepType.REQUEST -> Icons.Default.PlayArrow
        StepType.ANALYZING -> Icons.Default.Psychology
        StepType.TOOL_SELECTED -> Icons.Default.Settings
        StepType.DIRECT_ANSWER -> Icons.Default.CheckCircle
        StepType.TOOL_EXECUTED -> Icons.Default.Code
        StepType.MODEL_SYNTHESIS -> Icons.Default.Science
        StepType.FINAL_RESPONSE -> Icons.Default.CheckCircle
        StepType.ERROR -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp, 
                if (step.isPending) colorAccent else BorderColor, 
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(colorAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.isPending) {
                        CircularProgressIndicator(
                            color = colorAccent,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = stepIcon,
                            contentDescription = null,
                            tint = colorAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        color = LightText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = step.description,
                        color = GreyText,
                        fontSize = 12.sp
                    )
                }
            }

            // Step Content
            step.content?.let { content ->
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom Widgets based on tool output
                if (step.type == StepType.TOOL_EXECUTED && !content.startsWith("Error")) {
                    val parsedMap = parseContentToMap(content)
                    when {
                        parsedMap.containsKey("temperature") -> WeatherWidget(parsedMap)
                        parsedMap.containsKey("exchangeRate") -> CurrencyWidget(parsedMap)
                        parsedMap.containsKey("monthlyPayment") -> LoanWidget(parsedMap)
                        parsedMap.containsKey("bmi") -> BmiWidget(parsedMap)
                        parsedMap.containsKey("paletteHexList") -> ColorPaletteWidget(parsedMap)
                        else -> RawResultWidget(content)
                    }
                } else if (step.type == StepType.FINAL_RESPONSE) {
                    // Beautiful markdown-ish final text box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GoldAccent, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = content,
                            color = LightText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    // Standard raw code/metadata box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = GreyText
                        )
                    }
                }
            }
        }
    }
}

// Custom Widgets for Tool Executions
@Composable
fun WeatherWidget(data: Map<String, String>) {
    val location = data["location"] ?: "Unknown"
    val temp = data["temperature"] ?: "--"
    val humidity = data["humidity"] ?: "--"
    val condition = data["condition"] ?: "--"
    val rec = data["recommendation"] ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(location, color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(condition, color = TealAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Weather",
                    tint = TealAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("TEMPERATURE", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(temp, color = LightText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("HUMIDITY", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(humidity, color = LightText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(rec, color = GreyText, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
fun CurrencyWidget(data: Map<String, String>) {
    val amount = data["amount"] ?: "0.0"
    val from = data["from"] ?: ""
    val to = data["to"] ?: ""
    val rate = data["exchangeRate"] ?: "1.0"
    val converted = data["convertedAmount"] ?: "0.0"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CURRENCY EXCHANGE RESULT", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("$amount $from", color = LightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.padding(horizontal = 16.dp).size(20.dp)
                )
                Text("$converted $to", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Exchange Rate: 1 $from = $rate $to",
                color = GreyText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LoanWidget(data: Map<String, String>) {
    val principal = data["principal"] ?: "$0"
    val rate = data["interestRate"] ?: "0%"
    val years = data["termYears"] ?: ""
    val monthly = data["monthlyPayment"] ?: "$0"
    val total = data["totalPayment"] ?: "$0"
    val interest = data["totalInterest"] ?: "$0"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("FINANCE SOLVER SUMMARY", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ESTIMATED MONTHLY PAYMENT", color = GreyText, fontSize = 10.sp)
                Text(monthly, color = TealAccent, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("LOAN PRINCIPAL", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(principal, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ANNUAL RATE", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(rate, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DURATION", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(years, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TOTAL INTEREST", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(interest, color = CoralAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL REPAYMENT", color = GreyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(total, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BmiWidget(data: Map<String, String>) {
    val weight = data["weight"] ?: ""
    val height = data["height"] ?: ""
    val bmi = data["bmi"]?.toDoubleOrNull() ?: 0.0
    val category = data["category"] ?: "Unknown"
    val advice = data["advice"] ?: ""

    val meterColor = when {
        bmi < 18.5 -> BlueAccent
        bmi < 25.0 -> TealAccent
        bmi < 30.0 -> GoldAccent
        else -> CoralAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("BMI HEALTH METRICS", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BODY MASS INDEX", color = GreyText, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(bmi.toString(), color = meterColor, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                        Text(" kg/m²", color = GreyText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = meterColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = category.uppercase(),
                        color = meterColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Custom slider progress indicator to show BMI position
            val progress = ((bmi - 15.0) / 20.0).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(CardSlate)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(BlueAccent, TealAccent, GoldAccent, CoralAccent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(advice, color = GreyText, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun ColorPaletteWidget(data: Map<String, String>) {
    val theme = data["theme"] ?: ""
    val hexString = data["paletteHexList"] ?: ""
    val hexes = hexString.split(",").map { it.trim() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("THEMED COLOR PALETTE: ${theme.uppercase()}", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ColorLens, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Palette boxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                hexes.forEach { hex ->
                    val colorValue = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorValue)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(hex))
                                Toast.makeText(context, "Copied Hex $hex", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Tip: Click any color above to copy its hex value.",
                color = GreyText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RawResultWidget(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GoldAccent, RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text("LOCAL FUNCTION RESPONSE MAP:", color = GreyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = LightText
            )
        }
    }
}

// Helpers
private fun parseContentToMap(content: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    content.lines().forEach { line ->
        if (line.contains(":")) {
            val idx = line.indexOf(":")
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            map[key] = value
        }
    }
    return map
}

// Playground Tab View
@Composable
fun ToolsPlaygroundView(state: LabUiState, viewModel: LabViewModel) {
    var selectedTool by remember { mutableStateOf<LabTool>(WeatherTool) }

    LaunchedEffect(Unit) {
        viewModel.selectPlaygroundTool(WeatherTool)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Local Function Catalog",
                color = LightText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "These are the exact Kotlin objects compiled in the domain layer. Select any tool to test-run its code locally.",
                color = GreyText,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Tools List
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                LabToolRegistry.tools.forEach { tool ->
                    val isSelected = selectedTool.name == tool.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playground_tool_row_${tool.name}")
                            .clickable {
                                selectedTool = tool
                                viewModel.selectPlaygroundTool(tool)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CardSlate else CardSlate.copy(alpha = 0.5f)
                        ),
                        border = if (isSelected) BorderStroke(1.dp, TealAccent) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) TealAccent.copy(alpha = 0.2f) else CardSlate,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) TealAccent else GreyText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tool.displayName,
                                    color = LightText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "name: ${tool.name}()",
                                    color = GreyText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Badge(containerColor = TealAccent, contentColor = SlateDark) {
                                    Text("SELECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Tool Playground Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tool_playground_form"),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Playground: ${selectedTool.displayName}",
                        color = TealAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = selectedTool.description,
                        color = GreyText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(bottom = 16.dp))

                    // Dynamic argument form
                    selectedTool.schema.properties?.forEach { (propName, propSchema) ->
                        val currentVal = state.playgroundArgs[propName] ?: ""
                        Text(
                            text = "$propName (${propSchema.type})",
                            color = LightText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = currentVal,
                            onValueChange = { viewModel.updatePlaygroundArg(propName, it) },
                            placeholder = { Text(propSchema.description ?: "", color = GreyText, fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playground_arg_input_$propName")
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText,
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SlateDark,
                                unfocusedContainerColor = SlateDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.executePlaygroundTool() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playground_execute_button")
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = SlateDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN LOCAL KOTLIN FUNCTION", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Execution output
                    state.playgroundResult?.let { result ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Kotlin Execution Output:",
                            color = LightText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(SlateDark)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = result,
                                fontFamily = FontFamily.Monospace,
                                color = TealAccent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Logs View
@Composable
fun ApiLogsView(state: LabUiState) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Developer API Log Stream",
                color = LightText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Inspect the actual raw JSON payloads exchanged with the Gemini REST API during the multi-turn session.",
                color = GreyText,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (state.rawJsonLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = GreyText.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Payloads Logged Yet", color = LightText, fontWeight = FontWeight.SemiBold)
                        Text("Run a query on the Sandbox tab to stream logs.", color = GreyText, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(state.rawJsonLogs) { (title, json) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                color = TealAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "JSON Payload copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy JSON",
                                    tint = GreyText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = json,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = LightText,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
