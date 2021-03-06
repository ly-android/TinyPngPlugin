package com.allen.tinypng

import com.tinify.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.lang.Exception
import java.security.MessageDigest
import java.text.DecimalFormat

/**
 * TingPng Task
 * @author liyong1
 */
class TinyPngTask extends DefaultTask {

    def android
    TinyPngExtension configuration
    def apiKeys = new ArrayList<String>()

    TinyPngTask() {
        description = 'Tiny Resources'
        group = 'tinyPng'
        outputs.upToDateWhen { false }
        android = project.extensions.android
        configuration = project.tinyInfo
    }

    static String formetFileSize(long fileS) {
        def df = new DecimalFormat("#.00")
        if (fileS == 0L) {
            return "0B"
        }

        if (fileS < 1024) {
            return df.format((double) fileS) + "B"
        } else if (fileS < 1048576) {
            return df.format((double) fileS / 1024) + "KB"
        } else if (fileS < 1073741824) {
            return df.format((double) fileS / 1048576) + "MB"
        } else {
            return df.format((double) fileS / 1073741824) + "GB"
        }
    }

    static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream() { is ->
            int read
            byte[] buffer = new byte[8192]
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] md5sum = digest.digest()
        BigInteger bigInt = new BigInteger(1, md5sum)
        return bigInt.toString(16).padLeft(32, '0')
    }

    String getFileKey(String filePath) {
        def key = filePath.replace(project.projectDir.parent, "")
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return key.replaceAll("\\\\", "/")
        }
        return key
    }

    TinyPngResult compress(File resDir, Iterable<String> whiteList, Iterable<TinyPngInfo> compressedList) {
        def newCompressedList = new ArrayList<TinyPngInfo>()
        def accountError = false
        def beforeTotalSize = 0
        def afterTotalSize = 0
        label:
        for (File file : resDir.listFiles()) {
            def filePath = file.path
            def fileName = file.name

            for (String s : whiteList) {
                if (fileName ==~ /$s/) {
                    println("match whit list, skip it >>>>>>>>>>>>> $filePath")
                    continue label
                }
            }

            for (TinyPngInfo info : compressedList) {
                def key = getFileKey(filePath)
                if (key == info.path && generateMD5(file) == info.md5) {
                    continue label
                }
            }

            if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                if (fileName.contains(".9")) {
                    continue
                }

                println("find target pic >>>>>>>>>>>>> $filePath")

                def fis = new FileInputStream(file)

                try {
                    def beforeSize = fis.available()
                    def beforeSizeStr = formetFileSize(beforeSize)

                    // Use the Tinify API client
                    def tSource = Tinify.fromFile("${resDir}/${fileName}")
                    tSource.toFile("${resDir}/${fileName}")

                    def afterSize = fis.available()
                    def afterSizeStr = formetFileSize(afterSize)

                    beforeTotalSize += beforeSize
                    afterTotalSize += afterSize
                    def key = getFileKey(filePath)
                    newCompressedList.add(new TinyPngInfo(key, beforeSizeStr, afterSizeStr,
                            generateMD5(file)))
                    println("beforeSize: $beforeSizeStr -> afterSize: ${afterSizeStr}")
                } catch (AccountException e) {
                    println("AccountException: ${e.getMessage()}")
                    accountError = true
                    break
                    // Verify your API key and account limit.
                } catch (ClientException e) {
                    // Check your source image and request options.
                    println("ClientException: ${e.getMessage()}")
                } catch (ServerException e) {
                    // Temporary issue with the Tinify API.
                    println("ServerException: ${e.getMessage()}")
                } catch (ConnectionException e) {
                    // A network connection error occurred.
                    println("ConnectionException: ${e.getMessage()}")
                } catch (IOException e) {
                    // Something else went wrong, unrelated to the Tinify API.
                    println("IOException: ${e.getMessage()}")
                } catch (Exception e) {
                    println("Exception: ${e.toString()}")
                }
            }
        }
        return new TinyPngResult(beforeTotalSize, afterTotalSize, accountError, newCompressedList)
    }

    private boolean validKey() {
        if (apiKeys.size() == 0) return false
        def apiKey = apiKeys.pop()
        try {
            Tinify.setKey(apiKey)
            Tinify.validate() //返回验证成功的
        } catch (Exception ignored) {
            println("Tiny Validation of API key failed.")
            ignored.printStackTrace()
            return validKey()
        }
        return true
    }

    @TaskAction
    def run() {
        println(configuration.toString())
        apiKeys.addAll(configuration.apiKeys)
        if (!(configuration.resourceDir ?: false)) {
            println("Not found resources list")
            return
        }
        if (!(configuration.apiKeys ?: false)) {
            println("Tiny API Key not set")
            return
        }

        if (!validKey()) {
            println "has no valid tiny api keys!"
            return
        }

        def compressedList = new ArrayList<TinyPngInfo>()
        def compressedListFile = new File("${project.projectDir}/compressed-resource.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        } else {
            try {
                def list = new JsonSlurper().parse(compressedListFile, "utf-8")
                if (list instanceof ArrayList) {
                    compressedList = list
                } else {
                    println("compressed-resource.json is invalid, ignore")
                }
            } catch (Exception ignored) {
                println("compressed-resource.json is invalid, ignore")
            }
        }

        def beforeSize = 0L
        def afterSize = 0L
        def error = false
        def newCompressedList = new ArrayList<TinyPngInfo>()
        configuration.resourceDir.each { d ->
            def dir = new File(project.rootDir, d)
            if (dir.exists() && dir.isDirectory()) {
                if (!(configuration.resourcePattern ?: false)) {
                    configuration.resourcePattern = ["drawable[a-z-]*"]
                }
                configuration.resourcePattern.each { p ->
                    dir.eachDirMatch(~/$p/) { drawDir ->
                        if (!error) {
                            TinyPngResult result = compress(drawDir, configuration.whiteList, compressedList)
                            beforeSize += result.beforeSize
                            afterSize += result.afterSize
                            error = result.error
                            if (result.getResults()) {
                                newCompressedList.addAll(result.getResults())
                            }
                        }
                    }
                }
            }
        }

        if (newCompressedList) {
            for (TinyPngInfo newTinyPng : newCompressedList) {
                def index = compressedList.path.indexOf(newTinyPng.path)
                if (index >= 0) {
                    compressedList[index] = newTinyPng
                } else {
                    compressedList.add(0, newTinyPng)
                }
            }
            def jsonOutput = new JsonOutput()
            def json = jsonOutput.toJson(compressedList)
            compressedListFile.write(jsonOutput.prettyPrint(json), "utf-8")
            println("Task finish, compress ${newCompressedList.size()} files, before total size: ${formetFileSize(beforeSize)} after total size: ${formetFileSize(afterSize)}")
        }
    }
}