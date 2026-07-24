# Compass: Developer Guide & Architecture

## 🚀 Getting Started: Build & Run in Android Studio

**Prerequisites**
- **Android Studio** (Koala / Ladybug or newer recommended).
- **JDK 17** (configured in Android Studio).
- **Android SDK Platform 34** (target SDK).
- **Test Device:** A physical Android device or Emulator with at least 4GB of RAM allocated (local LLM inference requires substantial memory).

**1. Clone and Open the Project**
- Clone the repository to your local machine.
- Open Android Studio and select **Open** -> Navigate to the cloned project folder.
- Wait for the initial **Gradle Sync** to complete. This will pull down necessary dependencies including Jetpack Compose, Kotlin Coroutines, Room Database, and the `com.google.ai.edge.litertlm` engine.

**2. Build and Run**
- Connect a physical Android device via USB debugging or launch a high-RAM Android Virtual Device (AVD).
- Click the **Run 'app'** (Play button) in the Android Studio toolbar, or run `./gradlew assembleDebug` from the terminal.

**3. How the App Works (The User Journey)**
1. **Model Download/Sideload:** Upon first launch, the user navigates to the **Model Manager**. Here, they can download an ungated Gemma 4 model directly to their device storage. Alternatively, models can be sideloaded into the `Downloads` folder, which the app automatically scans.
2. **Local Engine Initialization:** Once a model is selected, the `LocalGemmaClient` loads the `.tflite` / `.litertlm` binary into the device's RAM using the CPU backend.
3. **Offline Generation:** In the **Chat Screen**, the user enters a prompt. The query runs entirely locally against the loaded model, returning responses with zero network calls.
4. **Local Document Grounding (RAG):** The user can go to the **Document Reader** to load local `.md` or `.txt` files. These files act as context windows, anchoring the model's responses in the provided local data.

## System Architecture
Compass is a native Android application built with modern, declarative principles. It is designed to be highly efficient, minimizing battery drain while maximizing on-device inference speed.

- **UI Layer (Jetpack Compose):** A highly responsive, Material Design 3 interface featuring a Real-time Chat UI (`ChatScreen.kt`), a Model Manager (`ModelManagerScreen.kt`), and a Document Reader (`DocumentReaderScreen.kt`). 
- **State Management:** Kotlin Coroutines and `StateFlow` bridge the UI and background workers, ensuring fluid UI updates even during heavy LLM inference (`ChatViewModel.kt`, `HomeViewModel.kt`).
- **Inference Engine:** The core local inference is handled by `Google AI Edge LiteRT LM` (`LocalGemmaClient.kt`). This runs entirely on the device CPU, using advanced RAM caching for fast model-switching.
- **Network & Storage Layer:** A robust, redirect-aware `ModelDownloader.kt` securely fetches quantized model files (e.g., `.tflite`, `.task`) directly to the Android file system.

## Accessing Ungated Gemma 4 Variants

To make downloading the massive Gemma 4 models frictionless for the user without requiring them to set up HuggingFace API tokens, the application implements a specialized download pipeline:

1.  **Community-Hosted Ungated Repositories:** Instead of pointing to the strictly gated official Google repositories which require authentication, the `ModelVariant.kt` file defines hardcoded URLs pointing to trusted community mirrors (e.g., `micmacfree/gemma-4-e2b-it-litertlm` and `DarrenJiaImbue/gemma-4-E4B-it-qat-litertlm`). These community members have already accepted the Gemma terms and re-hosted the `.litertlm` conversions publicly.
2.  **Robust Redirect & CDN Handling:** HuggingFace repository URLs (like `/resolve/main/...`) do not host the file directly; they issue HTTP 302 redirects to a Content Delivery Network (like Cloudflare or AWS S3). The `ModelDownloader.kt` manually intercepts these HTTP redirects in a `while` loop to follow them until it reaches the actual binary payload.
3.  **Spoofing User-Agent:** Basic HTTP requests from Android are often blocked with a `403 Forbidden` by HuggingFace's anti-scraping firewalls. To bypass this, the downloader spoofs a standard desktop Chrome `User-Agent`, making the request appear as a standard web browser download.
4.  **Token Leak Prevention:** Although we use ungated variants by default, the downloader supports an optional `authToken` for users who want to download private models. Crucially, the code inspects the domain before attaching the `Authorization: Bearer` header, ensuring it is *only* sent to `huggingface.co` and never leaked to third-party CDNs during redirects.

## How Gemma 4 Was Used

**1. Building the Application:**
Gemma 4 was instrumental in the actual development of Compass. As an AI coding assistant, Gemma 4 was used to architect the Jetpack Compose UI, implement the complex Coroutine-based download manager (handling redirects and chunked file writing), and structure the LiteRT engine bindings to ensure zero-latency local execution.

**2. Integrating Gemma 4 as the Edge Model:**
The brain of the Compass app is a highly quantized version of **Gemma 4** (specifically optimized for mobile edge deployment). 
- **Integration:** Gemma 4 was converted to the `.tflite` / `.litertlm` format and integrated using the `com.google.ai.edge.litertlm` library.
- **Execution:** When a user asks a question, the `LocalGemmaClient` initializes the Gemma 4 engine using the `Backend.CPU()` configuration. The prompt is passed directly to the `Conversation` object, and Gemma 4 generates the response locally. 
- **Optimization:** We configured the engine with custom sampler settings (`topK = 64`, `topP = 0.95`, `temperature = 1.0`) to balance factual accuracy (crucial for health topics) with conversational fluidity.
