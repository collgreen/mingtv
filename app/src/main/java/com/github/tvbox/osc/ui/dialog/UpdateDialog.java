package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.GitHubUtils;
import com.lxj.xpopup.core.BottomPopupView;

public class UpdateDialog extends BottomPopupView {

    private final String versionName;
    private final String changelog;
    private final String downloadUrl;

    public UpdateDialog(@NonNull Context context,
                        String versionName,
                        String changelog,
                        String downloadUrl) {
        super(context);
        this.versionName = versionName;
        this.changelog   = changelog;
        this.downloadUrl = downloadUrl;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_update;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        TextView tvVersion  = findViewById(R.id.tv_update_version);
        TextView tvLog      = findViewById(R.id.tv_update_changelog);
        View     btnDownload = findViewById(R.id.btn_update_download);
        View     btnLater   = findViewById(R.id.btn_update_later);

        tvVersion.setText("新版本：v" + versionName);
        tvLog.setText(changelog.isEmpty() ? "作者太懒，没写更新说明 :)" : changelog);

        btnDownload.setOnClickListener(v -> {
            String url = GitHubUtils.applyBestProxy(downloadUrl);
            getContext().startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
            dismiss();
        });

        btnLater.setOnClickListener(v -> dismiss());
    }
}
