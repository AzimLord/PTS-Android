package com.ktmb.pts.base

import android.app.Dialog
import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ktmb.pts.animation.RevealAnimation
import com.ktmb.pts.utilities.px
import kotlinx.android.synthetic.main.view_toolbar.*

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var revealAnimation: RevealAnimation
    protected var dialog: Dialog? = null

    override fun onStart() {
        super.onStart()
        setToolbar()
    }

    protected open fun setToolbar() {
        setSupportActionBar(v_toolbar)
        supportActionBar?.run {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            elevation = 4.px.toFloat()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (::revealAnimation.isInitialized) {
                    revealAnimation.unRevealActivity()
                } else {
                    finish()
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    open fun onRetryButtonClick(view: View) {
        // Do nothing
    }

    protected fun startRevealActivity(root: View, intent: Intent, requestCode: Int? = null) {
        //calculates the center of the View v you are passing
        val revealX = (root.x + root.width / 2).toInt()
        val revealY = (root.y + root.height / 2).toInt()

        //create an intent, that launches the second activity and pass the x and y coordinates
        intent.putExtra(RevealAnimation.EXTRA_CIRCULAR_REVEAL_X, revealX)
        intent.putExtra(RevealAnimation.EXTRA_CIRCULAR_REVEAL_Y, revealY)

        //just start the activity as an shared transition, but set the options bundle to null
        if (requestCode != null) {
            ActivityCompat.startActivityForResult(this, intent, requestCode, null)
        } else {
            ActivityCompat.startActivity(this, intent, null)
        }

        //to prevent strange behaviours override the pending transitions
        overridePendingTransition(0, 0)
    }
}