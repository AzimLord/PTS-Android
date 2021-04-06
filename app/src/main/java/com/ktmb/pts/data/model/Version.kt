package com.ktmb.pts.data.model


class Version(version: String) : Comparable<Version?> {
    private val version: String
    fun get(): String {
        return version
    }

    override fun compareTo(other: Version?): Int {
        if (other != null) {
            val thisParts = this.get().split("\\.".toRegex()).toTypedArray()
            val thatParts = other.get().split("\\.".toRegex()).toTypedArray()
            val length = thisParts.size.coerceAtLeast(thatParts.size)
            for (i in 0 until length) {
                val thisPart = if (i < thisParts.size) thisParts[i].toInt() else 0
                val thatPart = if (i < thatParts.size) thatParts[i].toInt() else 0
                if (thisPart < thatPart) return -1
                if (thisPart > thatPart) return 1
            }
        }
        return 0
    }

    override fun equals(that: Any?): Boolean {
        if (this === that) return true
        return if (that == null) false else this.javaClass == that.javaClass && this.compareTo(that as Version?) == 0
    }

    init {
        var trimmedVersion = version.replace("[^0-9?!\\.]".toRegex(), "")
        // replace all empty version number-parts with zeros
        trimmedVersion = trimmedVersion.replace("\\.(\\.|$)".toRegex(), "\\.0$1")
        if (!trimmedVersion.matches(Regex("[0-9]+(\\.[0-9]+)*"))) throw Exception("Invalid version format. Original: `$version` trimmed: `$trimmedVersion`")
        this.version = trimmedVersion
    }
}