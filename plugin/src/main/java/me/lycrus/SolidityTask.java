package me.lycrus;

import me.lycrus.antlr.ImportsRetriever;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.api.tasks.options.Option;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SolidityTask extends AbstractTask {
    public SolidityBuilderExtension solidityBuilderExtension;
    public Set<Sol> outOfDateFiles = new HashSet<>();
    public Set<Sol> removedFiles = new HashSet<>();
    public PackageManager packageManager;

    @Nested
    public SolidityBuilderExtension getSolidityBuilderExtension() {
        return this.solidityBuilderExtension;
    }

    public void setSolidityBuilderExtension(SolidityBuilderExtension solidityBuilderExtension) {
        this.solidityBuilderExtension = solidityBuilderExtension;
    }

    public Boolean doCleanBuild;

    @Option(option = "doCleanBuild", description = "do a clean full build")
    public Boolean getDoCleanBuild() {
        return doCleanBuild;
    }

    @Inject
    public SolidityTask(SolidityBuilderExtension solidityBuilderExtension) {
        this.solidityBuilderExtension = solidityBuilderExtension;
    }

    //因为library只检测变动，不检测文件，所以library的变动是触发全build的。
//引用{非}library是不检测文件变动的，所以非library的变动是不会触发build的。当然，一个sol引用了一个sol@lib，并且sol@lib变更了，那么sol则{不会}重新编译
    @TaskAction
    public void start(IncrementalTaskInputs inputs) {
        try {
            /*Configuration configuration=null;
            if (getProject().getPlugins().hasPlugin(JavaPlugin.class)) {
                configuration = getProject()
                        .getConfigurations()
                        //.getByName(JavaPlugin.API_CONFIGURATION_NAME);
                        .getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME);
            }
            DependencyHandler dependencies = getProject().getDependencies();
            dependencies.add(configuration.getName(),"org.web3j:console:3.6.0");*/

            File configPath = new File(getProject().getBuildDir(), "SolidityBuilder");

            packageManager = PackageManager.create(solidityBuilderExtension);
            Sol.packageManager = packageManager;
            Compiler compiler = new Compiler(solidityBuilderExtension.getVersion(), packageManager, getProject());

            ImportsRetriever importsRetriever = new ImportsRetriever(packageManager);
            if (!inputs.isIncremental()) {
                //have to compare config to remove previous build
                //Config newConfig = Config.create(solidityBuilderExtension);

                //OK, now we clear all build we made before
                try {
                    FileUtils.deleteDirectory(new File(getProject().getBuildDir(), "/solidity"));
                    //todo delete recorded wrappers
                    FileUtils.deleteDirectory(configPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            outOfDateFiles.clear();
            removedFiles.clear();

            inputs.outOfDate(new Action<InputFileDetails>() {
                @Override
                public void execute(InputFileDetails inputFileDetails) {
                    outOfDateFiles.add(Sol.getSol(inputFileDetails.getFile().toPath()));
                }
            });

            inputs.removed(new Action<InputFileDetails>() {
                @Override
                public void execute(InputFileDetails inputFileDetails) {
                    removedFiles.add(Sol.getSolDeleted(inputFileDetails.getFile().toPath()));
                }
            });

            Dependency dependency = Dependency.load(configPath, packageManager);


            //parse all out of date files to gather predecessors and set reflect it to dependency
            for (Sol file : outOfDateFiles) {
                Set<Sol> imports = importsRetriever.getImports(file);
                dependency.setNode(file, imports);
            }


            //delete all removed files
            dependency.removeNode(removedFiles);

            dependency.save(configPath);

            Set<Sol> needRebuild = new HashSet<>();

            needRebuild.addAll(outOfDateFiles);

            Set<Sol> filesDependOnOutOfDateFiles = dependency.findAllSuccessors(outOfDateFiles);
            needRebuild.addAll(filesDependOnOutOfDateFiles);

            System.out.println("files to rebuild : ");
            for (Sol file : needRebuild) {
                System.out.println(file.fileLocation);
            }

            Set<Sol> needImport = new HashSet<>();
            needImport.addAll(dependency.findAllPredecessors(needRebuild));
            //add them to SolcStandardInput

            System.out.println("files to import : ");
            for (Sol file : needImport) {
                System.out.println(file.fileLocation);
            }

            SolcStandardInput solcStandardInput = SolcStandardInput.create(packageManager);
            //solcStandardInput.addSources(solidityBuilderExtension.getSourceDirectorySet().getFiles());
            solcStandardInput.addSources(needRebuild).addSources(needImport);
            SolcStandardOutput solcStandardOutput = compiler.compile(solcStandardInput);

            List<Path> jsonDir = new LinkedList<>();
            jsonDir.add(solidityBuilderExtension.truffleJsonDir);
            if (solidityBuilderExtension.solidityOutputDir != null) {
                jsonDir.add(solidityBuilderExtension.solidityOutputDir);
            }
            TruffleContractWriter truffleContractWriter = new TruffleContractWriter(jsonDir);

            Set<SmartContract> writedTruffleContracts = truffleContractWriter.write(solcStandardOutput);
            WrapperInvoker wrapperInvoker = new WrapperInvoker();
            wrapperInvoker.wrapper(writedTruffleContracts, solidityBuilderExtension);
            System.out.println("end end end");
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}