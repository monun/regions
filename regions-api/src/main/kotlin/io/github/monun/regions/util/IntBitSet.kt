/*
 * Copyright (c) 2020 monun
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.monun.regions.util


interface Indexable {
    val offset: Int

    val raw: Int
        get() = 1 shl offset
}

val Iterable<Indexable>.raws: Int
    get() {
        if (this is IntBitSet<Indexable>)
            return rawElements

        var raws = 0

        forEach {
            raws = raws or it.raw
        }

        return raws
    }

open class IntBitSet<E : Indexable>(rawElements: Int = 0, private val converter: (offset: Int) -> E?) : MutableSet<E>,
    Cloneable {
    var rawElements = rawElements
        private set

    override fun add(element: E): Boolean {
        val rawElements = this.rawElements
        val raw = element.raw

        if (rawElements and raw == raw) return false

        this.rawElements = rawElements or raw
        return true
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val rawElements = this.rawElements
        val raws = elements.raws

        if (rawElements and raws == raws) return false

        this.rawElements = rawElements or raws
        return true
    }

    override fun clear() {
        rawElements = 0
    }

    override fun iterator(): MutableIterator<E> {
        return IntBitSetIterator()
    }

    private inner class IntBitSetIterator : MutableIterator<E> {
        private var offset = 0
        private var next: E? = null

        init {
            prepareNext()
        }

        private fun prepareNext() {
            val rawElements = rawElements
            val converter = converter
            var offset = this.offset
            var next: E? = null

            do {
                val raw = 1 shl offset

                if ((rawElements and raw) == raw) {
                    next = converter(offset)
                }

                offset++
            } while (next == null && (rawElements ushr offset) != 0)

            this.offset = offset
            this.next = next
        }

        override fun hasNext(): Boolean {
            return next != null
        }

        override fun next(): E {
            return next?.also {
                prepareNext()
            } ?: throw NoSuchElementException()
        }

        override fun remove() {
            next?.also {
                rawElements = rawElements and it.raw.inv()
                prepareNext()
            } ?: throw NoSuchElementException()
        }
    }

    override fun remove(element: E): Boolean {
        val rawElement = this.rawElements
        val raw = element.raw

        if (rawElement and raw != raw) return false

        this.rawElements = rawElements and raw.inv()
        return true
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val rawElements = this.rawElements
        val raws = elements.raws

        if (rawElements and raws == 0) return false

        this.rawElements = rawElements and raws.inv()
        return true
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        val rawElements = this.rawElements
        val raws = elements.raws

        if (rawElements and raws.inv() != 0) return false

        this.rawElements = rawElements and raws
        return true
    }

    override val size: Int
        get() {
            var rawElements = this.rawElements
            var count = 0

            while (rawElements > 0) {
                if (rawElements and 1 == 1)
                    count++
                rawElements = rawElements ushr 1
            }

            return count
        }

    override fun contains(element: E): Boolean {
        val raw = element.raw

        return rawElements and raw == raw
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        val raws = elements.raws

        return rawElements and raws == raws
    }

    override fun isEmpty(): Boolean {
        return rawElements == 0
    }

    fun or(other: IntBitSet<E>) {
        rawElements = rawElements or other.rawElements
    }

    fun and(other: IntBitSet<E>) {
        rawElements = rawElements and other.rawElements
    }

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): IntBitSet<E> {
        return super.clone() as IntBitSet<E>
    }

    override fun toString(): String {
        return joinToString(prefix = "[", postfix = "]", separator = ",", transform = { it.toString() })
    }
}