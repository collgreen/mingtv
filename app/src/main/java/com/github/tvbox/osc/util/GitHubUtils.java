package com.github.tvbox.osc.util;

import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub 访问工具 —— 处理代理前缀、拼接 raw/发布地址
 *
 * 代理索引（由 ProxyTester 自动测速后选出，缓存到 HawkConfig.GITHUB_PROXY_IDX）：
 *   0  ghfast.top        — 国内可直连，格式：https://ghfast.top/raw.xxx
 *   1  mirror.ghproxy.com — 格式：https://mirror.ghproxy.com/https://raw.xxx
 *   2  ghproxy.com       — 格式：https://ghproxy.com/https://raw.xxx
 *   3  直连              — https://raw.xxx
 */
public class GitHubUtils {

    public static final String REPO    = "collgreen/mingtv";
    public static final String BRANCH  = "master";
    public static final String RAW_BASE = "https://raw.githubusercontent.com/";

    static final int PROXY_COUNT = 4;

    /**
     * 构造仓库某文件的裸 raw 地址（不含代理前缀）
     */
    public static String rawUrlForPath(String path) {
        return RAW_BASE + REPO + "/" + BRANCH + "/" + path;
    }

    /**
     * 根据代理索引，把 rawUrl 转换为对应的代理 URL
     */
    public static String buildProxiedUrl(int proxyIdx, String rawUrl) {
        switch (proxyIdx) {
            case 0: // ghfast.top — 不含 https:// 前缀
                return "https://ghfast.top/" + rawUrl.replaceFirst("^https?://", "");
            case 1: // mirror.ghproxy.com
                return "https://mirror.ghproxy.com/" + rawUrl;
            case 2: // ghproxy.com
                return "https://ghproxy.com/" + rawUrl;
            default: // 直连
                return rawUrl;
        }
    }

    /**
     * 返回 rawUrl 对应的所有代理变体（按 0~3 索引顺序）
     */
    public static List<String> getAllProxiedUrls(String rawUrl) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < PROXY_COUNT; i++) {
            result.add(buildProxiedUrl(i, rawUrl));
        }
        return result;
    }

    /**
     * 对 rawUrl 应用当前缓存的最佳代理
     */
    public static String applyBestProxy(String rawUrl) {
        int idx = Hawk.get(HawkConfig.GITHUB_PROXY_IDX, 0);
        if (idx < 0 || idx >= PROXY_COUNT) idx = 0;
        return buildProxiedUrl(idx, rawUrl);
    }

    /**
     * 判断某 URL 是否属于本仓库托管的内容
     */
    public static boolean isManagedUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.contains("raw.githubusercontent.com/" + REPO);
    }

    /**
     * 剥离已知代理前缀，返回裸的 https://raw.githubusercontent.com/... URL
     */
    public static String stripProxyPrefix(String url) {
        if (url == null) return "";
        if (url.startsWith("https://mirror.ghproxy.com/")) {
            return url.substring("https://mirror.ghproxy.com/".length());
        }
        if (url.startsWith("https://ghproxy.com/")) {
            return url.substring("https://ghproxy.com/".length());
        }
        if (url.startsWith("https://ghfast.top/")) {
            // ghfast.top 去掉了 https://，需要还原
            return "https://" + url.substring("https://ghfast.top/".length());
        }
        return url;
    }
}
