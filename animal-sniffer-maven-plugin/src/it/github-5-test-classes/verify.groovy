File log = new File(basedir, 'build.log')

assert log.text.contains( 'Main.java:34: Undefined reference: java.util.concurrent.ConcurrentHashMap' )
assert !log.text.contains( 'Undefined reference: void org.junit.Assert.assertTrue(boolean)' )