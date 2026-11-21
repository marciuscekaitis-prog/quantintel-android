package com.quantintel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

private val Ink = Color(0xFF0B1220)
private val Panel = Color(0xFF121D2E)
private val Muted = Color(0xFF91A0B7)
private val Green = Color(0xFF35D49A)
private val Red = Color(0xFFFF6B78)
private val Blue = Color(0xFF6BA8FF)

data class Stock(
    val symbol: String,
    val price: Double,
    val changePct: Double,
    val rsi: Double,
    val sma50: Double,
    val volatility: Double,
    val score: Double,
    val history: List<Double>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuantIntelApp() }
    }
}

@Composable
fun QuantIntelApp() {
    val scope = rememberCoroutineScope()
    var stocks by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var updated by remember { mutableStateOf("Not refreshed yet") }

    suspend fun refresh() {
        loading = true
        error = null
        try {
            val results = withContext(Dispatchers.IO) {
                listOf("AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "AVGO", "LLY", "JPM", "SPY", "QQQ")
                    .mapNotNull { symbol -> runCatching { fetchStock(symbol) }.getOrNull() }
            }
            if (results.isEmpty()) error = "No market data returned. Check your connection and try again."
            else {
                stocks = results.sortedByDescending { it.score }
                updated = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(java.util.Date())
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load market data."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("QUANTINTEL", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("Market intelligence", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("US EQUITIES", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("SCANNED", if (stocks.isEmpty()) "—" else stocks.size.toString(), Modifier.weight(1f))
                    Metric("TOP SIGNAL", stocks.firstOrNull()?.symbol ?: "—", Modifier.weight(1f))
                    Metric("DATA", "DAILY", Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Opportunity radar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ranked by transparent technical signals", color = Muted, fontSize = 11.sp)
                    }
                    Button(onClick = { scope.launch { refresh() } }, enabled = !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(if (loading) "SCANNING…" else "↻ REFRESH", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Text("Updated: $updated", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp, bottom = 10.dp))
                if (loading && stocks.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Green)
                    }
                } else {
                    error?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                        items(stocks, key = { it.symbol }) { stock ->
                            StockCard(stock, expanded = selected == stock.symbol, onClick = {
                                selected = if (selected == stock.symbol) null else stock.symbol
                            })
                        }
                        item {
                            Text(
                                "Research tool only • Data from Yahoo Finance public chart endpoint. Quotes may be delayed or unavailable. Scores are heuristic, not predictions or investment advice. No trading is performed.",
                                color = Muted, fontSize = 10.sp, lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StockCard(stock: Stock, expanded: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable { onClick() }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stock.symbol, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("RSI  " + fmt(stock.rsi) + "    ·    50D trend  " + if (stock.price >= stock.sma50) "BULLISH" else "BELOW", color = Muted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$" + fmt(stock.price), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text((if (stock.changePct >= 0) "+" else "") + fmt(stock.changePct) + "%", color = if (stock.changePct >= 0) Green else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QUANT SCORE", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text(fmt(stock.score) + " / 100", color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF26364A), RoundedCornerShape(8.dp))) {
            Box(Modifier.fillMaxWidth((stock.score / 100.0).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(Green, RoundedCornerShape(8.dp)))
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            PriceChart(stock.history)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Detail("RSI (14)", fmt(stock.rsi))
                Detail("20D VOL.", fmt(stock.volatility) + "%")
                Detail("SMA (50)", "$" + fmt(stock.sma50))
            }
            Text("Tap card to collapse", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text("Tap for chart & metrics", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun Detail(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PriceChart(values: List<Double>) {
    Canvas(Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF0D1725), RoundedCornerShape(8.dp)).padding(8.dp)) {
        if (values.size < 2) return@Canvas
        val low = values.minOrNull() ?: return@Canvas
        val high = values.maxOrNull() ?: return@Canvas
        val range = (high - low).takeIf { it > 0.0 } ?: 1.0
        val points = values.mapIndexed { i, v ->
            Offset(x = i.toFloat() / (values.size - 1) * size.width, y = size.height - ((v - low) / range).toFloat() * size.height)
        }
        for (i in 0 until points.lastIndex) drawLine(Blue, points[i], points[i + 1], strokeWidth = 3f)
    }
}

private fun fetchStock(symbol: String): Stock {
    val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=6mo&interval=1d")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("User-Agent", "Mozilla/5.0 QUANTINTEL/1.0")
    }
    try {
        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val quote = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
        val closes = quote.getJSONArray("close")
        val prices = (0 until closes.length()).mapNotNull { i ->
            if (closes.isNull(i)) null else closes.optDouble(i).takeIf { it.isFinite() && it > 0 }
        }
        require(prices.size >= 55) { "Insufficient price history for $symbol" }
        val last = prices.last()
        val previous = prices[prices.lastIndex - 1]
        val sma50 = prices.takeLast(50).average()
        val returns = prices.zipWithNext { a, b -> (b / a) - 1.0 }
        val gains = returns.takeLast(14).map { it.coerceAtLeast(0.0) }.average()
        val losses = returns.takeLast(14).map { (-it).coerceAtLeast(0.0) }.average()
        val rsi = if (losses == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + gains / losses))
        val recent = returns.takeLast(20)
        val mean = recent.average()
        val vol = sqrt(recent.map { (it - mean).pow(2) }.average()) * sqrt(252.0) * 100.0
        val momentum = ((last / prices[prices.size - 22]) - 1.0) * 100.0
        val trend = if (last >= sma50) 1.0 else 0.0
        val rsiScore = when { rsi in 45.0..65.0 -> 1.0; rsi in 35.0..75.0 -> 0.6; else -> 0.2 }
        val momentumScore = ((momentum + 15.0) / 30.0).coerceIn(0.0, 1.0)
        val volScore = (1.0 - (vol / 80.0)).coerceIn(0.0, 1.0)
        val score = (trend * 35.0) + (rsiScore * 25.0) + (momentumScore * 25.0) + (volScore * 15.0)
        return Stock(symbol, last, ((last / previous) - 1.0) * 100.0, rsi, sma50, vol, score, prices.takeLast(60))
    } finally {
        connection.disconnect()
    }
}

private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)
