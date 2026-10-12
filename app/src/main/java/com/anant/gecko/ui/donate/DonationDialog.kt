package com.anant.gecko.ui.donate

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.gecko.R
import com.anant.gecko.ui.pack.GeckoButton
import com.anant.gecko.ui.pack.GeckoButtonTone
import com.anant.gecko.ui.pack.GeckoIcon
import com.anant.gecko.ui.pack.GeckoText
import com.anant.gecko.ui.theme.LocalGeckoTheme

private const val RAZORPAY_DONATION_URL = "https://rzp.io/rzp/fitbuddy"
private const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/anantdark"
private const val UPI_PAYMENT_URI =
    "upi://pay?cu=INR&mc=5817&mode=19&pa=anantdark969817.rzp@rxairtel&" +
        "tn=Payment%20To%20Anantdark&tr=TaiXaWZZm9gukCqrv2"
private const val DEVELOPER_EMAIL = "fitbuddy31@proton.me"
private const val UPI_QR_ASPECT_RATIO = 674f / 1644f
private const val DONATION_BODY_HEIGHT_FRACTION = 0.72f
private val DONATION_DIALOG_MAX_WIDTH = 400.dp
private val DONATION_BODY_MAX_HEIGHT = 640.dp

private val DONATION_HEART_COLORS = listOf(
    Color(0xFFE91E63),
    Color(0xFFFF5722),
    Color(0xFF9C27B0),
    Color(0xFF1976D2),
    Color(0xFF00897B),
    Color(0xFFFFA000),
)

private enum class DonationStep { REGION, INDIA, INTERNATIONAL, UPI }

fun initialDonationHeartColorIndex(): Int =
    (System.currentTimeMillis() % DONATION_HEART_COLORS.size).toInt()

fun nextDonationHeartColorIndex(current: Int): Int =
    (current + 1) % DONATION_HEART_COLORS.size

fun donationHeartColor(index: Int): Color =
    DONATION_HEART_COLORS[index.mod(DONATION_HEART_COLORS.size)]

@Composable
fun DonationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    var step by rememberSaveable { mutableStateOf(DonationStep.REGION) }
    var showQr by rememberSaveable { mutableStateOf(false) }
    val title = when (step) {
        DonationStep.REGION -> "Fuel the free app"
        DonationStep.INDIA -> "Donate from India"
        DonationStep.INTERNATIONAL -> "Donate internationally"
        DonationStep.UPI -> "Pay with UPI"
    }
    val navigateBack = {
        step = when (step) {
            DonationStep.REGION -> DonationStep.REGION
            DonationStep.INDIA, DonationStep.INTERNATIONAL -> DonationStep.REGION
            DonationStep.UPI -> DonationStep.INDIA
        }
    }

    Dialog(
        onDismissRequest = {
            if (step == DonationStep.REGION) onDismiss() else navigateBack()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = DONATION_DIALOG_MAX_WIDTH)
                .fillMaxWidth()
                .clip(pack.shapes.dialog)
                .background(c.surface)
                .border(pack.shapes.hairline, c.surfaceBorder, pack.shapes.dialog)
                .padding(20.dp),
        ) {
            GeckoText(text = title, style = pack.type.headline, color = c.ink)
            DonationDialogContent {
                when (step) {
                    DonationStep.REGION -> DonationRegionContent(
                        context = context,
                        onIndia = { step = DonationStep.INDIA },
                        onInternational = { step = DonationStep.INTERNATIONAL },
                    )
                    DonationStep.INDIA -> IndiaDonationContent(
                        context = context,
                        onUpi = {
                            showQr = false
                            step = DonationStep.UPI
                        },
                    )
                    DonationStep.INTERNATIONAL -> InternationalDonationContent(context)
                    DonationStep.UPI -> UpiDonationContent(
                        context = context,
                        showQr = showQr,
                        onShowQr = { showQr = true },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                if (step == DonationStep.REGION) {
                    GeckoButton(
                        label = "Close",
                        onClick = onDismiss,
                        tone = GeckoButtonTone.Ghost,
                    )
                } else {
                    GeckoButton(
                        label = "Back",
                        onClick = navigateBack,
                        tone = GeckoButtonTone.Ghost,
                    )
                    GeckoButton(
                        label = "Close",
                        onClick = onDismiss,
                        tone = GeckoButtonTone.Ghost,
                    )
                }
            }
        }
    }
}

@Composable
private fun DonationRegionContent(
    context: Context,
    onIndia: () -> Unit,
    onInternational: () -> Unit,
) {
    DonationInfoCard(
        iconRes = R.drawable.ic_ph_heart,
        text = "Gecko stays free and open source because of optional tips. " +
            "Buy the dev a chai. No account, no pressure.",
    )
    DonationSectionTitle("Where are you?")
    GeckoButton(
        label = "India · UPI or card",
        onClick = onIndia,
        tone = GeckoButtonTone.Accent,
        leadingIcon = R.drawable.ic_ph_map_pin,
        modifier = Modifier.fillMaxWidth(),
    )
    GeckoButton(
        label = "International · GitHub Sponsors",
        onClick = onInternational,
        tone = GeckoButtonTone.Ink,
        leadingIcon = R.drawable.ic_ph_globe,
        modifier = Modifier.fillMaxWidth(),
    )
    DonationSectionTitle("Say hi")
    val pack = LocalGeckoTheme.current
    GeckoText(DEVELOPER_EMAIL, pack.type.body, pack.colors.ink)
    GeckoButton(
        label = "Copy email",
        onClick = { copyDeveloperEmail(context) },
        tone = GeckoButtonTone.Ghost,
        leadingIcon = R.drawable.ic_ph_copy,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun IndiaDonationContent(
    context: Context,
    onUpi: () -> Unit,
) {
    DonationInfoCard(
        iconRes = R.drawable.ic_ph_wallet,
        text = "UPI is fastest. Razorpay works if you prefer a debit or credit card.",
    )
    DonationSectionTitle("Payment method")
    GeckoButton(
        label = "Pay with UPI",
        onClick = onUpi,
        tone = GeckoButtonTone.Accent,
        leadingIcon = R.drawable.ic_ph_wallet,
        modifier = Modifier.fillMaxWidth(),
    )
    GeckoButton(
        label = "Pay by card with Razorpay",
        onClick = { openRazorpayPaymentPage(context) },
        tone = GeckoButtonTone.Ghost,
        leadingIcon = R.drawable.ic_ph_credit_card,
        modifier = Modifier.fillMaxWidth(),
    )
    DonationPrivacyNote(
        "Payment apps open only when you choose them. Gecko never sees your card details.",
    )
}

@Composable
private fun InternationalDonationContent(context: Context) {
    DonationInfoCard(
        iconRes = R.drawable.ic_ph_globe,
        text = "GitHub Sponsors is the easiest way to tip from outside India.",
    )
    DonationSectionTitle("International payment")
    GeckoButton(
        label = "Sponsor on GitHub",
        onClick = { openGitHubSponsorsPage(context) },
        tone = GeckoButtonTone.Accent,
        leadingIcon = R.drawable.ic_ph_arrow_square_out,
        modifier = Modifier.fillMaxWidth(),
    )
    DonationPrivacyNote(
        "GitHub opens only when you tap. Gecko does not store payment details.",
    )
}

@Composable
private fun UpiDonationContent(
    context: Context,
    showQr: Boolean,
    onShowQr: () -> Unit,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    DonationInfoCard(
        iconRes = R.drawable.ic_ph_wallet,
        text = "Open a UPI app on this phone, or scan the QR from another device.",
    )
    GeckoButton(
        label = "Pay with UPI app",
        onClick = { openUpiApp(context) },
        tone = GeckoButtonTone.Accent,
        leadingIcon = R.drawable.ic_ph_arrow_square_out,
        modifier = Modifier.fillMaxWidth(),
    )
    GeckoButton(
        label = "Show UPI QR",
        onClick = onShowQr,
        tone = GeckoButtonTone.Ghost,
        leadingIcon = R.drawable.ic_ph_qr_code,
        modifier = Modifier.fillMaxWidth(),
    )
    if (showQr) {
        DonationSectionTitle("Scan from another device")
        Image(
            painter = painterResource(R.drawable.donation_upi_qr),
            contentDescription = "UPI QR code for supporting Gecko development",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(UPI_QR_ASPECT_RATIO)
                .clip(pack.shapes.panel),
        )
        GeckoText(
            "Before paying, verify that the recipient is Anantdark.",
            pack.type.caption,
            c.inkMuted,
        )
    }
}

@Composable
private fun DonationDialogContent(content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * DONATION_BODY_HEIGHT_FRACTION)
        .dp
        .coerceAtMost(DONATION_BODY_MAX_HEIGHT)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun DonationInfoCard(iconRes: Int, text: String) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(pack.shapes.panel)
            .background(c.canvasDeep.copy(alpha = 0.45f))
            .border(pack.shapes.hairline, c.surfaceBorder.copy(alpha = 0.5f), pack.shapes.panel)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GeckoIcon(iconRes, null, c.accent, size = 26.dp)
        GeckoText(
            text = text,
            style = pack.type.body,
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DonationSectionTitle(text: String) {
    val pack = LocalGeckoTheme.current
    GeckoText(text, pack.type.caption, pack.colors.inkMuted)
}

@Composable
private fun DonationPrivacyNote(text: String) {
    val pack = LocalGeckoTheme.current
    GeckoText(text, pack.type.caption, pack.colors.inkMuted)
}

private fun copyDeveloperEmail(context: Context) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (clipboard == null) {
        Toast.makeText(context, "Couldn't access the clipboard", Toast.LENGTH_SHORT).show()
        return
    }
    clipboard.setPrimaryClip(ClipData.newPlainText("Gecko developer email", DEVELOPER_EMAIL))
    Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
}

private fun openUpiApp(context: Context) {
    launchExternalIntent(
        context = context,
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(UPI_PAYMENT_URI)),
        errorMessage = "No UPI app found",
    )
}

private fun openRazorpayPaymentPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RAZORPAY_DONATION_URL)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    launchExternalIntent(context, intent, "Couldn't open the payment page")
}

private fun openGitHubSponsorsPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_SPONSORS_URL)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    launchExternalIntent(context, intent, "Couldn't open GitHub Sponsors")
}

private fun launchExternalIntent(
    context: Context,
    intent: Intent,
    errorMessage: String,
) {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
}
