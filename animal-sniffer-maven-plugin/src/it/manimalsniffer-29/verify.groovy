def logFile = new File( basedir, 'build.log' )
assert logFile.exists()

def buildLog = logFile.getText('UTF-8')

assert buildLog.contains("Bad.java:9: Undefined reference: void java.util.concurrent.DelayQueue.<init>()")
assert buildLog.contains("Bad.java:9: Undefined reference: java.util.concurrent.ScheduledFuture")
assert buildLog.contains("Bad.java:11: Undefined reference: boolean java.util.concurrent.ScheduledFuture.isCancelled()")
assert buildLog.contains("Bad.java:15: Undefined reference: java.util.concurrent.Callable java.util.concurrent.Executors.callable(Runnable)")
assert buildLog.contains("Bad.java:15: Undefined reference: Object java.util.concurrent.Callable.call()")

assert !buildLog.contains("Good1.java")
assert !buildLog.contains("Good2.java")