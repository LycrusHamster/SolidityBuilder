import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class PluginTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDirRoot = new TemporaryFolder()

    File testProjectDir

    def setup() {

        //testProjectDir = new File("/home/lycrus/IdeaProjects/SolidityBuilder/pluginTest")
        //testProjectDir = new File(testProjectDirRoot.root, 'pluginTest')
        if(testProjectDir == null){
            println 'specify testProjectDir in src/test/groovy/me/lycrus/PluginTest.groovy'
        }
        def testURL = PluginTest.class.getResource("pluginTest/build.gradle")

        /*if(false){
            throw new RuntimeException("test resource not found")
        }
        if(testURL.equals(null)){
            throw new RuntimeException("test resource not found")
        }*/

    }

    def doFirst() {
        given:
        if (testProjectDir.exists()) FileUtils.cleanDirectory(testProjectDir)
        FileUtils.forceMkdir(testProjectDir)
        String pluginTestDir = PluginTest.class.getResource("pluginTest").getFile();
        FileUtils.copyDirectoryToDirectory(new File(pluginTestDir), testProjectDir.getParentFile())

        when:
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withDebug(true).
                withArguments("olivia");
        BuildResult res = runner.build();
        then:
        println res.output
    }

    def doSecond() {
        given:
        String pluginTestDirStep2 = PluginTest.class.getResource("step2").getFile();
        FileUtils.copyDirectoryToDirectory(new File(pluginTestDirStep2,"solidity"), new File(testProjectDir,"src/main"))
        FileUtils.copyFileToDirectory(new File(pluginTestDirStep2,"build.gradle"),testProjectDir);
        FileUtils.forceDelete(new File(testProjectDir,"src/main/solidity/contracts/pg2/D.sol"))
        when:
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withDebug(true).
                withArguments("olivia");
        BuildResult res = runner.build();
        then:
        println res.output
    }

    def doThird() {
        given:
        String pluginTestDirStep2 = PluginTest.class.getResource("step3").getFile();
        FileUtils.copyFileToDirectory(new File(pluginTestDirStep2,"build.gradle"),testProjectDir);
        when:
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withDebug(true).
                withArguments("olivia");
        BuildResult res = runner.build();
        then:
        println res.output
    }
}