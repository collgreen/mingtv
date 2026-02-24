package com.github.tvbox.osc.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.GitHubUtils;
import com.lxj.xpopup.core.BottomPopupView;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;

import java.io.File;

public class UpdateDialog extends BottomPopupView {

    private final String versionName;
    private final String changelog;
    private final String downloadUrl;

    public UpdateDialog(@NonNull android.content.Context context,
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

        TextView tvVersion        = findViewById(R.id.tv_update_version);
        TextView tvLog            = findViewById(R.id.tv_update_changelog);
        View     btnDownload      = findViewById(R.id.btn_update_download);
        View     btnLater         = findViewById(R.id.btn_update_later);
        LinearLayout layoutProgress = findViewById(R.id.layout_download_progress);
        ProgressBar  progressBar  = findViewById(R.id.progress_download);
        TextView tvStatus         = findViewById(R.id.tv_download_status);

        tvVersion.setText("新版本：v" + versionName);
        tvLog.setText(changelog.isEmpty() ? "作者太懒，没写更新说明 :)" : changelog);

        btnDownload.setOnClickListener(v -> {
            btnDownload.setVisibility(View.GONE);
            btnLater.setVisibility(View.GONE);
            layoutProgress.setVisibility(View.VISIBLE);
            tvStatus.setText("准备下载...");

            String url = GitHubUtils.applyBestProxy(downloadUrl);
            File saveFile = new File(getContext().getExternalCacheDir(), "mingtv_update.apk");
            if (saveFile.exists()) saveFile.delete();

            OkGo.<File>get(url)
                    .execute(new FileCallback(saveFile.getParent(), saveFile.getName()) {
                        @Override
                        public void onSuccess(Response<File> response) {
                            post(() -> {
                                tvStatus.setText("下载完成，正在安装...");
                                installApk(response.body());
                            });
                        }

                        @Override
                        public void downloadProgress(Progress progress) {
                            post(() -> {
                                int pct = (int) (progress.fraction * 100);
                                progressBar.setProgress(pct);
                                long curMB   = progress.currentSize / 1024 / 1024;
                                long totalMB = progress.totalSize   / 1024 / 1024;
                                tvStatus.setText(curMB + "MB / " + totalMB + "MB  " + pct + "%");
                            });
                        }

                        @Override
                        public void onError(Response<File> response) {
                            post(() -> {
                                ToastUtils.showShort("下载失败，请重试");
                                layoutProgress.setVisibility(View.GONE);
                                btnDownload.setVisibility(View.VISIBLE);
                                btnLater.setVisibility(View.VISIBLE);
                            });
                        }
                    });
        });

        btnLater.setOnClickListener(v -> dismiss());
    }

    private void installApk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(getContext(),
                    getContext().getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apkFile);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        dismiss();
    }
}
