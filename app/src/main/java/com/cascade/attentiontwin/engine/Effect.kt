package com.cascade.attentiontwin.engine

import java.util.Random

object Effect {

    data class EffectRow(
        val held: Boolean,
        val e: Double,      // propensity_hold
        val y: Double       // phoneMin10m
    )

    data class CausalEstimate(
        val eShow: Double,
        val eHold: Double,
        val harm: Double,
        val ciLow: Double,
        val ciHigh: Double,
        val nShow: Int,
        val nHold: Int,
        val isSufficient: Boolean
    )

    fun ipw(rows: List<EffectRow>): Pair<Double, Double> {
        val show = rows.filter { !it.held }
        val hold = rows.filter { it.held }

        if (show.isEmpty() || hold.isEmpty()) return 0.0 to 0.0

        val weightShowSum = show.sumOf { 1.0 / (1.0 - it.e.coerceIn(0.01, 0.99)) }
        val eShow = if (weightShowSum > 0.0) {
            show.sumOf { (it.y / (1.0 - it.e.coerceIn(0.01, 0.99))) } / weightShowSum
        } else 0.0

        val weightHoldSum = hold.sumOf { 1.0 / it.e.coerceIn(0.01, 0.99) }
        val eHold = if (weightHoldSum > 0.0) {
            hold.sumOf { (it.y / it.e.coerceIn(0.01, 0.99)) } / weightHoldSum
        } else 0.0

        return eShow to eHold
    }

    fun estimate(rows: List<EffectRow>): CausalEstimate {
        val nShow = rows.count { !it.held }
        val nHold = rows.count { it.held }
        val isSufficient = nShow >= 30 && nHold >= 30

        if (!isSufficient || rows.isEmpty()) {
            val (eShow, eHold) = ipw(rows)
            return CausalEstimate(
                eShow = eShow,
                eHold = eHold,
                harm = eShow - eHold,
                ciLow = 0.0,
                ciHigh = 0.0,
                nShow = nShow,
                nHold = nHold,
                isSufficient = false
            )
        }

        val (eShow, eHold) = ipw(rows)
        val harm = eShow - eHold

        // 400 bootstrap resamples
        val rnd = Random(42)
        val n = rows.size
        val bootHarms = DoubleArray(400)

        for (b in 0 until 400) {
            val sample = ArrayList<EffectRow>(n)
            repeat(n) {
                sample.add(rows[rnd.nextInt(n)])
            }
            val (bShow, bHold) = ipw(sample)
            bootHarms[b] = bShow - bHold
        }

        bootHarms.sort()
        val ciLow = bootHarms[(400 * 0.025).toInt().coerceIn(0, 399)]
        val ciHigh = bootHarms[(400 * 0.975).toInt().coerceIn(0, 399)]

        return CausalEstimate(
            eShow = eShow,
            eHold = eHold,
            harm = harm,
            ciLow = ciLow,
            ciHigh = ciHigh,
            nShow = nShow,
            nHold = nHold,
            isSufficient = true
        )
    }
}
