def logFile = new File(basedir, 'build.log')
assert logFile.exists()

def errors = logFile.readLines('UTF-8').findAll { it.startsWith('[ERROR]') && it.contains('Undefined reference:') }

def expectedErrors = [
    'Main.java': 1,
    'ArrayParameters.java': 3,
    'ConstructorParameter.java': 1,
    'AbstractParameter.java': 1,
    'InterfaceParameter.java': 1,
    'UnsupportedLambdaBody.java': 2
]
expectedErrors.each { file, count ->
    assert errors.count { it.contains(file + ':') } == count : "Unexpected errors for ${file}: ${errors}"
}

assert errors.count { it.contains('Undefined reference: java.util.Base64.Encoder') } == 8
assert errors.count { it.contains('Undefined reference: java.util.Base64.Decoder') } == 1
assert errors.any { it.contains('Undefined reference: java.util.Base64.Encoder java.util.Base64.getEncoder()') }
assert errors.size() == 9

['SupportedParameters.java', 'SuppressedMethods.java', 'SuppressedClass.java',
 'SuppressedLambdaCapture.java', 'SuppressedSyntheticAccessor.java'].each { file ->
    assert !errors.any { it.contains(file + ':') } : "Unexpected error for ${file}"
}

return true
