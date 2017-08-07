File log = new File(basedir, 'build.log')
assert log.exists()
assert log.text.contains( '[DEBUG] Ignoring module-info.class' )