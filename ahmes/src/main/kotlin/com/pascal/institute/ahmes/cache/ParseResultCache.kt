package com.pascal.institute.ahmes.cache

import com.pascal.institute.ahmes.model.SecReportParseResult
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * LRU Cache for parsed SEC documents.
 *
 * Caches parse results to avoid re-parsing the same document multiple times. Uses LRU (Least
 * Recently Used) eviction policy with configurable size and TTL.
 *
 * ## Features
 *
 * - **Thread-safe**: Uses ConcurrentHashMap internally
 * - **LRU eviction**: Automatically removes least recently used entries
 * - **TTL support**: Entries expire after configured time
 * - **Size limits**: Prevents memory bloat
 * - **Statistics**: Track hits, misses, evictions
 *
 * ## Usage
 *
 * ```kotlin
 * val cache = ParseResultCache(
 *     maxSize = 100,
 *     ttl = Duration.ofHours(1)
 * )
 *
 * // Try to get from cache
 * val result = cache.get(documentKey) ?: run {
 *     // Cache miss - parse document
 *     val parsed = parser.parse(document)
 *     cache.put(documentKey, parsed)
 *     parsed
 * }
 * ```
 *
 * @param maxSize Maximum number of entries to cache
 * @param ttl Time-to-live for cached entries
 */
class ParseResultCache(
        private val maxSize: Int = 100,
        private val ttl: Duration = Duration.ofHours(1)
) {

    private inner class CacheEntry(
            val result: SecReportParseResult,
            val timestamp: Instant = Instant.now(),
            var accessCount: Long = 0
    ) {
        val age: Duration
            get() = Duration.between(timestamp, Instant.now())

        val isExpired: Boolean
            get() = age > ttl

        fun markAccessed() {
            accessCount++
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val stats = CacheStatistics()

    /**
     * Get a cached parse result.
     *
     * @param key Cache key (typically accession number or file hash)
     * @return Cached result or null if not found/expired
     */
    fun get(key: String): SecReportParseResult? {
        val entry = cache[key]

        return when {
            entry == null -> {
                stats.recordMiss()
                null
            }
            entry.isExpired -> {
                cache.remove(key)
                stats.recordMiss()
                stats.recordEviction()
                null
            }
            else -> {
                entry.markAccessed()
                stats.recordHit()
                entry.result
            }
        }
    }

    /**
     * Put a parse result in the cache.
     *
     * If cache is full, removes the least recently used entry.
     *
     * @param key Cache key
     * @param result Parse result to cache
     */
    fun put(key: String, result: SecReportParseResult) {
        // Check if we need to evict
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            evictLRU()
        }

        cache[key] = CacheEntry(result)
    }

    /** Remove an entry from the cache. */
    fun remove(key: String): SecReportParseResult? {
        val entry = cache.remove(key)
        if (entry != null) {
            stats.recordEviction()
        }
        return entry?.result
    }

    /** Clear all entries from the cache. */
    fun clear() {
        val size = cache.size
        cache.clear()
        stats.recordEviction(size)
    }

    /** Evict the least recently used entry. */
    private fun evictLRU() {
        // Find entry with lowest access count and oldest timestamp
        val lruKey =
                cache.entries
                        .minByOrNull { (_, entry) ->
                            // Combine access count and age for LRU decision
                            // Use access count as primary, timestamp as secondary
                            entry.accessCount * 1000000 + entry.timestamp.toEpochMilli()
                        }
                        ?.key

        if (lruKey != null) {
            cache.remove(lruKey)
            stats.recordEviction()
        }
    }

    /**
     * Remove all expired entries.
     *
     * @return Number of entries removed
     */
    fun cleanupExpired(): Int {
        val expired = cache.filterValues { it.isExpired }
        expired.keys.forEach { cache.remove(it) }
        stats.recordEviction(expired.size)
        return expired.size
    }

    /** Get current cache size. */
    val size: Int
        get() = cache.size

    /** Get cache statistics. */
    fun getStatistics(): CacheStatistics = stats.copy()

    /** Reset statistics. */
    fun resetStatistics() {
        stats.reset()
    }
}

/** Cache statistics for monitoring performance. */
data class CacheStatistics(var hits: Long = 0, var misses: Long = 0, var evictions: Long = 0) {
    /** Hit rate (0.0 to 1.0). */
    val hitRate: Double
        get() {
            val total = hits + misses
            return if (total > 0) hits.toDouble() / total else 0.0
        }

    /** Total requests. */
    val totalRequests: Long
        get() = hits + misses

    internal fun recordHit() {
        hits++
    }

    internal fun recordMiss() {
        misses++
    }

    internal fun recordEviction(count: Int = 1) {
        evictions += count
    }

    internal fun reset() {
        hits = 0
        misses = 0
        evictions = 0
    }

    override fun toString(): String {
        return "CacheStats(hits=$hits, misses=$misses, evictions=$evictions, hitRate=${String.format("%.2f%%", hitRate * 100)})"
    }
}

/** Builder for ParseResultCache with fluent API. */
class ParseResultCacheBuilder {
    private var maxSize: Int = 100
    private var ttl: Duration = Duration.ofHours(1)

    fun maxSize(size: Int) = apply { this.maxSize = size }
    fun ttl(duration: Duration) = apply { this.ttl = duration }
    fun ttlMinutes(minutes: Long) = apply { this.ttl = Duration.ofMinutes(minutes) }
    fun ttlHours(hours: Long) = apply { this.ttl = Duration.ofHours(hours) }

    fun build() = ParseResultCache(maxSize, ttl)
}

/** Create a ParseResultCache with builder pattern. */
fun parseResultCache(configure: ParseResultCacheBuilder.() -> Unit): ParseResultCache {
    return ParseResultCacheBuilder().apply(configure).build()
}
