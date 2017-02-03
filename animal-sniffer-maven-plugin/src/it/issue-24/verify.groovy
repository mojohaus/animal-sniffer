File log = new File(basedir, 'build.log')
assert log.exists()
assert log.text.contains( 'For artifact {org.codehaus.mojo.signature:java14:null:signature}: The version cannot be empty.' )
