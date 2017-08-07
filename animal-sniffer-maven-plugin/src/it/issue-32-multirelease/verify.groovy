File log = new File(basedir, 'build.log')
assert log.exists()
assert log.text.contains( '[DEBUG] Ignoring META-INF/versions/9/org/jboss/marshalling/reflect/JDKSpecific.class')
assert log.text.contains( '[DEBUG] Ignoring META-INF/versions/9/org/jboss/marshalling/reflect/JDKSpecific$1.class' )
