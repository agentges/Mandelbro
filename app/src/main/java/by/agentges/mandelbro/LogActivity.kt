package by.agentges.mandelbro

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.ComponentActivity

open class LogActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("tttt", "Activity OnCreate ${this.hashCode()}")
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("tttt", "Activity OnStart ${this.hashCode()}")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("tttt", "Activity OnRestart ${this.hashCode()}")
    }

    override fun onResume() {
        super.onResume()
        Log.d("tttt", "Activity OnResume ${this.hashCode()}")
    }

    override fun onPause() {
        super.onPause()
        Log.d("tttt", "Activity OnPause ${this.hashCode()}")
    }

    override fun onStop() {
        super.onStop()
        Log.d("tttt", "Activity OnStop ${this.hashCode()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("tttt", "Activity OnDestroy ${this.hashCode()}")
    }
}