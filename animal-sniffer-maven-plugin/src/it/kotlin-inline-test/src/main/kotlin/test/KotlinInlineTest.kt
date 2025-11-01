package test

/**
 * Test case for Kotlin inline functions with @IgnoreJRERequirement annotation.
 * This reproduces the issue where @IgnoreJRERequirement doesn't work on Kotlin inline properties.
 */

// Test with inline property (should be ignored when annotated)
@org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
inline val Thread.threadIdCompat: Long
    get() {
        @Suppress("DEPRECATION")
        return id  // This should be ignored due to the annotation
    }

// Test with non-inline property (should also work)
@org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
val Thread.threadIdNonInline: Long
    get() {
        @Suppress("DEPRECATION")
        return id  // This should be ignored due to the annotation
    }

// Caller function that uses the inline property
fun useThreadId() {
    val thread = Thread.currentThread()
    val id1 = thread.threadIdCompat
    val id2 = thread.threadIdNonInline
}
