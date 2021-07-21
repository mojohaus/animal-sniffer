File log = new File(basedir, 'build.log')
assert log.exists()
assert log.text.contains( 'ManimalSniffer49.java:51: Undefined reference: java.nio.ByteBuffer java.nio.ByteBuffer.flip()' )
assert log.text.contains( 'ManimalSniffer49.java:76: Undefined reference: java.nio.ByteBuffer java.nio.ByteBuffer.flip()' )
