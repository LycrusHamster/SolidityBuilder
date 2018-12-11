package me.lycrus;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


@AllArgsConstructor
public class PackageManager {

    //prefix => absolute path
    //prefix and path must ends with '/'
    //e.g.          : github.com/ethereum/dapp-bin/=/usr/local/dapp-bin/
    //              : github.com/myPackage/moduleA/=/usr/local/else/
    //              : github.com/myPackage/moduleA/=/usr/local/else/
    //
    //note!!  java.nio.path will remove the ending "/", eg  a/b/c/ will be a/b/c if Paths.get("a/b/c/").toString()
    //note!!  java.nio.path will ignore "/" and "\"
    //but here it doesn't matter cause java.nio.file provide methods like resolve, relative, etc.
    //public Map<Path, Path> remappings = new LinkedHashMap<>();

    // absolute path => package name(must not be none,null,empty, split by dot)
    //public Map<Path, Path> packages = new LinkedHashMap<>();

    //absolutePath -> {prefix + packageName + absolutePath}
    public Map<Path, RemappingPackageRule> rules = new LinkedHashMap<>();

    private PackageManager() {
    }

    public static PackageManager create(SolidityBuilderExtension solidityBuilderExtension) {
        PackageManager pm = new PackageManager();

        //following remappings/package list in ascending order
        //add default remapping
        /*pm.remappings.put(Paths.get("src"), solidityBuilderExtension.project.getProjectDir().toPath().resolve(Paths.get("src")).normalize().toAbsolutePath());
        pm.remappings.put(Paths.get("project"), solidityBuilderExtension.project.getProjectDir().toPath().normalize().toAbsolutePath());
        pm.remappings.put(Paths.get("/"), Paths.get("/"));//for global access*/

        //src, project has NO package
        pm.rules.put(solidityBuilderExtension.project.getProjectDir().toPath().resolve(Paths.get("src")).normalize().toAbsolutePath(),
                new RemappingPackageRule(
                        Paths.get("src"),
                        solidityBuilderExtension.project.getProjectDir().toPath().resolve(Paths.get("src")).normalize().toAbsolutePath(),
                        null));
        pm.rules.put(solidityBuilderExtension.project.getProjectDir().toPath().normalize().toAbsolutePath(),
                new RemappingPackageRule(
                        Paths.get("project"),
                        solidityBuilderExtension.project.getProjectDir().toPath().normalize().toAbsolutePath(),
                        null));
        /*pm.rules.add(new RemappingPackageRule(Paths.get("/"),
                Paths.get("/"),
                null));*/

        //add libraries to remapping and package
        Set<Path> libraries = solidityBuilderExtension.libraries;
        for (Path path : libraries) {
            if (!Files.isDirectory(path)) {
                System.out.println("the library path should be a directory, not a file");
                continue;
            }
            try {
                Files.list(path).forEach(dir -> {
                    System.out.println("library : " + dir.getFileName());
                    //pm.remappings.put(dir.getFileName(), dir.toAbsolutePath());
                    //pm.packages.put(dir.toAbsolutePath(),dir.getFileName().toString());
                    pm.rules.put(dir.toAbsolutePath(),
                            new RemappingPackageRule(
                                    dir.getFileName(),
                                    dir.toAbsolutePath(),
                                    dir.getFileName()));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //add source files to remapping and pacakge
        /*Map<Path, Path> sourceRemappings = solidityBuilderExtension.sourceFileTrees.entrySet().stream().collect(
                Collectors.toMap(
                        e -> Paths.get(e.getKey()),
                        e -> e.getValue().getDir().toPath().normalize().toAbsolutePath()
                )
        );

        pm.remappings.putAll(sourceRemappings);

        Map<Path, String> sourcePackages = solidityBuilderExtension.sourceFileTrees.entrySet().stream().collect(
                Collectors.toMap(
                        e -> e.getValue().getDir().toPath().normalize().toAbsolutePath(),
                        e -> e.getKey()
                )
        );
        pm.packages.putAll(sourcePackages);*/


        //manual remappings owns highest priority
        //add manual remmappings to remappings and package(if specified)
        /*Map<Path, Path> tempRemapping = solidityBuilderExtension.remappings.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getValue().prefix,
                        e -> e.getValue().to
                ));

        pm.remappings.putAll(tempRemapping);

        Map<Path, String> tempPackage = solidityBuilderExtension.remappings.entrySet().stream()
                .filter(pathRemappingRuleEntry -> StringUtils.isNotEmpty(pathRemappingRuleEntry.getValue().packageName))
                .collect(Collectors.toMap(
                        o -> o.getValue().to,
                        o -> o.getValue().packageName
                ));

        pm.packages.putAll(tempPackage);*/

        //add configured rules for source and manual remappings
        //you can use manual remappings to override default 'src' and 'project, as well as auto library scan
        pm.rules.putAll(solidityBuilderExtension.remappings);

        return pm;
    }

    public List<String> getAllForSolcRemapping() {
        List<String> ret = this.rules.entrySet().stream().map(ruleEntry -> {
            String prefix = ruleEntry.getValue().prefix.toString();
            prefix.replaceAll("\\\\", "/");
            if (!prefix.endsWith("/")) {
                prefix += "/";
            }

            String target = ruleEntry.getValue().to.toString();
            target.replaceAll("\\\\", "/");
            if (!target.endsWith("/")) {
                target += "/";
            }

            return prefix + "=" + target;
        }).collect(Collectors.toList());
        return ret;
    }

    //give a {Solidity} import/relative/absolute path (which get from ImportsRetriever), return its package full name and file path
    //current must be absolute
    //this function simulate solc's manner
    //current in windows may be c:\a\b\m.sol
    //current in *nix may be /home/a/b/m.sol
    public Sol resolve(Path current, String solidityImportPath) {
        Path path = Paths.get(solidityImportPath);
        Sol sol = null;
        //path in windows may be c:/a/b/c.sol
        //path in *nix may be /home/a/b/c.sol
        if (path.isAbsolute()) {
            sol = Sol.getSol(path);
        }
        //path in windows and u*nix may be ./a/b/c.sol or ../a/./b/../c.sol
        else if (solidityImportPath.startsWith("./") || solidityImportPath.startsWith("../")) {
            path = current.getParent().resolve(path).normalize().toAbsolutePath();//now path resolves to absolute path;
            sol = Sol.getSol(path);
        }
        //path in windows may be open-zeppelin/contract/math/safeMath.sol
        //path in *nix may be /home/a/b/c.sol
        else {//check if it starts with package
            sol = interpret(path);
        }
        return sol;
    }


    //give a {relative} solidity path starting with package name and return its {absolute} path by match {package name}
    public Sol interpret(final Path path) {
        Iterator<Map.Entry<Path, RemappingPackageRule>> it = rules.entrySet().iterator();
        int count = -1;// -1 for not found
        //String packageName = null;
        Path ret = null;
        while (it.hasNext()) {
            Map.Entry<Path, RemappingPackageRule> e = it.next();
            Path prefix = e.getValue().prefix;
            Path absolutePath = e.getValue().to;
            if (path.startsWith(prefix)) {//got a match
                //count must be large than 1 for normal package/remappings
                //for Path("/") count goes to 0
                int num = prefix.getNameCount();
                if (count < num) {
                    count = num;
                    Path temp = prefix.relativize(path);//get rid of the prefix
                    ret = absolutePath.resolve(temp);
                    //e.g. package1/../package/2/xx.sol , it should goes to package2, not package1
                    //packageName = e.getKey().toString();
                }
            }
        }
        if(ret == null){
            System.out.println("can't parse import string : " + path.toString());
            return Sol.getSol(path);
        }
        return Sol.getSol(/*packageName,*/ ret);
    }

    //give an {absolute} path to find its {package name}
    public Path findPackage(Path absolutePath) {

        if(!absolutePath.isAbsolute()){
            return null;
        }

        Path packageName = null;
        int count = -1;//-1 = not match , 0 = match "/", large than 1 = match remappings

        Iterator<Map.Entry<Path, RemappingPackageRule>> it = rules.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Path, RemappingPackageRule> e = it.next();
            Path toPath = e.getValue().to;
            Path basePackage = e.getValue().packageName;
            if(basePackage == null){
                continue;
            }
            if (absolutePath.startsWith(e.getKey())) {

                //for *nix "/" =0, "a/" =1, "a" =1, "/a" =1, "a/b/" =2, "a/b" = 2, "\" =1 in *nix
                //for windows  "c:\" =0, "c:\a" =1, "a\b" =2, "a\" =1, "\a" =1, "a" =1, "\" =0
                int length = toPath.getNameCount();

                //the longest path priors
                if (count < length) {//   /a/b/c/d/e.sol, /a/b/c=> com.github.some.where, com.github.some.where.d.e.sol
                    count = length;

                    // /a/b/c => pkgOutter/pkgInner
                    // /a/b/c/d/m.sol => ./d/m.sol
                    Path relative = toPath.relativize(absolutePath);
                    //./d/m.sol => pkgOutter/pkgInner/d/m.sol
                    packageName = basePackage.resolve(relative);
                    packageName =packageName.getParent();
                    /*String[] packageBase = e.getValue().split("."); //the key may be com.github.some.where
                    packageName = new String[relative.getNameCount()-1];//split by "/"
                    for (int i = 0; i < relative.getNameCount() - 1; i++) {
                        packageName[i] = relative.getName(i).toString();
                    }
                    packageName = ArrayUtils.addAll(packageBase,packageName);*/
                }
            }
        }
        return packageName;

    }


}
