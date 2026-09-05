    private View modelPopupAnchor;
    private LinearLayout modelPopupContent;

    private void dismissComposerPopups() {
        if (modelSubPopup != null && modelSubPopup.isShowing()) {
            modelSubPopup.dismiss();
        }
        modelSubPopup = null;
        if (modelPopup != null && modelPopup.isShowing()) {
            modelPopup.dismiss();
        }
        if (modePopup != null && modePopup.isShowing()) {
            modePopup.dismiss();
        }
        dismissSlashPopup();
    }

    private void showModelPopup(View anchor) {
        if (streaming) return;
        if (modelPopup != null && modelPopup.isShowing()) {
            modelPopup.dismiss();
            return;
        }
        dismissComposerPopups();
        input.clearFocus();
        Context ctx = getContext();
        java.util.LinkedHashMap<String, String> sources = collectModelSources();
        if (sources.isEmpty()) {
            if (listener != null) listener.onModelManageClick();
            return;
        }

        modelPopupAnchor = anchor;
        modelPopupContent = new LinearLayout(ctx);
        modelPopupContent.setOrientation(VERTICAL);
        modelPopupContent.setBackground(LineCards.cardBackground(ctx, LineTheme.INPUT_BG, LineTheme.BORDER_LIGHT));
        LineTheme.padding(modelPopupContent, 4, 4, 4, 4);
        fillModelSourceList(modelPopupContent, sources);

        int popupWidth = LineTheme.dp(ctx, 220);
        modelPopup = new PopupWindow(modelPopupContent, popupWidth, LayoutParams.WRAP_CONTENT, true);
        modelPopup.setOutsideTouchable(true);
        modelPopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        modelPopup.setOnDismissListener(() -> {
            if (modelSubPopup != null && modelSubPopup.isShowing()) modelSubPopup.dismiss();
        });
        showPopupAboveComposer(modelPopup, anchor, popupWidth, 0, false);
    }

    private java.util.LinkedHashMap<String, String> collectModelSources() {
        java.util.LinkedHashMap<String, String> sources = new java.util.LinkedHashMap<>();
        for (ModelConfig m : availableModels) {
            String key = m.getProviderLabel().length() > 0 ? m.getProviderLabel() : "Other";
            if (!sources.containsKey(key)) {
                sources.put(key, m.getBaseUrl());
            }
        }
        return sources;
    }

    private void fillModelSourceList(LinearLayout content, java.util.LinkedHashMap<String, String> sources) {
        content.removeAllViews();
        Context ctx = getContext();
        int rowHeight = LineTheme.dp(ctx, 40);
        int manageRowHeight = LineTheme.dp(ctx, 36);
        for (String sName : sources.keySet()) {
            String currentModelName = "";
            for (ModelConfig m : availableModels) {
                String pk = m.getProviderLabel().length() > 0 ? m.getProviderLabel() : "Other";
                if (pk.equals(sName) && m.getId().equals(selectedModelId)) {
                    currentModelName = m.getName().length() > 0 ? m.getName() : m.getModelId();
                    break;
                }
            }
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            boolean isActive = currentModelName.length() > 0;
            row.setBackground(LineTheme.rounded(ctx, isActive ? LineTheme.ACCENT_DIM : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_SM));
            LineTheme.padding(row, LineTheme.SM, 0, LineTheme.SM, 0);
            row.setClickable(true);
            TextView nameView = LineTheme.textMedium(ctx, sName, LineTheme.FONT_SM, isActive ? LineTheme.ACCENT : LineTheme.TEXT);
            nameView.setSingleLine(true);
            row.addView(nameView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            TextView arrow = LineTheme.text(ctx, "\u203A", LineTheme.FONT_SM, LineTheme.TEXT_TERTIARY, Typeface.NORMAL);
            row.addView(arrow, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            final String sourceName = sName;
            final String baseUrl = sources.get(sName);
            row.setOnClickListener(v -> fillModelSubList(content, sourceName, baseUrl));
            content.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        }
        View div = new View(ctx);
        div.setBackgroundColor(LineTheme.BORDER_LIGHT);
        content.addView(div, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1));
        TextView manageItem = LineTheme.textMedium(ctx, ctx.getString(R.string.composer_model_manage), LineTheme.FONT_XS, LineTheme.TEXT_TERTIARY);
        manageItem.setGravity(Gravity.CENTER_VERTICAL);
        manageItem.setPadding(LineTheme.dp(ctx, LineTheme.SM), 0, 0, 0);
        manageItem.setClickable(true);
        manageItem.setOnClickListener(v -> {
            if (modelPopup != null) modelPopup.dismiss();
            post(() -> { if (listener != null) listener.onModelManageClick(); });
        });
        content.addView(manageItem, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, manageRowHeight));
        relayoutModelPopup();
    }

    private void fillModelSubList(LinearLayout content, String sourceName, String baseUrl) {
        content.removeAllViews();
        Context ctx = getContext();
        int rowHeight = LineTheme.dp(ctx, 40);

        TextView back = LineTheme.textMedium(ctx, "\u2190  " + sourceName, LineTheme.FONT_SM, LineTheme.TEXT);
        back.setGravity(Gravity.CENTER_VERTICAL);
        LineTheme.padding(back, LineTheme.SM, 0, LineTheme.SM, 0);
        back.setClickable(true);
        back.setOnClickListener(v -> fillModelSourceList(content, collectModelSources()));
        content.addView(back, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));

        TextView queryBtn = LineTheme.textMedium(ctx, ctx.getString(R.string.composer_model_submenu_query_button), LineTheme.FONT_XS, LineTheme.ACCENT);
        queryBtn.setGravity(Gravity.CENTER);
        queryBtn.setBackground(LineTheme.roundedStroke(ctx, LineTheme.ACCENT_MUTED, LineTheme.SHAPE_SM, LineTheme.ACCENT));
        LineTheme.padding(queryBtn, 0, 3, 0, 3);
        queryBtn.setClickable(true);
        queryBtn.setOnClickListener(v -> {
            queryBtn.setText(R.string.screen_model_add_query_button_loading);
            queryModelCount(baseUrl, queryBtn, ctx);
        });
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LineTheme.dp(ctx, 32));
        qp.leftMargin = LineTheme.dp(ctx, LineTheme.SM);
        qp.rightMargin = LineTheme.dp(ctx, LineTheme.SM);
        qp.bottomMargin = LineTheme.dp(ctx, 4);
        content.addView(queryBtn, qp);

        java.util.List<ModelConfig> models = new java.util.ArrayList<>();
        for (ModelConfig m : availableModels) {
            String pk = m.getProviderLabel().length() > 0 ? m.getProviderLabel() : "Other";
            if (pk.equals(sourceName)) models.add(m);
        }
        for (ModelConfig m : models) {
            boolean sel = m.getId().equals(selectedModelId);
            TextView item = LineTheme.textMedium(ctx,
                    m.getName().length() > 0 ? m.getName() : m.getModelId(),
                    LineTheme.FONT_SM, sel ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT);
            item.setSingleLine(true);
            item.setEllipsize(TextUtils.TruncateAt.END);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setBackground(LineTheme.rounded(ctx, sel ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_SM));
            LineTheme.padding(item, LineTheme.SM, 0, LineTheme.SM, 0);
            item.setClickable(true);
            final String mid = m.getId();
            item.setOnClickListener(v2 -> {
                if (modelPopup != null) modelPopup.dismiss();
                post(() -> { if (listener != null && !mid.equals(selectedModelId)) listener.onModelQuickSwitch(mid); });
            });
            content.addView(item, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        }
        relayoutModelPopup();
    }

    private void relayoutModelPopup() {
        if (modelPopup == null || !modelPopup.isShowing() || modelPopupAnchor == null) return;
        View content = modelPopup.getContentView();
        if (content == null) return;
        int popupWidth = modelPopup.getWidth();
        if (popupWidth <= 0) popupWidth = LineTheme.dp(getContext(), 220);
        content.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        modelPopup.update(popupWidth, content.getMeasuredHeight());
        showPopupAboveComposer(modelPopup, modelPopupAnchor, popupWidth, content.getMeasuredHeight(), false);
    }

    private void showModelSubMenu(View sourceRow, String sourceName, String baseUrl) {
        if (modelPopupContent != null) {
            fillModelSubList(modelPopupContent, sourceName, baseUrl);
        }
    }

    private void queryModelCount(String baseUrl, TextView queryBtn, Context ctx) {
        new Thread(() -> {
            try {
                int count = listener != null ? listener.onQueryModelCount(baseUrl) : 0;
                post(() -> {
                    queryBtn.setText(ctx.getString(R.string.composer_model_submenu_count_label, count));
                    android.widget.Toast.makeText(ctx, ctx.getString(R.string.composer_model_submenu_query_done_toast, count), android.widget.Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                post(() -> queryBtn.setText(R.string.toast_query_failed));
            }
        }, "linecode-model-query").start();
    }

