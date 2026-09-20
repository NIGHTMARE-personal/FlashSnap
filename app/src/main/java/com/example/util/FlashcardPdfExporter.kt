package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FlashcardDeck
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports FlashSnap decks into printable, double-sided cuttable flashcard sheets.
 * Formatted with archival bistre ink, crop guidelines, and duplex mirror alignment.
 */
object FlashcardPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard pt
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val COLS = 2
    private const val ROWS = 3
    private const val CARDS_PER_PAGE = COLS * ROWS

    fun exportDeckToPdf(context: Context, deck: FlashcardDeck): File? {
        if (deck.cards.isEmpty()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No cards to export in ${deck.title}", Toast.LENGTH_SHORT).show()
            }
            return null
        }

        val pdfDocument = PdfDocument()

        val cardWidth = (PAGE_WIDTH - 2 * MARGIN) / COLS
        val cardHeight = (PAGE_HEIGHT - 2 * MARGIN - 40f) / ROWS

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3C322C") // Bistre ink
            textSize = 11f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3C322C")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C0793D") // Gilt accent
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#756A63")
            textSize = 9f
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4CBB5")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0A794")
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        val bgCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCFAF6")
            style = Paint.Style.FILL
        }

        val totalCards = deck.cards.size
        val pagesCount = (totalCards + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE
        var pdfPageNumber = 1

        for (p in 0 until pagesCount) {
            val startIndex = p * CARDS_PER_PAGE
            val pageCards = deck.cards.subList(startIndex, minOf(startIndex + CARDS_PER_PAGE, totalCards))

            // ─────────────────────────────────────────────────────────────
            // SIDE A: FRONTS (Questions)
            // ─────────────────────────────────────────────────────────────
            val pageInfoFront = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pdfPageNumber++).create()
            val pageFront = pdfDocument.startPage(pageInfoFront)
            val canvasFront = pageFront.canvas

            drawPageBackground(canvasFront)
            drawHeader(
                canvas = canvasFront,
                title = deck.title,
                subject = deck.subject,
                side = "FRONT (Questions) • Page ${p + 1} of $pagesCount",
                headerPaint = headerPaint,
                subPaint = subPaint
            )

            for (i in pageCards.indices) {
                val card = pageCards[i]
                val col = i % COLS
                val row = i / COLS
                val x = MARGIN + col * cardWidth
                val y = MARGIN + 40f + row * cardHeight

                val rect = RectF(x + 4f, y + 4f, x + cardWidth - 4f, y + cardHeight - 4f)
                canvasFront.drawRoundRect(rect, 8f, 8f, bgCardPaint)
                canvasFront.drawRoundRect(rect, 8f, 8f, borderPaint)

                // Cut marks
                drawCutGuide(canvasFront, rect, cutLinePaint)

                // Card Number & Tag
                val cardNum = startIndex + i + 1
                canvasFront.drawText("#$cardNum", rect.left + 12f, rect.top + 20f, boldPaint)
                if (card.tag.isNotBlank()) {
                    canvasFront.drawText("• ${card.tag}", rect.left + 40f, rect.top + 20f, subPaint)
                }

                // Question Text
                drawWrappedText(
                    canvas = canvasFront,
                    text = card.question,
                    x = rect.left + 12f,
                    y = rect.top + 42f,
                    maxWidth = cardWidth - 24f,
                    paint = textPaint,
                    maxLines = 7
                )
            }
            pdfDocument.finishPage(pageFront)

            // ─────────────────────────────────────────────────────────────
            // SIDE B: BACKS (Answers) - Mirrored Horizontally for Duplex Print
            // ─────────────────────────────────────────────────────────────
            val pageInfoBack = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pdfPageNumber++).create()
            val pageBack = pdfDocument.startPage(pageInfoBack)
            val canvasBack = pageBack.canvas

            drawPageBackground(canvasBack)
            drawHeader(
                canvas = canvasBack,
                title = deck.title,
                subject = deck.subject,
                side = "BACK (Answers) • Duplex Aligned • Page ${p + 1} of $pagesCount",
                headerPaint = headerPaint,
                subPaint = subPaint
            )

            for (i in pageCards.indices) {
                val card = pageCards[i]
                val originalCol = i % COLS
                val row = i / COLS
                // Mirror horizontally: col 0 becomes col 1, col 1 becomes col 0
                val mirroredCol = (COLS - 1) - originalCol
                val x = MARGIN + mirroredCol * cardWidth
                val y = MARGIN + 40f + row * cardHeight

                val rect = RectF(x + 4f, y + 4f, x + cardWidth - 4f, y + cardHeight - 4f)
                canvasBack.drawRoundRect(rect, 8f, 8f, bgCardPaint)
                canvasBack.drawRoundRect(rect, 8f, 8f, borderPaint)

                drawCutGuide(canvasBack, rect, cutLinePaint)

                val cardNum = startIndex + i + 1
                canvasBack.drawText("ANSWER #$cardNum", rect.left + 12f, rect.top + 20f, boldPaint)

                // Answer Text
                drawWrappedText(
                    canvas = canvasBack,
                    text = card.answer,
                    x = rect.left + 12f,
                    y = rect.top + 42f,
                    maxWidth = cardWidth - 24f,
                    paint = textPaint,
                    maxLines = 7
                )
            }
            pdfDocument.finishPage(pageBack)
        }

        // Write to Cache File
        val exportDir = File(context.cacheDir, "flashsnap_exports").apply { mkdirs() }
        val cleanName = deck.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
        val outFile = File(exportDir, "FlashSnap_${cleanName}_Cards.pdf")

        try {
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return outFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FlashSnap: $title (Printable Cards)")
                putExtra(Intent.EXTRA_TEXT, "Here is your printable flashcard sheet for '$title' created with FlashSnap.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Print or Share Flashcards")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open share dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawPageBackground(canvas: Canvas) {
        val bg = Paint().apply { color = Color.parseColor("#F9F6F0") }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bg)
    }

    private fun drawHeader(
        canvas: Canvas,
        title: String,
        subject: String,
        side: String,
        headerPaint: Paint,
        subPaint: Paint
    ) {
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("FLASHSNAP • $subject", MARGIN, 22f, headerPaint)
        canvas.drawText("$title | $side | $dateStr", MARGIN, 34f, subPaint)
        
        val linePaint = Paint().apply {
            color = Color.parseColor("#E6DFC5")
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, 38f, PAGE_WIDTH - MARGIN, 38f, linePaint)
    }

    private fun drawCutGuide(canvas: Canvas, rect: RectF, cutPaint: Paint) {
        // Subtle corner cut guides
        val tick = 6f
        canvas.drawLine(rect.left - tick, rect.top, rect.left, rect.top, cutPaint)
        canvas.drawLine(rect.left, rect.top - tick, rect.left, rect.top, cutPaint)
        canvas.drawLine(rect.right, rect.top, rect.right + tick, rect.top, cutPaint)
        canvas.drawLine(rect.right, rect.top - tick, rect.right, rect.top, cutPaint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int
    ) {
        val words = text.split(" ")
        var lineY = y
        var currentLine = ""
        var linesCount = 0

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, lineY, paint)
                    linesCount++
                    lineY += paint.textSize * 1.35f
                    if (linesCount >= maxLines) {
                        canvas.drawText("...", x, lineY, paint)
                        return
                    }
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty() && linesCount < maxLines) {
            canvas.drawText(currentLine, x, lineY, paint)
        }
    }
}
