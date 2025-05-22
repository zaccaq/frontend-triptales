// File: app/src/main/java/com/example/frontend_triptales/ui/theme/mlkit/MLKitService.kt
package com.example.frontend_triptales.ui.theme.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Modello di dati per i risultati di ML Kit
 */
data class MLKitResult(
    val detectedObjects: List<DetectedObjectInfo> = emptyList(),
    val extractedText: String = "",
    val translatedText: String = "",
    val generatedCaption: String = ""
)

/**
 * Informazioni su un oggetto rilevato
 */
data class DetectedObjectInfo(
    val label: String,
    val confidence: Float,
    val boundingBox: Rect
)

/**
 * Servizio per l'integrazione con Google ML Kit
 * Fornisce funzionalità di OCR, riconoscimento oggetti, traduzione e generazione caption
 */
object MLKitService {
    private const val TAG = "MLKitService"

    // Text Recognition
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Object Detection
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val objectDetector = ObjectDetection.getClient(objectDetectorOptions)

    /**
     * Processa un'immagine con tutte le funzionalità ML Kit disponibili
     *
     * @param image InputImage da processare
     * @param targetLanguage Lingua target per la traduzione (default: inglese)
     * @return MLKitResult con tutti i risultati dell'analisi
     */
    suspend fun processImage(
        image: InputImage,
        targetLanguage: String = TranslateLanguage.ENGLISH
    ): MLKitResult {
        return try {
            Log.d(TAG, "Inizio processamento immagine con ML Kit")

            // Esegui tutte le analisi in parallelo per migliorare le performance
            val detectedObjects = detectObjects(image)
            val extractedText = extractText(image)

            // Traduci il testo solo se è stato estratto qualcosa
            val translatedText = if (extractedText.isNotEmpty()) {
                translateText(extractedText, targetLanguage)
            } else ""

            // Genera una caption intelligente basata sui risultati
            val caption = generateCaption(detectedObjects, extractedText)

            val result = MLKitResult(
                detectedObjects = detectedObjects,
                extractedText = extractedText,
                translatedText = translatedText,
                generatedCaption = caption
            )

            Log.d(TAG, "Processamento completato: ${detectedObjects.size} oggetti, " +
                    "${extractedText.length} caratteri di testo, caption: ${caption.isNotEmpty()}")

            result
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel processamento immagine", e)
            MLKitResult()
        }
    }

    /**
     * Rileva oggetti nell'immagine utilizzando ML Kit Object Detection
     *
     * @param image InputImage da analizzare
     * @return Lista di oggetti rilevati con le loro informazioni
     */
    private suspend fun detectObjects(image: InputImage): List<DetectedObjectInfo> {
        return try {
            Log.d(TAG, "Inizio riconoscimento oggetti")

            val results = objectDetector.process(image).await()
            val detectedObjects = results.map { detectedObject ->
                val label = when {
                    detectedObject.labels.isNotEmpty() -> detectedObject.labels.first().text
                    else -> "Oggetto sconosciuto"
                }
                val confidence = if (detectedObject.labels.isNotEmpty()) {
                    detectedObject.labels.first().confidence
                } else 0.5f

                DetectedObjectInfo(
                    label = label,
                    confidence = confidence,
                    boundingBox = detectedObject.boundingBox
                )
            }.filter { it.confidence > 0.3f } // Filtra oggetti con bassa confidenza

            Log.d(TAG, "Rilevati ${detectedObjects.size} oggetti")
            detectedObjects
        } catch (e: Exception) {
            Log.e(TAG, "Errore riconoscimento oggetti", e)
            emptyList()
        }
    }

    /**
     * Estrae testo dall'immagine utilizzando ML Kit Text Recognition (OCR)
     *
     * @param image InputImage da cui estrarre il testo
     * @return Testo estratto dall'immagine
     */
    private suspend fun extractText(image: InputImage): String {
        return try {
            Log.d(TAG, "Inizio estrazione testo OCR")

            val result = textRecognizer.process(image).await()
            val extractedText = result.text.trim()

            Log.d(TAG, "Estratti ${extractedText.length} caratteri di testo")
            extractedText
        } catch (e: Exception) {
            Log.e(TAG, "Errore estrazione testo", e)
            ""
        }
    }

    /**
     * Traduce il testo utilizzando ML Kit Translation
     *
     * @param text Testo da tradurre
     * @param targetLanguage Lingua target per la traduzione
     * @return Testo tradotto
     */
    private suspend fun translateText(text: String, targetLanguage: String): String {
        return try {
            if (text.isBlank()) return ""

            Log.d(TAG, "Inizio traduzione testo verso $targetLanguage")

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ITALIAN)
                .setTargetLanguage(targetLanguage)
                .build()

            val translator = Translation.getClient(options)

            // Scarica il modello se necessario
            translator.downloadModelIfNeeded().await()

            // Esegui la traduzione
            val translatedText = translator.translate(text).await()

            Log.d(TAG, "Traduzione completata")
            translatedText
        } catch (e: Exception) {
            Log.e(TAG, "Errore traduzione", e)
            text // Ritorna il testo originale in caso di errore
        }
    }

    /**
     * Genera una caption intelligente basata sui risultati dell'analisi ML Kit
     *
     * @param objects Lista di oggetti rilevati
     * @param text Testo estratto dall'immagine
     * @return Caption generata automaticamente
     */
    private fun generateCaption(
        objects: List<DetectedObjectInfo>,
        text: String
    ): String {
        // Filtra oggetti con alta confidenza
        val highConfidenceObjects = objects.filter { it.confidence > 0.7f }
            .map { it.label.lowercase() }
            .distinct()

        // Filtra oggetti comuni per evitare caption troppo generiche
        val filteredObjects = highConfidenceObjects.filter { label ->
            !label.contains("unknown", ignoreCase = true) &&
                    !label.contains("object", ignoreCase = true) &&
                    label.length > 2
        }

        return when {
            // Caption ricca con oggetti e testo
            filteredObjects.isNotEmpty() && text.isNotEmpty() -> {
                val objectsList = if (filteredObjects.size > 3) {
                    filteredObjects.take(2).joinToString(", ") + " e altro"
                } else {
                    filteredObjects.joinToString(", ")
                }
                "Ho scattato una foto che mostra $objectsList. " +
                        "C'è anche del testo che dice: \"${text.take(50)}${if (text.length > 50) "..." else ""}\""
            }

            // Caption solo con oggetti
            filteredObjects.isNotEmpty() -> {
                val objectsList = when (filteredObjects.size) {
                    1 -> filteredObjects.first()
                    2 -> filteredObjects.joinToString(" e ")
                    else -> filteredObjects.take(2).joinToString(", ") + " e altro"
                }
                "Bella foto di $objectsList durante il nostro viaggio!"
            }

            // Caption solo con testo
            text.isNotEmpty() -> {
                if (text.length > 100) {
                    "Ho fotografato questo testo interessante: \"${text.take(80)}...\""
                } else {
                    "Testo fotografato: \"$text\""
                }
            }

            // Caption generica
            else -> generateRandomTravelCaption()
        }
    }

    /**
     * Genera una caption generica per i viaggi quando non ci sono risultati ML specifici
     */
    private fun generateRandomTravelCaption(): String {
        val captions = listOf(
            "Un momento speciale del nostro viaggio!",
            "Ricordo indimenticabile della gita!",
            "Che bella scoperta durante l'esplorazione!",
            "Un'altra tappa del nostro fantastico viaggio!",
            "Immagine che racconta la nostra avventura!",
            "Momento catturato durante l'esplorazione!",
            "Parte della nostra incredibile esperienza di viaggio!"
        )
        return captions.random()
    }

    /**
     * Crea un InputImage da URI
     *
     * @param context Context dell'applicazione
     * @param uri URI dell'immagine
     * @return InputImage pronto per essere processato, null in caso di errore
     */
    suspend fun createInputImageFromUri(
        context: Context,
        uri: Uri
    ): InputImage? {
        return try {
            Log.d(TAG, "Creazione InputImage da URI: $uri")
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            Log.e(TAG, "Errore creazione InputImage da URI", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Errore generico creazione InputImage", e)
            null
        }
    }

    /**
     * Crea un InputImage da Bitmap
     *
     * @param bitmap Bitmap dell'immagine
     * @param rotationDegrees Gradi di rotazione dell'immagine (default: 0)
     * @return InputImage pronto per essere processato
     */
    fun createInputImageFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): InputImage {
        return InputImage.fromBitmap(bitmap, rotationDegrees)
    }

    /**
     * Rilascia le risorse utilizzate da ML Kit
     * Chiamare questo metodo quando non si ha più bisogno del servizio
     */
    fun cleanup() {
        try {
            textRecognizer.close()
            objectDetector.close()
            Log.d(TAG, "Risorse ML Kit rilasciate")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel rilascio risorse ML Kit", e)
        }
    }
}