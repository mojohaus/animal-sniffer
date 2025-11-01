package test

/**
 * Test case for Kotlin inline functions with @IgnoreJRERequirement annotation.
 * This tests both inline and non-inline methods to ensure the annotation works correctly.
 */

class ThreadUtils {
    // Inline method that uses a restricted API
    @org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
    inline fun getThreadIdInline(thread: Thread): Long {
        @Suppress("DEPRECATION")
        return thread.id  // Thread.getId() is deprecated
    }

    // Non-inline method that uses a restricted API
    @org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
    fun getThreadIdNonInline(thread: Thread): Long {
        @Suppress("DEPRECATION")
        return thread.id  // Thread.getId() is deprecated
    }

    // Method that calls the inline function
    fun useInlineMethod() {
        val thread = Thread.currentThread()
        val id = getThreadIdInline(thread)
    }

    // Method that calls the non-inline function
    fun useNonInlineMethod() {
        val thread = Thread.currentThread()
        val id = getThreadIdNonInline(thread)
    }
}
