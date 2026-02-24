package com.github.tvbox.osc.util;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 版本更新检测
 *
 * 并发请求所有代理变体的 update.json，取最先响应的结果：
 * <pre>
 * {
 *   "versionCode": 33,
 *   "versionName": "1.3",
 *   "changelog": "修了几个 bug，优化了启动速度",
 *   "downloadUrl": "https://github.com/collgreen/mingtv/releases/download/v1.3/MINGTV_v1.3_release.apk"
 * }
 * </pre>
 */
public class UpdateChecker {

    public interface UpdateCallback {
        void onUpdateAvailable(String newVersionName, String changelog, String downloadUrl);
        void onNoUpdate();
        void onError();
    }

    public static void check(Context context, UpdateCallback callback) {
        String rawUrl = GitHubUtils.rawUrlForPath("update.json");
        List<String> urls = GitHubUtils.getAllProxiedUrls(rawUrl);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger failed = new AtomicInteger(0);
        int total = urls.size();

        for (String url : urls) {
            OkGo.<String>get(url)
                    .tag("update_check")
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            if (done.compareAndSet(false, true)) {
                                OkGo.getInstance().cancelTag("update_check");
                                try {
                                    String body = response.body();
                                    JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                                    int newCode = obj.get("versionCode").getAsInt();
                                    String newName = obj.get("versionName").getAsString();
                                    String changelog = obj.has("changelog") ? obj.get("changelog").getAsString() : "";
                                    String downloadUrl = obj.has("downloadUrl") ? obj.get("downloadUrl").getAsString() : "";
                                    int currentCode = DefaultConfig.getAppVersionCode(context);
                                    if (newCode > currentCode) {
                                        callback.onUpdateAvailable(newName, changelog, downloadUrl);
                                    } else {
                                        callback.onNoUpdate();
                                    }
                                } catch (Exception e) {
                                    callback.onError();
                                }
                            }
                        }

                        @Override
                        public void onError(Response<String> response) {
                            if (failed.incrementAndGet() == total) {
                                if (done.compareAndSet(false, true)) {
                                    callback.onError();
                                }
                            }
                        }
                    });
        }
    }
}
