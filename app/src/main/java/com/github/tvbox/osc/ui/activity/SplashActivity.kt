package com.github.tvbox.osc.ui.activity

import android.content.Intent
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.databinding.ActivitySplashBinding
import com.github.tvbox.osc.util.GitHubUtils
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.ProxyTester
import com.orhanobut.hawk.Hawk

class SplashActivity : BaseVbActivity<ActivitySplashBinding>() {
    override fun init() {
        App.getInstance().isNormalStart = true

        ProxyTester.testAndCache {
            updateManagedUrls()
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    /** 将 Hawk 里本仓库托管的 URL 更新为当前最佳代理 */
    private fun updateManagedUrls() {
        val apiUrl = Hawk.get(HawkConfig.API_URL, "")
        if (GitHubUtils.isManagedUrl(apiUrl)) {
            Hawk.put(HawkConfig.API_URL, GitHubUtils.applyBestProxy(GitHubUtils.stripProxyPrefix(apiUrl)))
        }
        val liveUrl = Hawk.get(HawkConfig.LIVE_URL, "")
        if (GitHubUtils.isManagedUrl(liveUrl)) {
            Hawk.put(HawkConfig.LIVE_URL, GitHubUtils.applyBestProxy(GitHubUtils.stripProxyPrefix(liveUrl)))
        }
    }
}
