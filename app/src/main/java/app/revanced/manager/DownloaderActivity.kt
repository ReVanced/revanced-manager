package app.revanced.manager

import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import app.revanced.manager.domain.repository.DownloaderRepository
import org.koin.android.ext.android.inject

class DownloaderActivity : FragmentActivity() {
    private val downloaderRepository: DownloaderRepository by inject()
    private var downloaderPackageName = ""

    private val downloaderPkgState
        get() = downloaderRepository.findPackageByName(downloaderPackageName)

    private var res: Resources? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = FragmentContainerView(this).apply {
            // The fragment manager requires an ID to work.
            id = R.id.fragment_container
        }
        setContentView(
            view,
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )

        downloaderPackageName = intent.getStringExtra("DOWNLOADER_NAME").orEmpty()
        val fragmentClassName = intent.getStringExtra("FRAGMENT_CLASS_NAME")!!
        val args = intent.getBundleExtra("FRAGMENT_ARGS")

        // See DownloaderRepository.ResourceImpl.
        res = downloaderPkgState?.resourceImpl?.apply(super.resources)

        if (savedInstanceState == null) {
            @Suppress("UNCHECKED_CAST")
            val fragmentClass = classLoader!!.loadClass(fragmentClassName) as Class<Fragment>

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragment_container, fragmentClass, args)
            }
        }
    }

    override fun getClassLoader(): ClassLoader? =
        downloaderPkgState?.classLoader ?: super.classLoader

    override fun getResources(): Resources? = res ?: super.resources
}