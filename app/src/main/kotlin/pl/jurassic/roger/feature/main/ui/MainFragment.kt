package pl.jurassic.roger.feature.main.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.transition.TransitionManager
import android.view.View.GONE
import android.view.View.VISIBLE
import pl.jurassic.roger.R
import pl.jurassic.roger.data.ui.ProgressAngles
import pl.jurassic.roger.feature.common.ui.BaseFragment
import pl.jurassic.roger.feature.main.MainFragmentContract.Presenter
import pl.jurassic.roger.feature.main.MainFragmentContract.View
import pl.jurassic.roger.util.timer.TimerService
import pl.jurassic.roger.util.timer.TimerServiceBinder
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_main.main_break_item_lunch as breakItemLunch
import kotlinx.android.synthetic.main.fragment_main.main_break_item_other as breakItemOther
import kotlinx.android.synthetic.main.fragment_main.main_break_item_smoking as breakItemSmoking
import kotlinx.android.synthetic.main.fragment_main.main_break_time_text as breakTimeTextView
import kotlinx.android.synthetic.main.fragment_main.main_job_time_text as jobTimeTextView
import kotlinx.android.synthetic.main.fragment_main.main_root as rootConstraint
import kotlinx.android.synthetic.main.fragment_main.main_save_button as saveButton
import kotlinx.android.synthetic.main.fragment_main.main_time_progress as timeProgressView
import kotlinx.android.synthetic.main.fragment_main.main_timer_button as timerImageView

class MainFragment : BaseFragment<Presenter>(), View {

    @Inject
    lateinit var timerServiceIntent: Intent

    override val layoutId: Int = R.layout.fragment_main

    override lateinit var timerService: TimerService

    override var timerServiceBound: Boolean = false
        set(value) = with(timerService) {
            field = value
            if (value) {
                timeUpdateCallback = { presenter.onJobTimeReceive(it) }
                breakUpdateCallback = { breakType, breakTime -> presenter.onBreakTimeReceive(breakType, breakTime) }
                onNotificationPauseClicked = { presenter.onNotificationPauseClicked() }
                onNotificationResumeClicked = { presenter.onNotificationResumeClicked() }
            } else {
                timeUpdateCallback = null
                breakUpdateCallback = null
                onNotificationPauseClicked = null
                onNotificationResumeClicked = null
            }
        }

    private val timerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerServiceBinder
            timerService = binder.service
            timerServiceBound = true
            presenter.onServiceConnect()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            timerServiceBound = false
        }
    }

    override fun initOnClickListeners() = with(presenter) {
        timerImageView.setOnClickListener { onTimerButtonClicked() }
        breakItemSmoking.setOnClickListener { onSmokingItemClicked(it.isSelected) }
        breakItemLunch.setOnClickListener { onLunchItemClicked(it.isSelected) }
        breakItemOther.setOnClickListener { onOtherItemClicked(it.isSelected) }
        saveButton.setOnClickListener { onSaveClicked() }
    }

    override fun onResume() {
        super.onResume()

        activity?.bindService(timerServiceIntent, timerServiceConnection, Context.BIND_AUTO_CREATE)
        activity?.startService(timerServiceIntent)
    }

    override fun onPause() {
        super.onPause()
        if (timerServiceBound) {
            activity?.unbindService(timerServiceConnection)
            timerServiceBound = false
        }
    }

    override fun setProgressAngles(progressAngles: ProgressAngles) {
        timeProgressView.setProgressAngle(progressAngles)
    }

    override fun setJobTime(jobTime: String) {
        jobTimeTextView.text = jobTime
    }

    override fun setBreakTotalTime(breakTime: String) {
        breakTimeTextView.text = breakTime
    }

    override fun activeSmokingButton() {
        breakItemSmoking.isSelected = true
    }

    override fun deactivateSmokingButton() {
        breakItemSmoking.isSelected = false
    }

    override fun activeLunchButton() {
        breakItemLunch.isSelected = true
    }

    override fun deactivateLunchButton() {
        breakItemLunch.isSelected = false
    }

    override fun activeOtherButton() {
        breakItemOther.isSelected = true
    }

    override fun deactivateOtherButton() {
        breakItemOther.isSelected = false
    }

    override fun deactivateJobButton() {
        timerImageView.isSelected = false
    }

    override fun activeJobButton() {
        timerImageView.isSelected = true
    }

    override fun setLunchTimeText(breakTime: String) {
        breakItemLunch.breakTimeText = breakTime
    }

    override fun setSmokingTimeText(breakTime: String) {
        breakItemSmoking.breakTimeText = breakTime
    }

    override fun setOtherTimeText(breakTime: String) {
        breakItemOther.breakTimeText = breakTime
    }

    override fun hideSaveButton() {
        saveButton.visibility = GONE
        TransitionManager.beginDelayedTransition(rootConstraint)
    }

    override fun showSaveButton() {
        saveButton.visibility = VISIBLE
        TransitionManager.beginDelayedTransition(rootConstraint)
    }
}