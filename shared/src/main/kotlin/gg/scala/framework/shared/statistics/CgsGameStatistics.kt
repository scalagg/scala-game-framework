package gg.scala.framework.shared.statistics

/**
 * @author Limo
 * @date 6/17/2025
 */
class CgsGameStatistics
{
    var value = 0

    operator fun inc() : CgsGameStatistics
    {
        value++
        return this
    }

    operator fun dec() : CgsGameStatistics
    {
        value--
        return this
    }

    fun update(new: Int)
    {
        value = new
    }

    fun reset()
    {
        value = 0
    }
}