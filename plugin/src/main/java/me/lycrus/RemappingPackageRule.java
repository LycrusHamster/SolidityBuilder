package me.lycrus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.file.Path;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RemappingPackageRule {

    //for windows, prefix may be github.com\a\b, note that Path doesn't need following backslash
    //for *nix, prefix may be github.com/a/b, note that Path doesn't need following forward-slash
    public Path prefix; //maybe github.com/a/b

    //'to' path must be absolute!!!
    //for windows, 'to' may be github.com\x\y, note that Path doesn't need following backslash
    //for *nix, prefix may be github.com/x/y, note that Path doesn't need following forward-slash
    public Path to;//maybe /usr/lib/a/b

    //for windows, packageName may be github.com\x\y, note that Path doesn't need following backslash
    //for *nix, packageName may be github.com/x/y, note that Path doesn't need following forward-slash
    public Path packageName; //maybe x/y


    public boolean isDefaultPackage(){
        return packageName.getNameCount() == 0;
    }

    @Override
    public String toString() {
        return "RemappingPackageRule{" +
                "prefix=" + prefix +
                ", to=" + to +
                ", packageName=" + packageName +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RemappingPackageRule that = (RemappingPackageRule) o;

        return new EqualsBuilder()
                .append(prefix, that.prefix)
                .append(to, that.to)
                .append(packageName, that.packageName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(prefix)
                .append(to)
                .append(packageName)
                .toHashCode();
    }
}
