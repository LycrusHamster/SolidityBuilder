//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.lycrus;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Closure;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Setter
public class SolidityBuilderExtension {
    public static final String DSL_NAME = "solidity";
    public Project project;

    public SourceDirectorySetFactory sourceDirectorySetFactory;

    //this for public
    //public SourceDirectorySet sourceDirectorySet;
    //this for internal use
    //public SourceDirectorySet truffleContractSourceDirectorySet;

    //for solc
    public int optimizer = -1;
    public String version = "provided";

    //for customer to save compiled contracts
    public Path solidityOutputDir = null;

    //for wrapper
    public String defaultPackageName = "solidity";
    public String generatedPackageName = null;
    public boolean overwriteWrapper = true;
    public Set<String> excludedContracts = new HashSet<>();
    public boolean useNativeJavaTypes = true;
    public Path wrapperOutputDir;


    //for source
    public Map<String, ConfigurableFileTree> sourceFileTrees = new LinkedHashMap<>();//your prefix -> CFT(include its baseDir)
    //for dependency
    public Map<Path, RemappingPackageRule> remappings = new LinkedHashMap<>(); //absolutePath -> {prefix + packageName + absolutePath}
    public Set<Path> libraries = new LinkedHashSet<>(); // it should be absolute path , we will scan all sub paths
    public Path npm;
    public Path ethpm;
    public Path lib;


    //for internal use;
    //to store built solidity in truffle-contract format
    public Path truffleJsonDir;

    //for @InputFiles
    public FileCollection fileCollection;

    @Inject
    public SolidityBuilderExtension(Project project, SourceDirectorySetFactory sourceDirectorySetFactory) {
        this.project = project;
        this.fileCollection = project.files();
        this.sourceDirectorySetFactory = sourceDirectorySetFactory;
        //this.sourceDirectorySet = sourceDirectorySetFactory.create("solidity files");

        //default remappings for npm and ethpm
        npm = Paths.get(project.getProjectDir().toString(), "src/main/solidity/node_modules/").toAbsolutePath();
        ethpm = Paths.get(project.getProjectDir().toString(), "src/main/solidity/installed_contracts/").toAbsolutePath();
        lib = Paths.get(project.getProjectDir().toString(), "src/main/solidity/libraries/").toAbsolutePath();
        //default value
        truffleJsonDir = new File(project.getBuildDir(), "/solidity").toPath().normalize().toAbsolutePath();
        wrapperOutputDir = Paths.get(project.getProjectDir().toString(), "src/main/java/").toAbsolutePath();
        // contractDir = new File(project.getProjectDir() , "src/main/solidity/contracts").getAbsolutePath();

        //for default values
        //this.solidityOutputDir = (new File(project.getProjectDir(), "src/main/solidity/build")).getAbsolutePath();
        //this.sourceDirectorySet.srcDir(new File(project.getProjectDir() , "/src/main/solidity"));
        //this.sourceDirectorySet.setOutputDir(new File(project.getProjectDir() , "src/main/java"));
        //this.sourceDirectorySet.include("**/*.sol");


        //create truffle contract source dir set for easy use
        //this.truffleContractSourceDirectorySet = sourceDirectorySetFactory.create("truffle-contract json files");
        //this.truffleContractSourceDirectorySet.include("**/*.json");
        //this.truffleContractSourceDirectorySet.srcDir(truffleJsonDir);

        //add wrapper output dir to the Java source set if Java plugin is applied
        JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaPluginConvention != null) {
            SourceDirectorySet javaSourceDirectorySet = javaPluginConvention.getSourceSets().getByName("main").getJava();
            javaSourceDirectorySet.srcDir(new File(project.getProjectDir(), "src/main/java"));
        }


    }

    public void prepare() {
        if(StringUtils.isEmpty(defaultPackageName)){
            throw new RuntimeException("defaultPackageName must not be null/empty");
        }

        /*if(sourceDirectorySet.getSrcDirTrees().isEmpty()){
            this.sourceDirectorySet.srcDir(new File(project.getProjectDir() , "src/main/solidity/contracts"));
        }*/

        /*Iterator<Map.Entry<String, ConfigurableFileTree>> it = sourceFileTrees.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, ConfigurableFileTree> entry = it.next();
            if(entry.getValue().)
        }*/
        if (sourceFileTrees.isEmpty()) {
            sourceFileTrees.put(
                    project.getName(),
                    project.fileTree(
                            Paths.get(project.getProjectDir().toString(), "src/main/solidity/contracts").toAbsolutePath().toString(),
                            files -> files.include("**/*.sol")
                    )
            );
        }



        if (npm != null) {
            libraries.add(npm);
        }
        if (ethpm != null) {
            libraries.add(ethpm);
        }
        if (lib != null) {
            libraries.add(lib);
        }

    }


    public SolidityBuilderExtension fromJson(String jsonString) {
        ObjectMapper om = new ObjectMapper();
        SolidityBuilderExtension value = null;
        try {
            value = om.readValue(jsonString, SolidityBuilderExtension.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    //==========================================================================

    /*public void sourceSets(Closure closure) {
        ConfigureUtil.configure(closure, this.sourceDirectorySet);
    }*/

    //srcDir must be absolute
    public void source(String prefix, String srcDir) {
        Path srcDirPath = Paths.get(srcDir).toAbsolutePath();
        Action<? super ConfigurableFileTree> action = (Action<ConfigurableFileTree>) files -> {
            files.include("**/*.sol");
        };
        sourceFileTrees.put(prefix, project.fileTree(srcDirPath, action));

        RemappingPackageRule rr = new RemappingPackageRule(Paths.get(prefix), srcDirPath, Paths.get(prefix));
        this.remappings.put(srcDirPath,rr);
    }

    public void source(String prefix, String srcDir, Closure configClosure) {
        Path srcDirPath = Paths.get(srcDir).toAbsolutePath();
        sourceFileTrees.put(prefix, project.fileTree(srcDirPath, configClosure));

        RemappingPackageRule rr = new RemappingPackageRule(Paths.get(prefix), srcDirPath, Paths.get(prefix));
        this.remappings.put(srcDirPath,rr);
    }

    public void source(String prefix, String srcDir, String packageName, Closure configClosure) {
        Path srcDirPath = Paths.get(srcDir).toAbsolutePath();
        sourceFileTrees.put(prefix, project.fileTree(srcDirPath, configClosure));

        RemappingPackageRule rr = new RemappingPackageRule(Paths.get(prefix), srcDirPath, Paths.get(packageName));
        this.remappings.put(srcDirPath,rr);
    }

    //add a dir path as library, the library's last folder name is the prefix and package name
    public void library(String srcDir){
        Path srcDirPath = Paths.get(srcDir).toAbsolutePath();
        if(!srcDirPath.toFile().isDirectory()){
            throw new RuntimeException("adding library : " + srcDir +", the given path is not a directory");
        }
        if(srcDirPath.getFileName() == null){
            throw new RuntimeException("adding library : " + srcDir +", the given path might be root path, can't infer prefix and package name");
        }
        Path prefixPath = srcDirPath.getFileName();
        Path packageNamePath = srcDirPath.getFileName();
        RemappingPackageRule rr = new RemappingPackageRule(prefixPath, srcDirPath, packageNamePath);
        this.remappings.put(rr.to,rr);
    }

    public void remapping(String prefix, String strPath) {
        RemappingPackageRule rr = new RemappingPackageRule(Paths.get(prefix), Paths.get(strPath), Paths.get(prefix));
        this.remappings.put(rr.to,rr);
    }

    public void remapping(String prefix, String strPath, String packageName) {
        RemappingPackageRule rr = new RemappingPackageRule(Paths.get(prefix), Paths.get(strPath), Paths.get(packageName));
        this.remappings.put(rr.to,rr);
    }


    //add more library path, all sub-folders will be scanned as lib
    public void libraryRoot(String path) {
        libraries.add(Paths.get(path));
    }

    public void npm(String npm) {
        if (StringUtils.isEmpty(npm)) {
            this.npm = null;
        }
        this.npm = Paths.get(npm);
    }

    public void ethpm(String ethpm) {
        if (StringUtils.isEmpty(ethpm)) {
            this.ethpm = null;
        }
        this.ethpm = Paths.get(ethpm);
    }

    public void lib(String lib) {
        if (StringUtils.isEmpty(lib)) {
            this.lib = null;
        }
        this.lib = Paths.get(lib);
    }

    public void importProject(String path, String... type) {
        //todo
        //config the path to source, libraries
    }

    public void optimizer(int _input) {
        this.optimizer = _input;
    }

    public void solidityOutputDir(String _input) {
        this.solidityOutputDir = Paths.get(_input).normalize().toAbsolutePath();
    }

    public void defaultPackageName(String _input) {
        this.defaultPackageName = _input;
    }

    public void generatedPackageName(String _input) {
        this.generatedPackageName = _input;
    }

    public void excludedContracts(Set<String> _input) {
        this.excludedContracts = _input;
    }

    public void version(String _input) {
        this.version = _input;
    }

    public void overwriteWrapper(boolean _input) {
        this.overwriteWrapper = _input;
    }

    //==========================================================================

    @InputFiles
    @SkipWhenEmpty
    public FileCollection getAllSourceFileTrees() {
        sourceFileTrees.entrySet().stream().forEach(e -> {
            fileCollection = fileCollection.plus(e.getValue());
        });
        return fileCollection;
    }




    //@InputFiles
    /*public SourceDirectorySet getTruffleContractSourceDirectorySet() {
        return this.truffleContractSourceDirectorySet;
    }*/

    @Input
    public Integer getOptimizer() {
        return this.optimizer;
    }

    /*@Input
    public String getSolidityOutputDir() {
        if (this.solidityOutputDir == null) {
            return "";
        }
        return this.solidityOutputDir.toString();
    }*/

    @Input
    @Optional
    public String getSolidityOutputDir() {
        return this.solidityOutputDir == null? null:this.solidityOutputDir.toString();
    }

    @Input
    public String getVersion() {
        return this.version;
    }

    @Input
    public String getDefaultPackageName() {
        return this.defaultPackageName;
    }

    @Input
    @Optional
    public String getGeneratedPackageName() {
        return generatedPackageName;
    }

    @Input
    public Boolean isOverwriteWrapper() {
        return this.overwriteWrapper;
    }

    @Input
    public Set<String> getExcludedContracts() {
        return this.excludedContracts;
    }

    @Input
    public Boolean isUseNativeJavaTypes() {
        return this.useNativeJavaTypes;
    }

    @OutputDirectory
    public Path getWrapperOutputDir(){return this.wrapperOutputDir;}

    @Input
    public List<String> getRemappings() {
        return remappings.entrySet().stream().map(pathRemappingPackageRuleEntry -> {
            RemappingPackageRule e = pathRemappingPackageRuleEntry.getValue();
            return e.toString();
        }).collect(Collectors.toList());
    }

    @Input
    public List<String> getLibraries() {
        return libraries.stream().map(path -> path.toString()).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "SolidityBuilderExtension{" +
                "project=" + project +
                ", sourceDirectorySetFactory=" + sourceDirectorySetFactory +
                ", optimizer=" + optimizer +
                ", version='" + version + '\'' +
                ", solidityOutputDir='" + solidityOutputDir + '\'' +
                ", defaultPackageName='" + defaultPackageName + '\'' +
                ", overwriteWrapper=" + overwriteWrapper +
                ", excludedContracts=" + excludedContracts +
                ", useNativeJavaTypes=" + useNativeJavaTypes +
                ", sourceFileTrees=" + sourceFileTrees +
                ", remappings=" + remappings +
                ", libraries=" + libraries +
                ", npm='" + npm + '\'' +
                ", ethpm='" + ethpm + '\'' +
                ", lib='" + lib + '\'' +
                ", truffleJsonDir='" + truffleJsonDir + '\'' +
                ", fileCollection=" + fileCollection +
                '}';
    }

}
