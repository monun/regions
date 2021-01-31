package com.github.monun.regions.util

object ChunkCoordIntPair {
    fun pair(msw: Int, lsw: Int): Long {
        return (msw.toLong() shl 32) + lsw - Int.MIN_VALUE
    }

    fun msw(l: Long): Int {
        return (l shr 32).toInt()
    }

    fun lsw(l: Long): Int {
        return (l and 0xFFFFFFFFL).toInt() + Int.MIN_VALUE
    }
}