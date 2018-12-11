package me.lycrus;

import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
public class Compiler {

    public String command;
    public PackageManager packageManager;
    public String os;
    public Project project;
    //all compiler stores:
    //$temp/SolidityBuild/provided|4.0.25/solc[.exe]
    //privateTemp = $temp/SolidityBuild/
    public File privateTemp;

    public Compiler(String _version, PackageManager _packageManager, Project _project) {

        this.os = getOs();
        this.project = _project;
        this.privateTemp = new File(System.getProperty("java.io.tmpdir"), "SolidityBuild");
        try {
            FileUtils.forceMkdir(privateTemp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!prepareCommand(_version)) {
            throw new RuntimeException("compiler error");
        }

        this.packageManager = _packageManager;
        return;
    }

    private boolean prepareCommand(String version) {
        String testCommand = getCommand(version);

        if (!StringUtils.isEmpty(testCommand) && checkSolc(testCommand)) {
            return true;
        } else {
            testCommand = downloadAndExtractSolc(version);
            return checkSolc(testCommand);
        }
    }

    private String getCommand(String version) {
        String untestedCommand = null;
        if ("native".equals(version)) {
            return "solc";
        } else if ("provided".equals(version) ||
                (StringUtils.split(version, '.') != null && StringUtils.split(version, '.').length == 3)) {
            //$temp/solc/provided|4.0.25/solc[.exe]
            File target = new File(privateTemp, version + File.separator + "solc" + ("win".equals(this.os) ? ".exe" : ""));
            if (target.exists()) {
                untestedCommand = target.toString();
            }
        } else {
            // you got wrong here
        }
        return untestedCommand;
    }


    private String downloadAndExtractSolc(String version) {
        if ("native".equals(version)) {
            //should not goes here
        } else if ("provided".equals(version)) {

            if ("win".equals(this.os)) {
                String resourcePath = "solc" + File.separator + this.os + File.separator + "solc.zip";
                //copy to temp folder
                File copyTo = new File(this.privateTemp, "solc.zip");
                try {
                    FileUtils.forceMkdirParent(copyTo);
                    Files.copy(Compiler.class.getClassLoader().getResourceAsStream(resourcePath), copyTo.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //extract .zip
                File dir = new File(this.privateTemp, version);
                try {
                    FileUtils.forceMkdir(dir);
                    FileUtils.cleanDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                extractZipToDir(copyTo, dir);
                File solc = new File(dir, "solc.exe");
                solc.setExecutable(true, false);
                return solc.toString();

            } else if ("linux".equals(this.os)) {
                String resourcePath = "solc" + File.separator + this.os + File.separator + "solc";
                File dir = new File(this.privateTemp, version);
                try {
                    FileUtils.forceMkdir(dir);
                    FileUtils.cleanDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File copyTo = new File(dir, "solc");
                try {
                    Files.copy(Compiler.class.getClassLoader().getResourceAsStream(resourcePath), copyTo.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                copyTo.setExecutable(true, false);
                return copyTo.toString();

            } else if ("mac".equals(this.os)) {
                String resourcePath = "solc" + File.separator + this.os + File.separator + "solc.zip";
                //copy to temp folder
                File copyTo = new File(this.privateTemp, "solc.zip");
                try {
                    FileUtils.forceMkdirParent(copyTo);
                    Files.copy(Compiler.class.getClassLoader().getResourceAsStream(resourcePath), copyTo.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File dir = new File(this.privateTemp, version);
                try {
                    FileUtils.forceMkdir(dir);
                    FileUtils.cleanDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //extract .zip
                extractZipToDir(copyTo, dir);
                File solc = new File(dir, "solc");
                solc.setExecutable(true, false);
                return solc.toString();
            } else {

            }

        } else if (StringUtils.split(version, '.') != null && StringUtils.split(version, '.').length == 3) {
            //download from remote
            if ("win".equals(this.os)) {
                File downloadedFile = downloadFile(
                        "https://github.com/ethereum/solidity/releases/download/v" + version + "/solidity-windows.zip",
                        new File(this.privateTemp, "solidity-windows.zip")
                );
                File to = new File(this.privateTemp, version);
                try {
                    FileUtils.forceMkdir(to);
                    FileUtils.cleanDirectory(to);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                extractZipToDir(downloadedFile, to);
                File solc = new File(to,"solc.exe");
                solc.setExecutable(true,false);
                return solc.toString();
            } else if ("linux".equals(this.os)) {
                File downloadedFile = downloadFile(
                        "https://github.com/ethereum/solidity/releases/download/v" + version + "/solc-static-linux",
                        new File(this.privateTemp, "solc-static-linux")
                );
                File to = new File(this.privateTemp, version);
                try {
                    FileUtils.forceMkdir(to);
                    FileUtils.cleanDirectory(to);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File solc = new File(to,"solc");
                try {

                    Files.copy(downloadedFile.toPath(), solc.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                solc.setExecutable(true,false);
                return solc.toString();
            } else if ("mac".equals(this.os)) {
                //not support yet
            }

        } else {
            // you got wrong here
        }
        return null;
    }

    private boolean checkSolc(String testCommand) {
        List<String> args = new LinkedList<>();
        boolean success = false;


        args.add(testCommand);
        args.add("--version");
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;

        try {
            process = processBuilder.start();
            String answer = IOUtils.toString(process.getInputStream(), Charset.forName("UTF-8"));
            int exitValue = process.waitFor();
            if (StringUtils.contains(answer, "solc, the solidity compiler commandline interface") && exitValue == 0) {
                success = true;
                System.out.println(answer);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!success) {
            System.out.println("checkSolcCompiler fails");
            return false;
        }
        System.out.println("command : " + testCommand);

        this.command = testCommand;
        return true;
    }

    private File downloadFile(String url, File to) {
        try {
            URL downloadLink = new URL(url);
            FileUtils.copyURLToFile(downloadLink, to, 10 * 1000, 30 * 1000);
            return to;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("can't resolve : " + url + ", please check your version is correct and network");
        }
        return null;
    }


    public SolcStandardOutput compile(SolcStandardInput solcStandardInput) {
        List<String> args = new LinkedList<>();
        if (StringUtils.isEmpty(command)) {
            throw new RuntimeException("solc compiler not found");
        }
        args.add(command);
        args.add("--standard-json");
        args.add("--pretty-json");
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        int exitValue = -1;
        SolcStandardOutput solcStandardOutput = null;

        try {
            process = processBuilder.start();
            solcStandardInput.writeToOutputStream(process.getOutputStream());
            solcStandardOutput = SolcStandardOutput.create(process.getInputStream(), solcStandardInput, packageManager);
            Iterator<SolcStandardOutput.Error> it = solcStandardOutput.getErrors().iterator();

            while (it.hasNext()) {
                SolcStandardOutput.Error e = it.next();
                System.out.println(e.toString());
            }

            exitValue = process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (exitValue != 0) {
            System.out.println("solc fails with exit " + exitValue);
        }

        return solcStandardOutput;
    }

    private String getOs() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "win";
        } else if (os.contains("linux")) {
            return "linux";
        } else if (os.contains("mac")) {
            return "mac";
        } else {
            throw new RuntimeException("operating System isn't supported, os: " + os);
        }
    }

    private void extractZipToDir(File zipFile, File toDir) {
        //File zipFile=project.file(solc);
        FileTree zipfileTree = project.zipTree(zipFile);
        project.copy(copySpec -> copySpec.from(zipfileTree).into(toDir));
    }
}
