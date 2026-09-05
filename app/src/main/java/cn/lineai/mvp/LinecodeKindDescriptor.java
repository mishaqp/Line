package cn.lineai.mvp;
import cn.lineai.ui.theme.IconButtonView;

import android.content.Context;
import cn.lineai.R;
import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.data.model.ExtensionOverviewState;
import cn.lineai.model.LipPackageRecord;
import java.util.ArrayList;
import java.util.List;

final class LinecodeKindDescriptor implements ExtensionKindDescriptor {

    @Override
    public String kind() {
        return "linecode";
    }

    @Override
    public void setEnabled(ExtensionStore repository, String id, boolean enabled) {
        // linecode 类型不支持启用/禁用
    }

    @Override
    public void delete(ExtensionStore repository, String id) {
        // linecode 类型不支持删除
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.screen_extensions_section_linecode);
    }

    @Override
    public int iconType() {
        return IconButtonView.PACKAGE;
    }

    @Override
    public String inlineTitle(Context context) {
        return context.getString(R.string.screen_extension_detail_inline_title_linecode);
    }

    @Override
    public String inlineDesc(Context context) {
        return context.getString(R.string.screen_extension_detail_inline_desc_linecode);
    }

    @Override
    public boolean hasModifyAction() {
        return false;
    }

    @Override
    public int addActionType() {
        return ADD_ACTION_LIP;
    }

    @Override
    public List<ExtensionItem> getInstalledItems(ExtensionOverviewState state) {
        List<LipPackageRecord> packages = state == null ? null : state.getLipPackages();
        if (packages == null || packages.isEmpty()) {
            return new ArrayList<>();
        }
        List<ExtensionItem> items = new ArrayList<>(packages.size());
        for (LipPackageRecord pack : packages) {
            String version = pack.getVersion().length() == 0 ? "1.0" : pack.getVersion();
            String desc = "v" + version + " · " + pack.componentCount() + " · " + pack.getId();
            items.add(new ExtensionItem(pack.getId(), pack.getName(), desc, pack.isEnabled()));
        }
        return items;
    }

    @Override
    public String emptyMessage(Context context) {
        return context.getString(R.string.screen_extension_detail_empty_linecode);
    }

    @Override
    public String sectionTitle(Context context) {
        return context.getString(R.string.screen_extension_detail_section_install_other);
    }
}
