package com.sushiamo.bridge

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket

object BridgePrinter {
    fun printJob(job: ClaimedPrintJob) {
        val route = parseJsonObject(job.route)
        val host = route?.optString("host", "")?.trim().orEmpty()
        val port = route?.optInt("port", 9100) ?: 9100
        if (host.isBlank()) {
            throw IllegalStateException("NO_PRINTER_HOST")
        }

        val payload = parseJsonObject(job.payload)
        val ticketText = renderTicketText(job, payload)

        sendRawTextWithRetry(host = host, port = port, text = ticketText)
    }

    private fun sendRawTextWithRetry(host: String, port: Int, text: String) {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                sendRawText(host = host, port = port, text = text)
                return
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0) {
                    Thread.sleep(350)
                }
            }
        }
        throw IllegalStateException(lastError?.message ?: "PRINT_SEND_FAILED")
    }

    private fun sendRawText(host: String, port: Int, text: String) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, sanitizePort(port)), 8000)
            socket.soTimeout = 8000
            BufferedOutputStream(socket.getOutputStream()).use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write("\n\n\n".toByteArray(Charsets.UTF_8))
                out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))
                out.flush()
            }
        }
    }

    private fun renderTicketText(job: ClaimedPrintJob, payload: JSONObject?): String {
        if (payload == null) {
            return buildString {
                appendLine("COMANDA")
                appendLine("Job: ${job.id.take(8)}")
                appendLine("Reparto: ${job.department}")
            }
        }

        val orderNumber = payload.opt("order_number")?.toString()?.takeIf { it.isNotBlank() } ?: "-"
        val department = payload.optString("department", job.department).ifBlank { job.department }
        val tableNumber = payload.opt("table_number")?.toString()?.takeIf { it.isNotBlank() } ?: "-"
        val restaurantName = payload.optString("restaurant_name", "Ristorante")
        val createdAt = payload.optString("created_at", "")
        val items = payload.optJSONArray("items") ?: JSONArray()

        return buildString {
            appendLine("COMANDA #$orderNumber")
            appendLine("REPARTO: ${department.uppercase()}")
            appendLine("TAVOLO: $tableNumber")
            appendLine("------------------------------------------")
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val qty = item.optInt("quantity", 1).coerceAtLeast(1)
                val name = item.optString("name", "Prodotto").ifBlank { "Prodotto" }
                appendLine("${qty}x $name")

                val customerNotes = item.optString("customer_notes", "").trim()
                if (customerNotes.isNotBlank()) {
                    appendLine("  Nota: $customerNotes")
                }

                val notes = item.optString("notes", "").trim()
                if (notes.isNotBlank() && notes != customerNotes) {
                    appendLine("  Nota: $notes")
                }

                val modifiers = item.optJSONArray("modifiers") ?: JSONArray()
                for (mIdx in 0 until modifiers.length()) {
                    val modifier = modifiers.optJSONObject(mIdx) ?: continue
                    val modName = modifier.optString("name", "").trim()
                    if (modName.isNotBlank()) {
                        appendLine("  - $modName")
                    }
                }
            }
            appendLine("------------------------------------------")
            appendLine(restaurantName)
            if (createdAt.isNotBlank()) appendLine(createdAt)
            appendLine("Job ${job.id.take(8)}")
        }
    }

    private fun sanitizePort(port: Int): Int {
        return if (port in 1..65535) port else 9100
    }

    private fun parseJsonObject(raw: String?): JSONObject? {
        if (raw.isNullOrBlank()) return null
        return try {
            JSONObject(raw)
        } catch (_: Throwable) {
            null
        }
    }
}
