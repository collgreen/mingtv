package com.github.tvbox.osc.util;

import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub 访问工具 —— 处理代理前缀、拼接 raw/发布地址
 *
 * 代理列表（由 ProxyTester 自动测速后选出，缓存到 HawkConfig.GITHUB_PROXY_IDX）：
 *   0 mirror.ghproxy.com（默认）
 *   1 ghproxy.com（备用）
 *   2 直连
 */
public class GitHubUtils {

    public static final String REPO    = "collgreen/mingtv";
    public static final String BRANCH  = "main";
    public static final String RAW_BASE = "https://raw.githubusercontent.com/";

    /** 内部代理前缀列表，按优先级排列，ProxyTester 会选出最快的 */
    static final String[] PROXY_PREFIXES = {
            "https://mirror.ghproxy.com/",
            "https://ghproxy.com/",
            "" // 直连
    };

    /**
     * 构造仓库某文件的裸 raw 地址（不含代理前缀）
     */
    public static String rawUrlForPath(String path) {
        return RAW_BASE + REPO + "/" + BRANCH + "/" + path;
    }

    /**
     * 返回对应所有代理变体的 URL 列表（按 PROXY_PREFIXES 顺序）
     */
    public static List<String> getAllProxiedUrls(String rawUrl) {
        List<String> result = new ArrayList<>();
        for (String prefix : PROXY_PREFIXES) {
            result.add(prefix + rawUrl);
        }
        return result;
    }

    /**
     * 对任意 GitHub URL 应用当前缓存的最佳代理前缀
     */
    public static String applyBestProxy(String githubUrl) {
        int idx = Hawk.get(HawkConfig.GITHUB_PROXY_IDX, 0);
        if (idx < 0 || idx >= PROXY_PREFIXES.length) idx = 0;
        return PROXY_PREFIXES[idx] + githubUrl;
    }

    /**
     * 判断某 URL 是否属于本仓库托管的内容
     */
    public static boolean isManagedUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.contains("raw.githubusercontent.com/" + REPO);
    }

    /**
     * 剥离已知代理前缀，返回裸的 raw.githubusercontent.com URL
     */
    public static String stripProxyPrefix(String url) {
        if (url == null) return "";
        for (int i = 0; i < PROXY_PREFIXES.length - 1; i++) { // 跳过空字符串（直连）
            if (url.startsWith(PROXY_PREFIXES[i])) {
                return url.substring(PROXY_PREFIXES[i].length());
            }
        }
        return url;
    }
}
