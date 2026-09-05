package cn.lineai.ui.component;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.data.grok.GrokApiException;
import cn.lineai.data.grok.GrokAuthManager;
import cn.lineai.data.grok.GrokModelsRepository;
import cn.lineai.data.grok.GrokUsageRepository;
import cn.lineai.ui.theme.LineTheme;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Grok account screen: xAI OAuth, live models, plan and weekly usage/reset. */
public final class GrokAccountScreenView extends ScreenScaffoldView {
    private final Context context;
    private final Runnable onAddModel;
    private final GrokAuthManager authManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int loadGeneration;
    private boolean busy;

    public GrokAccountScreenView(Context context, Runnable onBack, Runnable onAddModel) {
        super(context, context.getString(R.string.screen_grok_account_title), onBack, null);
        this.context = context;
        this.onAddModel = onAddModel;
        this.authManager = new GrokAuthManager(context);
        LineTheme.padding(getContent(), LineTheme.LG, LineTheme.LG, LineTheme.LG, 100);
        reload();
    }

    private void reload() {
        final int generation = ++loadGeneration;
        busy = false;
        getContent().removeAllViews();
        if (!authManager.isAuthenticated()) {
            renderSignedOut("");
            return;
        }
        renderIdentity();
        addStatusText(context.getString(R.string.screen_grok_account_loading), LineTheme.TEXT_TERTIARY);
        loadLiveData(generation);
    }

    private void renderSignedOut(String error) {
        getContent().removeAllViews();
        addSectionTitle(context.getString(R.string.screen_grok_account_section_auth));
        addCardText(context.getString(R.string.screen_grok_account_signed_out),
                context.getString(R.string.screen_grok_account_login_desc));
        if (error != null && error.length() > 0) {
            addCardText(context.getString(R.string.screen_grok_account_error_title), error);
        }
        addButton(context.getString(R.string.screen_grok_account_login), LineTheme.ACCENT,
                LineTheme.TEXT_ON_COLOR, this::startLogin);
        addCardText(context.getString(R.string.screen_grok_account_security_title),
                context.getString(R.string.screen_grok_account_security_desc));
    }

    private void startLogin() {
        if (busy) {
            return;
        }
        busy = true;
        getContent().removeAllViews();
        addStatusText(context.getString(R.string.screen_grok_account_login_wait), LineTheme.TEXT_TERTIARY);
        authManager.startLogin(new GrokAuthManager.LoginCallback() {
            @Override
            public void onUserCode(String userCode, String verificationUri) {
                mainHandler.post(() -> {
                    getContent().removeAllViews();
                    addSectionTitle(context.getString(R.string.screen_grok_account_section_auth));
                    addCardText(context.getString(R.string.screen_grok_account_device_code_title),
                            context.getString(R.string.screen_grok_account_device_code_desc, userCode));
                    addStatusText(context.getString(R.string.screen_grok_account_browser_opened), LineTheme.TEXT_TERTIARY);
                });
            }

            @Override
            public void onComplete(boolean success, String message) {
                mainHandler.post(() -> {
                    busy = false;
                    if (success) {
                        reload();
                    } else {
                        renderSignedOut(message);
                    }
                });
            }
        });
    }

    private void renderIdentity() {
        addSectionTitle(context.getString(R.string.screen_grok_account_section_auth));
        LinearLayout card = card();
        addCardRow(card, context.getString(R.string.screen_grok_account_email), emptyDash(authManager.getEmail()));
        addCardRow(card, context.getString(R.string.screen_grok_account_plan), emptyDash(authManager.getPlanType()));
        addCardRow(card, context.getString(R.string.screen_grok_account_id), maskId(authManager.getUserId()));
        getContent().addView(card, fullWidthParams(LineTheme.SM));

        addButton(context.getString(R.string.screen_grok_account_refresh), LineTheme.ACCENT,
                LineTheme.TEXT_ON_COLOR, this::reload);
        addButton(context.getString(R.string.screen_grok_account_add_model), LineTheme.SURFACE_LIGHT,
                LineTheme.TEXT, onAddModel);
        addButton(context.getString(R.string.screen_grok_account_logout), LineTheme.DANGER,
                LineTheme.TEXT_ON_COLOR, () -> {
                    authManager.logout();
                    reload();
                });
    }

    private void loadLiveData(final int generation) {
        new Thread(() -> {
            LoadResult result = new LoadResult();
            try {
                result.status = GrokUsageRepository.fetch(context);
            } catch (Exception e) {
                result.usageError = e;
            }
            try {
                result.modelIds = GrokModelsRepository.fetchModelIds(context);
            } catch (Exception e) {
                result.modelsError = e;
            }
            mainHandler.post(() -> {
                if (generation != loadGeneration) {
                    return;
                }
                if (!authManager.isAuthenticated()) {
                    renderSignedOut(context.getString(R.string.screen_grok_account_session_expired));
                    return;
                }
                renderLiveData(result);
            });
        }, "linecode-grok-account").start();
    }

    private void renderLiveData(LoadResult result) {
        getContent().removeAllViews();
        renderIdentity();

        addSectionTitle(context.getString(R.string.screen_grok_usage_section));
        if (result.status != null && result.status.getUsage() != null) {
            addQuota(result.status.getUsage());
        } else {
            addCardText(context.getString(R.string.screen_grok_usage_failed_title), safeRequestError(result.usageError));
        }

        addSectionTitle(context.getString(R.string.screen_grok_models_section));
        if (result.modelIds != null && !result.modelIds.isEmpty()) {
            addCardText(context.getString(R.string.screen_grok_models_count, result.modelIds.size()),
                    joinModelIds(result.modelIds));
        } else if (result.modelsError != null) {
            addCardText(context.getString(R.string.screen_grok_models_unavailable),
                    safeRequestError(result.modelsError));
        } else {
            addCardText(context.getString(R.string.screen_grok_models_unavailable),
                    context.getString(R.string.screen_grok_models_unavailable_desc));
        }
    }

    private void addQuota(GrokUsageRepository.GrokUsageWindow window) {
        LinearLayout card = card();
        TextView titleView = LineTheme.textMedium(context,
                context.getString(R.string.screen_grok_weekly_limit), LineTheme.FONT_MD, LineTheme.TEXT);
        card.addView(titleView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        if (window.hasUsagePercent()) {
            String used = String.format(Locale.getDefault(), "%.1f%%", window.getUsedPercent());
            String remaining = String.format(Locale.getDefault(), "%.1f%%", window.getRemainingPercent());
            addCardRow(card, context.getString(R.string.screen_grok_used), used);
            addCardRow(card, context.getString(R.string.screen_grok_remaining), remaining);

            ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(1000);
            progress.setProgress((int) Math.round(window.getUsedPercent() * 10.0));
            progress.setProgressTintList(ColorStateList.valueOf(LineTheme.ACCENT));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LineTheme.dp(context, 7));
            progressParams.topMargin = LineTheme.dp(context, LineTheme.SM);
            progressParams.bottomMargin = LineTheme.dp(context, LineTheme.SM);
            card.addView(progress, progressParams);
        } else {
            addCardRow(card, context.getString(R.string.screen_grok_used),
                    context.getString(R.string.screen_grok_account_unknown));
        }
        if (window.getPeriodType().length() > 0) {
            addCardRow(card, context.getString(R.string.screen_grok_period), friendlyPeriod(window.getPeriodType()));
        }
        addCardRow(card, context.getString(R.string.screen_grok_reset),
                formatReset(window.getResetsAtEpochSeconds()));
        getContent().addView(card, fullWidthParams(LineTheme.SM));
    }

    private String friendlyPeriod(String periodType) {
        if (periodType.toUpperCase(Locale.ROOT).contains("WEEKLY")) {
            return context.getString(R.string.screen_grok_period_weekly);
        }
        if (periodType.toUpperCase(Locale.ROOT).contains("MONTHLY")) {
            return context.getString(R.string.screen_grok_period_monthly);
        }
        return periodType;
    }

    private String formatReset(long epochSeconds) {
        if (epochSeconds <= 0L) {
            return context.getString(R.string.screen_grok_reset_unknown);
        }
        long resetMillis = epochSeconds * 1000L;
        long remaining = resetMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            return context.getString(R.string.screen_grok_reset_now);
        }
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        return format.format(new Date(resetMillis)) + " · "
                + context.getString(R.string.screen_grok_reset_in, formatDuration(remaining));
    }

    private String formatDuration(long millis) {
        long totalMinutes = Math.max(1L, millis / 60000L);
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private String safeRequestError(Exception error) {
        if (error instanceof GrokApiException && ((GrokApiException) error).isUnauthorized()) {
            return context.getString(R.string.screen_grok_account_session_expired);
        }
        return context.getString(R.string.screen_grok_request_failed);
    }

    private String joinModelIds(List<String> ids) {
        StringBuilder result = new StringBuilder();
        int limit = Math.min(ids.size(), 60);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(ids.get(i));
        }
        if (ids.size() > limit) {
            result.append('\n').append("…");
        }
        return result.toString();
    }

    private String maskId(String value) {
        if (value == null || value.length() == 0) {
            return context.getString(R.string.screen_grok_account_unknown);
        }
        if (value.length() <= 8) {
            return value.substring(0, Math.min(4, value.length())) + "…";
        }
        return value.substring(0, 4) + "…" + value.substring(value.length() - 4);
    }

    private String emptyDash(String value) {
        return value == null || value.length() == 0
                ? context.getString(R.string.screen_grok_account_unknown)
                : value;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(LineTheme.rounded(context, LineTheme.SURFACE_ELEVATED, 12));
        LineTheme.padding(card, LineTheme.MD, LineTheme.MD, LineTheme.MD, LineTheme.MD);
        return card;
    }

    private void addCardText(String title, String body) {
        LinearLayout card = card();
        TextView titleView = LineTheme.textMedium(context, title, LineTheme.FONT_MD, LineTheme.TEXT);
        card.addView(titleView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (body != null && body.length() > 0) {
            TextView bodyView = LineTheme.text(context, body, LineTheme.FONT_SM,
                    LineTheme.TEXT_SECONDARY, Typeface.NORMAL);
            bodyView.setLineSpacing(LineTheme.dp(context, 3), 1f);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = LineTheme.dp(context, LineTheme.XS);
            card.addView(bodyView, bodyParams);
        }
        getContent().addView(card, fullWidthParams(LineTheme.SM));
    }

    private void addCardRow(LinearLayout card, String label, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = LineTheme.text(context, label, LineTheme.FONT_SM,
                LineTheme.TEXT_TERTIARY, Typeface.NORMAL);
        row.addView(labelView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView valueView = LineTheme.textMedium(context, value == null ? "" : value,
                LineTheme.FONT_SM, LineTheme.TEXT);
        valueView.setGravity(Gravity.RIGHT);
        row.addView(valueView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = LineTheme.dp(context, LineTheme.XS);
        card.addView(row, params);
    }

    private void addSectionTitle(String title) {
        TextView view = LineTheme.textMedium(context, title.toUpperCase(Locale.ROOT),
                LineTheme.FONT_XS, LineTheme.TEXT_TERTIARY);
        view.setLetterSpacing(0.05f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = LineTheme.dp(context, LineTheme.MD);
        params.bottomMargin = LineTheme.dp(context, LineTheme.SM);
        getContent().addView(view, params);
    }

    private void addStatusText(String text, int color) {
        TextView status = LineTheme.text(context, text, LineTheme.FONT_SM, color, Typeface.NORMAL);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, LineTheme.dp(context, LineTheme.LG), 0, LineTheme.dp(context, LineTheme.LG));
        getContent().addView(status, fullWidthParams(LineTheme.SM));
    }

    private void addButton(String label, int background, int textColor, final Runnable action) {
        if (action == null) {
            return;
        }
        TextView button = LineTheme.textMedium(context, label, LineTheme.FONT_SM, textColor);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(LineTheme.dp(context, 44));
        button.setBackground(LineTheme.rounded(context, background, 10));
        button.setClickable(true);
        button.setOnClickListener(v -> action.run());
        getContent().addView(button, fullWidthParams(LineTheme.SM));
    }

    private LinearLayout.LayoutParams fullWidthParams(int bottomMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.bottomMargin = LineTheme.dp(context, bottomMarginDp);
        return params;
    }

    private static final class LoadResult {
        GrokUsageRepository.GrokAccountStatus status;
        List<String> modelIds;
        Exception usageError;
        Exception modelsError;
    }
}
