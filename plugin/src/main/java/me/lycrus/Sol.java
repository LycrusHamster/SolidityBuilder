package me.lycrus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;


//present a sol file and all its info
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Sol {

    public static PackageManager packageManager;
    public static Map<Path, Sol> cache = new LinkedHashMap<>();

    public Path fullPackageName;// e.g. myPackage, may be null
    public Path fileLocation;//absolute, /some/where/myPackage/util/u.sol  c:/some/where/mayPackage/util/u.sol   note that only forward-slash is valid
    public String content;
    //public String[] packageNames;
    public boolean nonePackage;// packageNames's length == 0
    public String fileLocationSlashString;//  /some/where/myPackage/util/u.sol  c:/some/where/mayPackage/util/u.sol   note that only forward-slash is valid

    /*public Sol(String fullPackageName, Path fileLocation) {
        this.fullPackageName = fullPackageName;
        this.fileLocation = fileLocation.toAbsolutePath();
        this.packageNames = fullPackageName.split("/");
        this.nonePackage = this.packageNames.length == 0;
        try {
            this.content = FileUtils.readFileToString(this.fileLocation.toFile(), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileLocationSlashString = fileLocation.toString().replaceAll("\\\\","/");
    }*/

    //Path github.com/a/b/c => String github.com.a.b.c
    //Path null => null
    //may be null
    public String getPackage(){
        return StringUtils.join(fullPackageName,".");
    }

    private Sol(Path fileLocation) {
        this(fileLocation,true);
    }

    private Sol(Path fileLocation, boolean readContent) {
        //this.fullPackageName = packageManager.findPackage(fileLocation);
        this.fullPackageName = packageManager.findPackage(fileLocation);
        this.fileLocation = fileLocation.toAbsolutePath();

        //this.packageNames = fullPackageName != null?fullPackageName.split("/"):new String[]{};
        this.nonePackage = fullPackageName==null?true:fullPackageName.getNameCount() == 0;
        if(readContent) {
            try {
                this.content = FileUtils.readFileToString(this.fileLocation.toFile(), Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("read .sol file error : " + this.fileLocation.toFile());
            }
            if (this.content == null) {
                throw new RuntimeException("read .sol file error : " + this.fileLocation.toFile());
            }
        }
        this.fileLocationSlashString = fileLocation.toString().replaceAll("\\\\", "/");
    }

    public static Sol getSol(Path fileLocation) {
        Sol ret = cache.get(fileLocation);
        if (ret == null) {
            ret = new Sol(fileLocation);
            cache.put(fileLocation, ret);
        }
        return ret;
    }

    public static Sol getSolDeleted(Path fileLocation) {
        Sol ret = cache.get(fileLocation);
        if (ret == null) {
            ret = new Sol(fileLocation,false);
            cache.put(fileLocation, ret);
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Sol sol = (Sol) o;

        return new EqualsBuilder()
                .append(fileLocation, sol.fileLocation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(fileLocation)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Sol{" +
                "fullPackageName=" + fullPackageName +
                ", fileLocation=" + fileLocation +
                ", content='" + content + '\'' +
                ", nonePackage=" + nonePackage +
                ", fileLocationSlashString='" + fileLocationSlashString + '\'' +
                '}';
    }
}
