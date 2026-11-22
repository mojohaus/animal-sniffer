def logFile = new File( basedir, 'build.log' )
assert logFile.exists()

def buildLog = logFile.getText('UTF-8')

assert buildLog.contains("Bad.java:9: Undefined reference: void java.util.concurrent.DelayQueue.<init>()")
assert buildLog.contains("Bad.java:9: Undefined reference: java.util.concurrent.ScheduledFuture")
assert buildLog.contains("Bad.java:11: Undefined reference: boolean java.util.concurrent.ScheduledFuture.isCancelled()")
assert buildLog.contains("Bad.java:15: Undefined reference: java.util.concurrent.Callable java.util.concurrent.Executors.callable(Runnable)")
assert buildLog.contains("Bad.java:15: Undefined reference: Object java.util.concurrent.Callable.call()")

// The debug log lists every compiled source file, so check the violations, not the whole log
def undefinedReferences = buildLog.readLines().findAll { it.contains("Undefined reference") }
for (line in undefinedReferences) {
    assert line.contains("Bad.java:") : "Only Bad.java should have violations: " + line
}