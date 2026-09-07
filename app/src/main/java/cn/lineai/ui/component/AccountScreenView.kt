package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.R
import cn.lineai.ui.model.AccountModelProvider
import cn.lineai.ui.model.AccountProviderKind
import cn.lineai.ui.model.AccountQuotaKind
import cn.lineai.ui.model.AccountQuotaWindow
import cn.lineai.ui.model.AccountScreenState
import cn.lineai.ui.model.AccountScreenViewModel
import cn.lineai.ui.theme.LineTheme
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Shared Compose account screen for Codex and Grok.
 * Provider-specific OAuth, token storage and HTTP stay in the data layer.
 */
class AccountScreenView(
    context: Context,
    provider: AccountModelProvider,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onAddModel()
    }

    init {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val model: AccountScreenViewModel = viewModel(
                    key = "account-screen:${provider.kind}",
                    factory = AccountScreenViewModel.factory(context, provider)
                )
                AccountScreenTheme {
                    AccountScreenContent(
                        state = model.state.collectAsStateWithLifecycle().value,
                        onBack = listener::onBack,
                        onAddModel = listener::onAddModel,
                        onRefresh = model::refresh,
                        onLogin = model::login,
                        onLogout = model::logout
                    )
                }
            }
        }
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}

private data class AccountStrings(
    val title: Int,
    val authSection: Int,
    val signedOut: Int,
    val loginDescription: Int,
    val errorTitle: Int,
    val login: Int,
    val securityTitle: Int,
    val securityDescription: Int,
    val loginWait: Int,
    val loading: Int,
    val email: Int,
    val plan: Int,
    val accountId: Int,
    val refresh: Int,
    val addModel: Int,
    val logout: Int,
    val unknown: Int,
    val sessionExpired: Int,
    val usageSection: Int,
    val used: Int,
    val remaining: Int,
    val reset: Int,
    val resetUnknown: Int,
    val resetNow: Int,
    val resetIn: Int,
    val usageFailedTitle: Int,
    val requestFailed: Int,
    val modelsSection: Int,
    val modelsCount: Int,
    val modelsUnavailable: Int,
    val modelsUnavailableDescription: Int
)

private fun stringsFor(kind: AccountProviderKind): AccountStrings = when (kind) {
    AccountProviderKind.CODEX -> AccountStrings(
        title = R.string.screen_codex_account_title,
        authSection = R.string.screen_codex_account_section_auth,
        signedOut = R.string.screen_codex_account_signed_out,
        loginDescription = R.string.screen_codex_account_login_desc,
        errorTitle = R.string.screen_codex_account_error_title,
        login = R.string.screen_codex_account_login,
        securityTitle = R.string.screen_codex_account_security_title,
        securityDescription = R.string.screen_codex_account_security_desc,
        loginWait = R.string.screen_codex_account_login_wait,
        loading = R.string.screen_codex_account_loading,
        email = R.string.screen_codex_account_email,
        plan = R.string.screen_codex_account_plan,
        accountId = R.string.screen_codex_account_id,
        refresh = R.string.screen_codex_account_refresh,
        addModel = R.string.screen_codex_account_add_model,
        logout = R.string.screen_codex_account_logout,
        unknown = R.string.screen_codex_account_unknown,
        sessionExpired = R.string.screen_codex_account_session_expired,
        usageSection = R.string.screen_codex_usage_section,
        used = R.string.screen_codex_used,
        remaining = R.string.screen_codex_remaining,
        reset = R.string.screen_codex_reset,
        resetUnknown = R.string.screen_codex_reset_unknown,
        resetNow = R.string.screen_codex_reset_now,
        resetIn = R.string.screen_codex_reset_in,
        usageFailedTitle = R.string.screen_codex_usage_failed_title,
        requestFailed = R.string.screen_codex_request_failed,
        modelsSection = R.string.screen_codex_models_section,
        modelsCount = R.string.screen_codex_models_count,
        modelsUnavailable = R.string.screen_codex_models_unavailable,
        modelsUnavailableDescription = R.string.screen_codex_models_unavailable_desc
    )

    AccountProviderKind.GROK -> AccountStrings(
        title = R.string.screen_grok_account_title,
        authSection = R.string.screen_grok_account_section_auth,
        signedOut = R.string.screen_grok_account_signed_out,
        loginDescription = R.string.screen_grok_account_login_desc,
        errorTitle = R.string.screen_grok_account_error_title,
        login = R.string.screen_grok_account_login,
        securityTitle = R.string.screen_grok_account_security_title,
        securityDescription = R.string.screen_grok_account_security_desc,
        loginWait = R.string.screen_grok_account_login_wait,
        loading = R.string.screen_grok_account_loading,
        email = R.string.screen_grok_account_email,
        plan = R.string.screen_grok_account_plan,
        accountId = R.string.screen_grok_account_id,
        refresh = R.string.screen_grok_account_refresh,
        addModel = R.string.screen_grok_account_add_model,
        logout = R.string.screen_grok_account_logout,
        unknown = R.string.screen_grok_account_unknown,
        sessionExpired = R.string.screen_grok_account_session_expired,
        usageSection = R.string.screen_grok_usage_section,
        used = R.string.screen_grok_used,
        remaining = R.string.screen_grok_remaining,
        reset = R.string.screen_grok_reset,
        resetUnknown = R.string.screen_grok_reset_unknown,
        resetNow = R.string.screen_grok_reset_now,
        resetIn = R.string.screen_grok_reset_in,
        usageFailedTitle = R.string.screen_grok_usage_failed_title,
        requestFailed = R.string.screen_grok_request_failed,
        modelsSection = R.string.screen_grok_models_section,
        modelsCount = R.string.screen_grok_models_count,
        modelsUnavailable = R.string.screen_grok_models_unavailable,
        modelsUnavailableDescription = R.string.screen_grok_models_unavailable_desc
    )
}

@Composable
internal fun AccountScreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(LineTheme.ACCENT),
            onPrimary = Color(LineTheme.TEXT_ON_COLOR),
            background = Color(LineTheme.BG),
            onBackground = Color(LineTheme.TEXT),
            surface = Color(LineTheme.SURFACE),
            onSurface = Color(LineTheme.TEXT),
            surfaceVariant = Color(LineTheme.SURFACE_ELEVATED),
            onSurfaceVariant = Color(LineTheme.TEXT_SECONDARY),
            error = Color(LineTheme.DANGER)
        ),
        content = content
    )
}

@Composable
internal fun AccountScreenContent(
    state: AccountScreenState,
    onBack: () -> Unit,
    onAddModel: () -> Unit,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = stringsFor(state.providerKind)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        AccountHeader(stringResource(strings.title), onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.authenticated) {
                SignedInContent(state, strings, onRefresh, onAddModel, onLogout)
            } else {
                SignedOutContent(state, strings, onLogin)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun AccountHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("‹", fontSize = 30.sp, color = Color(LineTheme.TEXT))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SignedOutContent(
    state: AccountScreenState,
    strings: AccountStrings,
    onLogin: () -> Unit
) {
    SectionTitle(stringResource(strings.authSection))
    InfoCard(stringResource(strings.signedOut), stringResource(strings.loginDescription))

    if (state.sessionExpired) {
        InfoCard(
            stringResource(strings.errorTitle),
            stringResource(strings.sessionExpired),
            error = true
        )
    } else if (state.loginError.isNotBlank()) {
        InfoCard(stringResource(strings.errorTitle), state.loginError, error = true)
    }

    if (state.loginInProgress) {
        if (state.providerKind == AccountProviderKind.GROK && state.deviceCode.isNotBlank()) {
            InfoCard(
                stringResource(R.string.screen_grok_account_device_code_title),
                stringResource(R.string.screen_grok_account_device_code_desc, state.deviceCode)
            )
            Text(
                text = stringResource(R.string.screen_grok_account_browser_opened),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        } else {
            LoadingRow(stringResource(strings.loginWait))
        }
    } else {
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(strings.login))
        }
    }

    InfoCard(
        stringResource(strings.securityTitle),
        stringResource(strings.securityDescription)
    )
}

@Composable
private fun SignedInContent(
    state: AccountScreenState,
    strings: AccountStrings,
    onRefresh: () -> Unit,
    onAddModel: () -> Unit,
    onLogout: () -> Unit
) {
    SectionTitle(stringResource(strings.authSection))
    IdentityCard(state, strings)

    Button(
        onClick = onRefresh,
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(strings.refresh))
    }
    OutlinedButton(onClick = onAddModel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(strings.addModel))
    }
    Button(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(LineTheme.DANGER),
            contentColor = Color(LineTheme.TEXT_ON_COLOR)
        )
    ) {
        Text(stringResource(strings.logout))
    }

    if (state.loading) {
        LoadingRow(stringResource(strings.loading))
    }

    SectionTitle(stringResource(strings.usageSection))
    UsageContent(state, strings)

    SectionTitle(stringResource(strings.modelsSection))
    ModelsContent(state, strings)
}

@Composable
private fun LoadingRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
        Text(label, color = Color(LineTheme.TEXT_SECONDARY))
    }
}

@Composable
private fun IdentityCard(state: AccountScreenState, strings: AccountStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountRow(stringResource(strings.email), unknownIfBlank(state.email, strings.unknown))
            AccountRow(stringResource(strings.plan), unknownIfBlank(state.plan, strings.unknown))
            AccountRow(stringResource(strings.accountId), maskId(state.accountId, strings.unknown))
        }
    }
}

@Composable
private fun UsageContent(state: AccountScreenState, strings: AccountStrings) {
    if (state.usageError) {
        InfoCard(
            stringResource(strings.usageFailedTitle),
            stringResource(strings.requestFailed),
            error = true
        )
        return
    }

    val windows = state.usage?.windows.orEmpty()
    if (windows.isEmpty()) {
        if (state.providerKind == AccountProviderKind.CODEX) {
            InfoCard(
                stringResource(R.string.screen_codex_usage_unavailable),
                stringResource(R.string.screen_codex_usage_unavailable_desc)
            )
        } else {
            InfoCard(
                stringResource(strings.usageFailedTitle),
                stringResource(strings.requestFailed)
            )
        }
        return
    }

    windows.forEach { QuotaCard(state.providerKind, it, strings) }
}

@Composable
private fun QuotaCard(
    providerKind: AccountProviderKind,
    window: AccountQuotaWindow,
    strings: AccountStrings
) {
    val title = when (window.kind) {
        AccountQuotaKind.PRIMARY -> stringResource(R.string.screen_codex_primary_limit)
        AccountQuotaKind.SECONDARY -> stringResource(R.string.screen_codex_secondary_limit)
        AccountQuotaKind.SUBSCRIPTION -> stringResource(R.string.screen_grok_weekly_limit)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            if (window.hasUsagePercent) {
                AccountRow(stringResource(strings.used), formatPercent(providerKind, window.usedPercent))
                AccountRow(stringResource(strings.remaining), formatPercent(providerKind, window.remainingPercent))
                LinearProgressIndicator(
                    progress = { (window.usedPercent.coerceIn(0.0, 100.0) / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(7.dp)
                )
            } else {
                AccountRow(stringResource(strings.used), stringResource(strings.unknown))
            }

            if (providerKind == AccountProviderKind.CODEX && window.windowMinutes > 0L) {
                AccountRow(
                    stringResource(R.string.screen_codex_window),
                    formatCodexWindow(window.windowMinutes)
                )
            }
            if (providerKind == AccountProviderKind.GROK && window.periodType.isNotBlank()) {
                AccountRow(
                    stringResource(R.string.screen_grok_period),
                    friendlyGrokPeriod(window.periodType)
                )
            }
            AccountRow(
                stringResource(strings.reset),
                formatReset(window.resetsAtEpochSeconds, strings)
            )
        }
    }
}

@Composable
private fun ModelsContent(state: AccountScreenState, strings: AccountStrings) {
    if (state.modelsError) {
        InfoCard(
            stringResource(strings.modelsUnavailable),
            stringResource(strings.requestFailed),
            error = true
        )
        return
    }
    if (state.models.isEmpty()) {
        InfoCard(
            stringResource(strings.modelsUnavailable),
            stringResource(strings.modelsUnavailableDescription)
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(strings.modelsCount, state.models.size),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            state.models.take(12).forEach { modelId ->
                Text(
                    text = modelId,
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.models.size > 12) {
                Text("…", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, error: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (error) Color(LineTheme.DANGER) else Color(LineTheme.TEXT)
            )
            if (body.isNotBlank()) {
                Text(body, color = Color(LineTheme.TEXT_SECONDARY), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun AccountRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = 13.sp
        )
        Text(
            value,
            color = Color(LineTheme.TEXT),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun unknownIfBlank(value: String, unknownRes: Int): String =
    value.ifBlank { stringResource(unknownRes) }

@Composable
private fun maskId(value: String, unknownRes: Int): String {
    if (value.isBlank()) return stringResource(unknownRes)
    return if (value.length <= 8) {
        value.take(4) + "…"
    } else {
        value.take(4) + "…" + value.takeLast(4)
    }
}

private fun formatPercent(kind: AccountProviderKind, value: Double): String {
    if (kind == AccountProviderKind.CODEX) {
        return "${value.roundToInt()}%"
    }
    val tenths = (value * 10.0).roundToInt()
    return "${tenths / 10}.${abs(tenths % 10)}%"
}

@Composable
private fun formatCodexWindow(minutes: Long): String = when {
    minutes % 1440L == 0L -> stringResource(R.string.screen_codex_window_days, minutes / 1440L)
    minutes % 60L == 0L -> stringResource(R.string.screen_codex_window_hours, minutes / 60L)
    else -> stringResource(R.string.screen_codex_window_minutes, minutes)
}

@Composable
private fun friendlyGrokPeriod(periodType: String): String = when {
    periodType.uppercase(Locale.ROOT).contains("WEEKLY") ->
        stringResource(R.string.screen_grok_period_weekly)
    periodType.uppercase(Locale.ROOT).contains("MONTHLY") ->
        stringResource(R.string.screen_grok_period_monthly)
    else -> periodType
}

@Composable
private fun formatReset(epochSeconds: Long, strings: AccountStrings): String {
    if (epochSeconds <= 0L) return stringResource(strings.resetUnknown)

    val resetMillis = epochSeconds * 1000L
    val remaining = resetMillis - System.currentTimeMillis()
    if (remaining <= 0L) return stringResource(strings.resetNow)

    val context = LocalContext.current
    val resetDate = Date(resetMillis)
    val date = android.text.format.DateFormat.getDateFormat(context).format(resetDate)
    val time = android.text.format.DateFormat.getTimeFormat(context).format(resetDate)
    return "$date $time · " + stringResource(strings.resetIn, formatDuration(remaining))
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = maxOf(1L, millis / 60_000L)
    val days = totalMinutes / 1440L
    val hours = (totalMinutes % 1440L) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0L -> "${days}d ${hours}h"
        hours > 0L -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
