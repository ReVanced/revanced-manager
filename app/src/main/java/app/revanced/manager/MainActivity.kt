package app.revanced.manager

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import app.revanced.manager.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application);
        DynamicColors.applyIfAvailable(this);
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        supportFragmentManager.beginTransaction().replace(androidx.appcompat.R.id.content, Logs()).commit()
        val navView: BottomNavigationView = binding.navView
//        val sussy: ActionBar? = supportActionBar
//        if (sussy != null) {
//            sussy.setDisplayHomeAsUpEnabled(true)
//        }
        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)


        }
    }