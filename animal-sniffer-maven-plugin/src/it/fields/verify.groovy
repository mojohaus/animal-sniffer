def logFile = new File( basedir, 'build.log' )
assert logFile.exists()

def buildLog = logFile.getText('UTF-8')

assert buildLog.contains('Test.java: Field f: Undefined reference: java.util.Optional')
assert buildLog.contains('Test.java:11: Undefined reference: java.util.Optional')

assert !(buildLog =~ /TestSuppressedClass.*Undefined reference/)

assert buildLog.contains('TestSuppressedField.java: Field f1: Undefined reference: java.util.Optional')
assert !buildLog.contains('Field f2')
assert buildLog.contains('TestSuppressedField.java: Field f3: Undefined reference: java.util.Optional')
assert buildLog.contains('TestSuppressedField.java:15: Undefined reference: java.util.Optional')

assert buildLog.contains('TestWithNested.java: Field f: Undefined reference: java.util.Optional')
assert buildLog.contains('TestWithNested.java:9: Undefined reference: java.util.Optional')
assert buildLog.contains('TestWithNested.java: Field TestWithNested$Nested.f: Undefined reference: java.util.Optional')
assert buildLog.contains('TestWithNested.java:16: Undefined reference: java.util.Optional')

assert !(buildLog =~ /TestWithNestedSuppressedClass.*Undefined reference/)
