package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GitHub 代理自动测速
 *
 * 并发请求所有代理变体，取最先成功响应的作为最佳代理，结果缓存 12 小时。
 * - 首次启动（无缓存）：等待测速完成再回调 onDone
 * - 后续启动（缓存有效）：立即回调，后台静默重测以保持缓存更新
 */
public class ProxyTester {

    private static final long CACHE_TTL_MS = 12 * 60 * 60 * 1000L;
    private static final String TAG = "proxy_test";

    public static void testAndCache(Runnable onDone) {
        long lastTime = Hawk.get(HawkConfig.GITHUB_PROXY_CACHE_TIME, 0L);
        boolean hasFreshCache = (System.currentTimeMillis() - lastTime) < CACHE_TTL_MS;

        if (hasFreshCache) {
            // 缓存有效，立即继续启动流程，后台静默重测
            new Handler(Looper.getMainLooper()).post(onDone);
            runTest(null);
            return;
        }

        // 无有效缓存（首次安装或缓存过期），等测速完成再继续
        runTest(onDone);
    }

    private static void runTest(Runnable onDone) {
        String rawUrl = GitHubUtils.rawUrlForPath("update.json");
        List<String> urls = GitHubUtils.getAllProxiedUrls(rawUrl);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger failed = new AtomicInteger(0);
        int total = urls.size();

        for (int i = 0; i < total; i++) {
            final int idx = i;
            OkGo.<String>get(urls.get(i))
                    .tag(TAG)
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            if (done.compareAndSet(false, true)) {
                                OkGo.getInstance().cancelTag(TAG);
                                Hawk.put(HawkConfig.GITHUB_PROXY_IDX, idx);
                                Hawk.put(HawkConfig.GITHUB_PROXY_CACHE_TIME, System.currentTimeMillis());
                                if (onDone != null) {
                                    new Handler(Looper.getMainLooper()).post(onDone);
                                }
                            }
                        }

                        @Override
                        public void onError(Response<String> response) {
                            if (failed.incrementAndGet() == total) {
                                // 全部失败，沿用已缓存索引（或默认 0）继续启动
                                if (done.compareAndSet(false, true)) {
                                    if (onDone != null) {
                                        new Handler(Looper.getMainLooper()).post(onDone);
                                    }
                                }
                            }
                        }
                    });
        }
    }
}
