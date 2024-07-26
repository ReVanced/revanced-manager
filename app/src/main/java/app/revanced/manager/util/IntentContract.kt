package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

object IntentContract : ActivityResultContract<Intent, IntentContract.Result>() {
    override fun createIntent(context: Context, input: Intent) = input
    override fun parseResult(resultCode: Int, intent: Intent?) = Result(resultCode, intent)

    class Result(val code: Int, val intent: Intent?)
}