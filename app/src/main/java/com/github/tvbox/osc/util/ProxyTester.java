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
 * 策略：优先使用代理（ghfast.top / mirror.ghproxy.com / ghproxy.com）并发竞速，
 * 只有当全部代理均失败时才回落到直连。结果缓存 12 小时。
 *
 * - 首次启动（无缓存）：等待测速完成再回调 onDone
 * - 后续启动（缓存有效）：立即回调，后台静默重测保持缓存更新
 */
public class ProxyTester {

    private static final long CACHE_TTL_MS = 12 * 60 * 60 * 1000L;
    private static final String TAG = "proxy_test";
    /** 代理数量（排除最后一个直连） */
    private static final int PROXY_ONLY_COUNT = GitHubUtils.PROXY_COUNT - 1;
    /** 直连的索引 */
    private static final int DIRECT_IDX = GitHubUtils.PROXY_COUNT - 1;

    public static void testAndCache(Runnable onDone) {
        long lastTime = Hawk.get(HawkConfig.GITHUB_PROXY_CACHE_TIME, 0L);
        boolean hasFreshCache = (System.currentTimeMillis() - lastTime) < CACHE_TTL_MS;

        if (hasFreshCache) {
            new Handler(Looper.getMainLooper()).post(onDone);
            runTest(null);
            return;
        }

        runTest(onDone);
    }

    private static void runTest(Runnable onDone) {
        String rawUrl = GitHubUtils.rawUrlForPath("update.json");
        List<String> urls = GitHubUtils.getAllProxiedUrls(rawUrl);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger proxyFailed = new AtomicInteger(0);

        // 第一阶段：只并发测代理，不测直连
        for (int i = 0; i < PROXY_ONLY_COUNT; i++) {
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
                            if (proxyFailed.incrementAndGet() == PROXY_ONLY_COUNT) {
                                // 第二阶段：所有代理均失败，尝试直连兜底
                                tryDirect(urls.get(DIRECT_IDX), done, onDone);
                            }
                        }
                    });
        }
    }

    /** 直连兜底，所有代理都不通时才调用 */
    private static void tryDirect(String url, AtomicBoolean done, Runnable onDone) {
        OkGo.<String>get(url)
                .tag(TAG)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        if (done.compareAndSet(false, true)) {
                            Hawk.put(HawkConfig.GITHUB_PROXY_IDX, DIRECT_IDX);
                            Hawk.put(HawkConfig.GITHUB_PROXY_CACHE_TIME, System.currentTimeMillis());
                            if (onDone != null) {
                                new Handler(Looper.getMainLooper()).post(onDone);
                            }
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        // 全部失败，沿用上次缓存的索引继续启动
                        if (done.compareAndSet(false, true)) {
                            if (onDone != null) {
                                new Handler(Looper.getMainLooper()).post(onDone);
                            }
                        }
                    }
                });
    }
}
