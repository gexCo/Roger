package pl.jurassic.roger.feature.main.presentation

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import org.joda.time.DateTime
import pl.jurassic.roger.data.WorkTime
import pl.jurassic.roger.data.ui.BreakProgressAngle
import pl.jurassic.roger.data.ui.ProgressAngles
import pl.jurassic.roger.feature.main.MainFragmentContract.Presenter
import pl.jurassic.roger.feature.main.MainFragmentContract.Router
import pl.jurassic.roger.feature.main.MainFragmentContract.View
import pl.jurassic.roger.sumByLong
import pl.jurassic.roger.util.repository.Repository
import pl.jurassic.roger.util.timer.BreakType
import pl.jurassic.roger.util.tools.DateFormatter
import timber.log.Timber

class MainFragmentPresenter(
    private val view: View,
    private val router: Router,
    private val dateFormatter: DateFormatter,
    private val repository: Repository,
    private val jobTimeSubject: Subject<Long>,
    private val breakTimeSubject: Subject<Long>,
    private val compositeDisposable: CompositeDisposable
) : Presenter {

    companion object {
        private const val FULL_ANGLE_DEGREE = 360f
        private const val MINUTS_IN_HOUR = 60f
        private const val SECONDS_IN_MINUTS = 60f
        private const val MILLISECONDS_IN_SECONDS = 1000f
        private const val WORK_TIME = 8f //todo take from share-prefs
    }

    private val configuration by lazy { view.timerService.configuration }

    override fun initialize() {
        compositeDisposable.add(
            Observable.combineLatest(
                jobTimeSubject.map { countProgressAngle(it) },
                breakTimeSubject.map { transformToProgressAngleList(it) },
                BiFunction<Float, List<BreakProgressAngle>, ProgressAngles> { t1, t2 -> ProgressAngles(t1, t2) }
            )
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ view.setProgressAngles(it) }, { Timber.e(it) })
        )
    }

    override fun clear() {
        compositeDisposable.clear()
    }

    override fun onServiceConnect() {
        breakTimeSubject.onNext(0) //Todo refactor
    }

    override fun onTimerButtonClicked() {
        when (configuration.isRunning) {
            true -> pauseTimer()
            false -> startTimer()
        }
    }

    private fun startTimer() = with(view) {
        timerService.startJobTimer()
        activeJobButton()
        hideSaveButton()
    }

    private fun pauseTimer() = with(view) {
        timerService.pauseJobTimer()
        pauseBreakTimer()
        deactivateAllButtons()
        showSaveButton()
    }

    private fun deactivateAllButtons() = with(view) {
        deactivateJobButton()
        deactivateSmokingButton()
        deactivateLunchButton()
        deactivateOtherButton()
    }

    override fun onJobTimeReceive(time: Long) {
        jobTimeSubject.onNext(time)
        view.setJobTime(dateFormatter.parseTime(time))
    }

    private fun countProgressAngle(time: Long) =
        time * FULL_ANGLE_DEGREE / (WORK_TIME * MINUTS_IN_HOUR * MILLISECONDS_IN_SECONDS) //todo add SECONDS_IN_MINUTS

    override fun onBreakTimeReceive(breakType: BreakType, time: Long) {
        val breakTime = dateFormatter.parseTime(getBreakTime(breakType) + time)
        when (breakType) {
            BreakType.LUNCH -> view.setLunchTimeText(breakTime)
            BreakType.SMOKING -> view.setSmokingTimeText(breakTime)
            BreakType.OTHER -> view.setOtherTimeText(breakTime)
        }
        breakTimeSubject.onNext(time)
        view.setBreakTotalTime(dateFormatter.parseTime(getBreakTotalTime() + time))
    }

    private fun getBreakTotalTime(): Long =
        configuration.breakTimesList.sumByLong { (it.stopTimestamp - it.startTimestamp) }

    private fun getBreakTime(breakType: BreakType): Long =
        configuration.breakTimesList
            .filter { it.breakType == breakType }
            .sumByLong { (it.stopTimestamp - it.startTimestamp) }

    private fun transformToProgressAngleList(time: Long): List<BreakProgressAngle> =
        configuration.breakTimesList
            .map {
                val startAngle = countProgressAngle(it.jobTimeThatPass)
                val sweepAngle = countProgressAngle(it.stopTimestamp - it.startTimestamp)
                BreakProgressAngle(startAngle, sweepAngle, it.breakType.breakColorRes)
            }
            .also { if (it.isNotEmpty()) it.last().sweepAngle += countProgressAngle(time) }

    override fun onSmokingItemClicked(isSelected: Boolean) = with(view) {
        when (isSelected) {
            true -> {
                pauseBreakTimer()
                deactivateSmokingButton()
            }
            false -> {
                startBreakTimer(BreakType.SMOKING)
                startTimer()

                deactivateOtherButton()
                deactivateLunchButton()
                activeSmokingButton()
            }
        }
    }

    private fun pauseBreakTimer() = with(view) {
        activeBreakType?.let { timerService.pauseBreakTimer() }
        activeBreakType = null
    }

    private fun startBreakTimer(breakType: BreakType) = with(view) {
        activeBreakType?.let { timerService.pauseBreakTimer() }
        activeBreakType = breakType
        timerService.startBreakTimer(breakType)
    }

    override fun onLunchItemClicked(isSelected: Boolean) = with(view) {
        when (isSelected) {
            true -> {
                pauseBreakTimer()
                deactivateLunchButton()
            }
            false -> {
                startBreakTimer(BreakType.LUNCH)
                startTimer()

                deactivateOtherButton()
                deactivateSmokingButton()
                activeLunchButton()
            }
        }
    }

    override fun onOtherItemClicked(isSelected: Boolean) = with(view) {
        when (isSelected) {
            true -> {
                pauseBreakTimer()
                deactivateOtherButton()
            }
            false -> {
                startBreakTimer(BreakType.OTHER)
                startTimer()

                deactivateSmokingButton()
                deactivateLunchButton()
                activeOtherButton()
            }
        }
    }

    override fun onSaveClicked() = with(configuration) {
        val workTime = WorkTime(configuration.totalJobTimeThatPass, breakTimesList, DateTime.now())
        repository.saveWorkTime(workTime)

        router.navigateToSummaryScreen()
    }
}