File log = new File(basedir, 'build.log')
assert log.exists()
assert log.text.contains( 'ManimalSniffer49.java:51: Covariant return type change detected: java.nio.Buffer java.nio.ByteBuffer.flip() has been changed to java.nio.ByteBuffer java.nio.ByteBuffer.flip()' )
assert log.text.contains( 'ManimalSniffer49.java:76: Covariant return type change detected: java.nio.Buffer java.nio.ByteBuffer.flip() has been changed to java.nio.ByteBuffer java.nio.ByteBuffer.flip()' )
