package me.lycrus;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.plugins.JavaPlugin;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

import static me.lycrus.SolidityBuilderExtension.DSL_NAME;

public class SolidityBuilder implements Plugin<Project> {
    private final SourceDirectorySetFactory sourceDirectorySetFactory;

    @Inject
    SolidityBuilder(SourceDirectorySetFactory sourceDirectorySetFactory) {
        this.sourceDirectorySetFactory = sourceDirectorySetFactory;
    }

    public void apply(Project project) {
        System.out.println("gradle plugin SolidityBuilder started");


        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            project.getPlugins().apply(JavaPlugin.class);
        }

        Configuration configuration= project.getConfigurations()
                    //.getByName(JavaPlugin.API_CONFIGURATION_NAME);
                    .getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME);

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add(configuration.getName(),"org.web3j:core:4.0.3");


        project.getExtensions().create(DSL_NAME, SolidityBuilderExtension.class, new Object[]{project, this.sourceDirectorySetFactory});

        project.afterEvaluate((project1) -> {
            SolidityBuilderExtension extensions = (SolidityBuilderExtension) project1.getExtensions().findByName(DSL_NAME);
            extensions.prepare();

            SolidityTask solidityTask = project1.getTasks().create("olivia", SolidityTask.class, new Object[]{extensions});
            System.out.println("gradle plugin SolidityBuilder finished");

            if (project1.getTasks().getByName("compileJava")!= null) {
                Task compileJavaTask = project1.getTasks().getByName("compileJava");
                compileJavaTask.dependsOn(solidityTask);
            }
        });

    }

    public static void main(String[] args) {
        Path test = Paths.get("");
        int c = test.getNameCount();
        return;
    }
}
