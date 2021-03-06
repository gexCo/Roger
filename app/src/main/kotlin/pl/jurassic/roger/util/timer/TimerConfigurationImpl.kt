package pl.jurassic.roger.util.timer

import org.joda.time.DateTime
import pl.jurassic.roger.data.BreakTime
import pl.jurassic.roger.data.BreakType

class TimerConfigurationImpl : TimerConfiguration {

    override var isRunning: Boolean = false

    override var activeBreakType: BreakType? = null

    override var totalJobTimeThatPass: Long = 0L

    override var timeThatPass: Long = 0L //TODO remove and write other logic

    override var startJobTime: DateTime = DateTime.now()

    override var breakTimesList: ArrayList<BreakTime> = arrayListOf()
}