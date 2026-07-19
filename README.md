# Gemini Function Selection Lab 🧪

An interactive Android sandbox built with Kotlin and Jetpack Compose demonstrating real-time **dynamic function calling and tool selection** using the **Gemini API (`gemini-3.5-flash`)**.

This project provides a developer playground to understand, visualize, and test the multi-turn exchange cycle required for LLMs to dynamically select and invoke local application tools.

---

## 🌟 Key Features

1. **Interactive Sandbox Console**: Enter natural language requests. Gemini automatically decides whether to answer directly or delegate the task to one of five local compiled tools.
2. **Visual Workflow Timeline**: A diagnostic tracker mapping the six critical stages of function calling:
   - 🛰️ **User Request Sent**: Input captured.
   - 🧠 **LLM Tool Selection**: Gemini parses functions and selects the correct tool.
   - ⚙️ **Tool Selected & Arguments Extracted**: Highlights arguments extracted by the model.
   - 🖥️ **Local Execution**: Dynamic Kotlin function runs on the device.
   - 🔄 **Synthesis Loop**: Transmitting local outputs back to the Gemini model.
   - 💬 **Final Contextualized Response**: Renders the natural language answer.
3. **Bespoke Visual Widgets**: Interactive UI states for each tool (animated Weather cards, currency exchange arrows, dynamic BMI sliders, live Color palettes with Copy Hex shortcuts, and loan calculators).
4. **Local Tools Playground**: Catalog of the compiled functions (`WeatherTool`, `CurrencyTool`, `LoanTool`, `BmiTool`, `ColorPaletteTool`). Developers can test code behavior directly, independent of the AI.
5. **Developer API Log Stream**: Real-time logging of the raw JSON requests and responses sent over the wire, simplifying onboarding for new engineers.

---

## 🛠️ Codebase Architecture

The project is organized following Clean Architecture and MVVM patterns to ensure maintainability:

```text
app/src/main/java/com/example/
├── MainActivity.kt               # Application entry point
├── data/
│   ├── model/
│   │   └── GeminiModels.kt       # Request/Response data structures for Moshi
│   └── network/
│       └── RetrofitClient.kt     # Retrofit client with 60s timeouts and Moshi
├── domain/
│   └── tool/
│       └── LabTools.kt           # Local Kotlin tool registry, schemas, and logic
└── ui/
    ├── lab/
    │   ├── LabScreen.kt          # Compose UI, workflow timeline, and logs
    │   └── LabViewModel.kt       # State engine orchestrating the 2-turn cycle
    └── theme/
        ├── Color.kt              # Slate dark visual color accents
        ├── Theme.kt              # App-wide Material 3 configuration
        └── Type.kt               # Modern display typography
```

---

## 📡 API Integration & The Function Calling Cycle

Gemini Function Calling works in a standard **two-turn, non-blocking asynchronous cycle**:

### Turn 1: Tool Declaration & Choice
The client sends the user prompt along with the metadata schemas of all registered tools.
- **Endpoint**: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${API_KEY}`
- **Payload Schema**:
```json
{
  "contents": [
    { "role": "user", "parts": [{ "text": "What is the weather in Tokyo?" }] }
  ],
  "tools": [
    {
      "functionDeclarations": [
        {
          "name": "getWeather",
          "description": "Get the current weather and activity recommendation for a given location.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "location": { "type": "STRING", "description": "The city and state or country" }
            },
            "required": ["location"]
          }
        }
      ]
    }
  ]
}
```

### Turn 1 Response: LLM Invocation Request
If Gemini decides a tool is required, it returns a response containing a `functionCall` object instead of natural text.
- **Response Schema**:
```json
{
  "candidates": [
    {
      "content": {
        "role": "model",
        "parts": [
          {
            "functionCall": {
              "name": "getWeather",
              "args": { "location": "Tokyo" }
            }
          }
        ]
      }
    }
  ]
}
```

### Turn 2: Client Execution & Response Synthesis
The app intercepts the `functionCall`, executes `WeatherTool.execute(location = "Tokyo")` locally, and sends a follow-up request. This second request **must** contain the entire conversation history, including the model's call and the client's function response.
- **Follow-up Payload Schema**:
```json
{
  "contents": [
    { "role": "user", "parts": [{ "text": "What is the weather in Tokyo?" }] },
    {
      "role": "model",
      "parts": [{ "functionCall": { "name": "getWeather", "args": { "location": "Tokyo" } } }]
    },
    {
      "role": "user",
      "parts": [
        {
          "functionResponse": {
            "name": "getWeather",
            "response": {
              "location": "Tokyo",
              "temperature": "22°C / 71.6°F",
              "condition": "Partly Cloudy with gentle breeze",
              "humidity": "54%",
              "recommendation": "Perfect weather for sightseeing!"
            }
          }
        }
      ]
    }
  ]
}
```
Gemini processes this history and synthesizes a user-friendly conversational answer.

---

## 🚀 Setup & Local Onboarding

### Prerequisites
- Android Studio Ladybug (or higher)
- JDK 11+
- Internet permission on the device

### API Configuration (AI Studio Secret Management)
The project utilizes the **Secrets Gradle Plugin** to load credentials safely from a `.env` file into a `BuildConfig` parameter without exposure in source code.

1. Create a `.env` file at the root of the project:
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
2. Build files read this on compile:
   ```kotlin
   // app/build.gradle.kts
   secrets {
       propertiesFileName = ".env"
       defaultPropertiesFileName = ".env.example"
   }
   ```
3. Read the key securely in Kotlin code:
   ```kotlin
   val apiKey = com.example.BuildConfig.GEMINI_API_KEY
   ```

---

## 🧪 Registering a New Tool (Developer Guide)

To add a new tool to the lab sandbox:

1. **Implement `LabTool`** in `com.example.domain.tool.LabTools.kt`:
   ```kotlin
   object MyNewTool : LabTool {
       override val name = "calculateTax"
       override val displayName = "VAT Calculator"
       override val description = "Calculate standard tax or value added tax on a subtotal."
       override val icon = Icons.Default.Calculate
       override val schema = Schema(
           type = "OBJECT",
           properties = mapOf(
               "amount" to Schema(type = "NUMBER", description = "The transaction subtotal"),
               "rate" to Schema(type = "NUMBER", description = "The VAT tax rate percentage")
           ),
           required = listOf("amount", "rate")
       )

       override fun execute(args: Map<String, Any?>): Map<String, Any?> {
           val amount = args["amount"].toString().toDoubleOrNull() ?: 0.0
           val rate = args["rate"].toString().toDoubleOrNull() ?: 0.0
           val tax = amount * (rate / 100)
           return mapOf("subtotal" to amount, "tax" to tax, "total" to (amount + tax))
       }
   }
   ```
2. **Register it** in `LabToolRegistry`:
   ```kotlin
   val tools = listOf(
       WeatherTool,
       CurrencyTool,
       LoanTool,
       BmiTool,
       ColorPaletteTool,
       MyNewTool // Added here!
   )
   ```
3. **The app will automatically sync**:
   - The tool will immediately register with the Gemini API.
   - It will appear in the "Local Tool Playground" for developer testing.
   - The UI will dynamically render standard raw execution metrics when selected.

---

## 📱 Developer Sandbox Verification

Run the app in the emulator or on-device to test key user journeys:
- **Case A (Indirect Response)**: Type *"What is the weather in Paris?"*. Watch the pipeline step-through to Tool Selected, execute local weather, loop back to the API, and display the Paris weather card.
- **Case B (Direct Response)**: Type *"Who was the first president of the USA?"*. Observe that the pipeline bypasses local execution and answers directly.
- **Case C (Manual Sandbox)**: Open the "Tools Playground" tab. Select the **Color Palette Designer**, type "Forest" as the theme, click **Run Local Kotlin Function**, and verify that the local logic returns the moss/pine color values instantly.
